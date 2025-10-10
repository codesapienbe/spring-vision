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

    public String getEventId() {
        return eventId;
    }

    public String getPersonId() {
        return personId;
    }

    public String getPersonName() {
        return personName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String personId;
        private String personName;
        private LocalDateTime timestamp;
        private boolean authorized;
        private String location;
        private String action;

        public Builder personId(String personId) {
            this.personId = personId;
            return this;
        }

        public Builder personName(String personName) {
            this.personName = personName;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder authorized(boolean authorized) {
            this.authorized = authorized;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

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

