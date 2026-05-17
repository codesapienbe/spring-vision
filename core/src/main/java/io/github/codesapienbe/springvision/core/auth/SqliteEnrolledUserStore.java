package io.github.codesapienbe.springvision.core.auth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLite-backed implementation of {@link EnrolledUserStore}. The database file
 * lives at {@code ~/.springvision/db/auth.db} and is created on first access.
 *
 * <p>Embeddings are stored as little-endian {@code float32} blobs. Similarity
 * search is a full-scan cosine-similarity computation in Java — sufficient for
 * the demo-scale this store targets.</p>
 */
public class SqliteEnrolledUserStore implements EnrolledUserStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteEnrolledUserStore.class);

    private static final String SCHEMA = """
        CREATE TABLE IF NOT EXISTS enrolled_users (
            user_id    TEXT PRIMARY KEY,
            user_name  TEXT NOT NULL,
            remote_id  TEXT,
            embedding  BLOB NOT NULL,
            created_at INTEGER NOT NULL
        )
        """;

    private final String jdbcUrl;

    public SqliteEnrolledUserStore() {
        this(defaultDbPath());
    }

    public SqliteEnrolledUserStore(Path dbPath) {
        try {
            Files.createDirectories(dbPath.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create directory for auth DB at " + dbPath.getParent(), e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
        logger.info("EnrolledUserStore ready at {}", dbPath.toAbsolutePath());
    }

    private static Path defaultDbPath() {
        return Paths.get(System.getProperty("user.home"), ".springvision", "db", "auth.db");
    }

    private void initSchema() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement st = conn.createStatement()) {
            st.executeUpdate(SCHEMA);
            // Forward migration: add remote_id to databases created before this column existed.
            if (!hasColumn(conn, "enrolled_users", "remote_id")) {
                st.executeUpdate("ALTER TABLE enrolled_users ADD COLUMN remote_id TEXT");
                logger.info("Migrated enrolled_users: added remote_id column");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize auth.db schema", e);
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void enroll(String userId, String userName, String remoteId, float[] embedding) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("embedding must not be empty");
        }
        byte[] blob = floatsToBytes(embedding);
        String sql = """
            INSERT INTO enrolled_users (user_id, user_name, remote_id, embedding, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
              user_name  = excluded.user_name,
              remote_id  = excluded.remote_id,
              embedding  = excluded.embedding,
              created_at = excluded.created_at
            """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userName == null ? userId : userName);
            ps.setString(3, remoteId);
            ps.setBytes(4, blob);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to enroll user " + userId, e);
        }
    }

    @Override
    public boolean delete(String userId) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM enrolled_users WHERE user_id = ?")) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user " + userId, e);
        }
    }

    @Override
    public List<EnrolledUser> list() {
        List<EnrolledUser> out = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT user_id, user_name, remote_id, created_at FROM enrolled_users ORDER BY created_at DESC")) {
            while (rs.next()) {
                out.add(new EnrolledUser(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list enrolled users", e);
        }
        return out;
    }

    @Override
    public Optional<Match> findBestMatch(float[] probe, double threshold) {
        if (probe == null || probe.length == 0) {
            return Optional.empty();
        }
        float[] probeNorm = l2Normalize(probe);

        String bestId = null;
        String bestName = null;
        String bestRemote = null;
        double bestSim = -1.0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT user_id, user_name, remote_id, embedding FROM enrolled_users")) {
            while (rs.next()) {
                float[] stored = bytesToFloats(rs.getBytes(4));
                if (stored.length != probeNorm.length) {
                    continue;
                }
                double sim = dot(probeNorm, l2Normalize(stored));
                if (sim > bestSim) {
                    bestSim = sim;
                    bestId = rs.getString(1);
                    bestName = rs.getString(2);
                    bestRemote = rs.getString(3);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query enrolled users", e);
        }

        if (bestId == null || bestSim < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Match(bestId, bestName, bestRemote, bestSim));
    }

    // ---- vector helpers ----

    private static byte[] floatsToBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) {
            buf.putFloat(f);
        }
        return buf.array();
    }

    private static float[] bytesToFloats(byte[] b) {
        ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[b.length / Float.BYTES];
        for (int i = 0; i < out.length; i++) {
            out[i] = buf.getFloat();
        }
        return out;
    }

    private static float[] l2Normalize(float[] v) {
        double n = 0.0;
        for (float f : v) {
            n += f * f;
        }
        n = Math.sqrt(n);
        if (n == 0.0) {
            return v.clone();
        }
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / n);
        }
        return out;
    }

    private static double dot(float[] a, float[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            s += (double) a[i] * b[i];
        }
        return s;
    }
}
