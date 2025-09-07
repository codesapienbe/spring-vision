# JPA Vector Example (Spring Vision)

This example demonstrates a complete flow: upload a photo, register a face (store embedding) and run a similarity lookup using the JPA-backed vector provider. It includes a simple UI and an optional Postgres+pgvector test setup.

- Example module: `spring-vision-examples/jpa-vector-example`
- Example UI: `src/main/resources/static/index.html`
- Controller endpoints: `/api/faces/register` and `/api/faces/lookup`

Prerequisites
- Java 21 and Maven (project uses Java 21)
- Docker (to start Postgres+pgvector locally)

Quick start (Postgres + pgvector)

1. Start Postgres with pgvector (from the example directory):

```bash
cd spring-vision-examples/jpa-vector-example
docker compose up -d
```

2. (Optional) Create the `vector` extension if needed:

```bash
docker compose exec postgres psql -U postgres -d springvision_test -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

3. Run the example app using the `postgresql` profile so the example binds to localhost Postgres:

```bash
# from repo root
mvn -pl spring-vision-examples/jpa-vector-example spring-boot:run -Dspring-boot.run.profiles=postgresql
```

4. Open the UI in your browser:

- `http://localhost:8080/` — simple page to register a person and run a lookup.

Integration tests (PGVector)

- A disabled integration test exists at `spring-vision-jpa/src/test/java/com/springvision/jpa/PostgresPgVectorIntegrationTest.java`.
- To enable and run it:
  1. Remove the `@Disabled` annotation from the test file.
  2. Run the test (ensure Docker/Postgres is up):

```bash
# Run only the JPA module tests using the Postgres profile
mvn -pl spring-vision-jpa test -Dspring.profiles.active=postgresql
```

Notes
- The example uses a fallback JPA implementation when native DB features are unavailable. The pgvector native flows try to use JDBC-level `PGobject` bindings and fall back to `repository.save()` when that fails.
- The `docker compose` uses the `ankane/pgvector` image which bundles the `pgvector` extension.

Troubleshooting
- If the app fails to start because it cannot reach the DB, check `docker compose ps` and make sure the container is healthy.
- If the PG insert using native binding fails, the code falls back to JPA persistence; check logs for errors.

If you want I can add a small script or Makefile target (not committed per project rules) to automate running the example and tests locally. 