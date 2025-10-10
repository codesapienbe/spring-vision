package io.github.codesapienbe.springvision.cyber.models;

/**
 * Represents an authorized person in the physical access control system.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class AuthorizedPerson {

    private final String id;
    private final String name;
    private String department;
    private String accessLevel;
    private byte[] faceEmbedding; // Face recognition embedding
    private boolean active;

    public AuthorizedPerson(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public byte[] getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(byte[] faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "AuthorizedPerson{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", department='" + department + '\'' +
            ", accessLevel='" + accessLevel + '\'' +
            ", active=" + active +
            '}';
    }
}
