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

    /**
     * Creates a new authorized person.
     * @param id The unique identifier of the person.
     * @param name The name of the person.
     */
    public AuthorizedPerson(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
    }

    /**
     * Gets the unique identifier of the person.
     * @return The person's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of the person.
     * @return The person's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the department of the person.
     * @return The person's department.
     */
    public String getDepartment() {
        return department;
    }

    /**
     * Sets the department of the person.
     * @param department The person's department.
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Gets the access level of the person.
     * @return The person's access level.
     */
    public String getAccessLevel() {
        return accessLevel;
    }

    /**
     * Sets the access level of the person.
     * @param accessLevel The person's access level.
     */
    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Gets the face embedding for face recognition.
     * @return The face embedding data.
     */
    public byte[] getFaceEmbedding() {
        return faceEmbedding;
    }

    /**
     * Sets the face embedding for face recognition.
     * @param faceEmbedding The face embedding data.
     */
    public void setFaceEmbedding(byte[] faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    /**
     * Checks if the person is currently active.
     * @return {@code true} if the person is active, {@code false} otherwise.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the person is currently active.
     * @param active {@code true} to set the person as active, {@code false} otherwise.
     */
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
