package io.github.codesapienbe.springvision.cyber.models;

import java.time.LocalDateTime;

/**
 * Represents an access control event (successful or failed access attempt).
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class AccessEvent {

    private final String eventId;
    private final String personId;
    private final String personName;
    private final LocalDateTime timestamp;
    private final boolean authorized;
    private String location;
    private String action;

    private AccessEvent(Builder builder) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.personId = builder.personId;
        this.personName = builder.personName;
        this.timestamp = builder.timestamp;
        this.authorized = builder.authorized;
        this.location = builder.location;
        this.action = builder.action;
    }

    /**
     * Gets the unique event identifier.
     *
     * @return The event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the identifier of the person involved in the event.
     *
     * @return The person's ID.
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Gets the name of the person involved in the event.
     *
     * @return The person's name.
     */
    public String getPersonName() {
        return personName;
    }

    /**
     * Gets the timestamp of when the event occurred.
     *
     * @return The event timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if the access was authorized.
     *
     * @return {@code true} if authorized, {@code false} otherwise.
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * Gets the location where the event occurred.
     *
     * @return The event location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location where the event occurred.
     *
     * @param location The event location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the action that was performed (e.g., "entry", "exit").
     *
     * @return The action performed.
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action that was performed.
     *
     * @param action The action performed.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Creates a new builder for creating an {@link AccessEvent}.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AccessEvent}.
     * Provides a fluent API for constructing access event instances.
     */
    public static class Builder {
        private String personId;
        private String personName;
        private LocalDateTime timestamp;
        private boolean authorized;
        private String location;
        private String action;

        /**
         * Default constructor for {@link Builder}.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Sets the person's ID.
         *
         * @param personId The person's ID.
         * @return This builder.
         */
        public Builder personId(String personId) {
            this.personId = personId;
            return this;
        }

        /**
         * Sets the person's name.
         *
         * @param personName The person's name.
         * @return This builder.
         */
        public Builder personName(String personName) {
            this.personName = personName;
            return this;
        }

        /**
         * Sets the event timestamp.
         *
         * @param timestamp The timestamp.
         * @return This builder.
         */
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets whether the access was authorized.
         *
         * @param authorized {@code true} if authorized, {@code false} otherwise.
         * @return This builder.
         */
        public Builder authorized(boolean authorized) {
            this.authorized = authorized;
            return this;
        }

        /**
         * Sets the event location.
         *
         * @param location The location.
         * @return This builder.
         */
        public Builder location(String location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the action performed.
         *
         * @param action The action.
         * @return This builder.
         */
        public Builder action(String action) {
            this.action = action;
            return this;
        }

        /**
         * Builds the {@link AccessEvent}.
         *
         * @return A new {@link AccessEvent} instance.
         */
        public AccessEvent build() {
            return new AccessEvent(this);
        }
    }

    @Override
    public String toString() {
        return "AccessEvent{" +
            "eventId='" + eventId + '\'' +
            ", personId='" + personId + '\'' +
            ", personName='" + personName + '\'' +
            ", timestamp=" + timestamp +
            ", authorized=" + authorized +
            ", location='" + location + '\'' +
            ", action='" + action + '\'' +
            '}';
    }
}
