package io.github.codesapienbe.springvision.starter.web.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for TaskSubmissionResponse record.
 * Tests all constructors, getters, and record-specific behavior.
 */
class TaskSubmissionResponseTest {

    // Test fixtures
    private final String testCorrelationId = "task-submit-123";
    private final String testTaskId = "task-456";
    private final String testStatus = "SUBMITTED";

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create TaskSubmissionResponse with all parameters")
        void shouldCreateTaskSubmissionResponseWithAllParameters() {
            // When: Creating TaskSubmissionResponse with all parameters
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // Then: Should store all values correctly
            assertThat(response.correlationId()).isEqualTo(testCorrelationId);
            assertThat(response.taskId()).isEqualTo(testTaskId);
            assertThat(response.status()).isEqualTo(testStatus);
        }

        @Test
        @DisplayName("Should create TaskSubmissionResponse with null values")
        void shouldCreateTaskSubmissionResponseWithNullValues() {
            // When: Creating TaskSubmissionResponse with null values
            TaskSubmissionResponse response = new TaskSubmissionResponse(null, null, null);

            // Then: Should store null values
            assertThat(response.correlationId()).isNull();
            assertThat(response.taskId()).isNull();
            assertThat(response.status()).isNull();
        }

        @Test
        @DisplayName("Should allow access to all fields via getters")
        void shouldAllowAccessToAllFieldsViaGetters() {
            // Given: TaskSubmissionResponse instance
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When: Accessing fields via getters
            String correlationId = response.correlationId();
            String taskId = response.taskId();
            String status = response.status();

            // Then: Should return correct values
            assertThat(correlationId).isEqualTo(testCorrelationId);
            assertThat(taskId).isEqualTo(testTaskId);
            assertThat(status).isEqualTo(testStatus);
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: TaskSubmissionResponse instance
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When: Calling toString
            String string = response.toString();

            // Then: Should return a non-null string containing class name and field values
            assertThat(string).isNotNull();
            assertThat(string).contains("TaskSubmissionResponse");
            assertThat(string).contains(testCorrelationId);
            assertThat(string).contains(testTaskId);
            assertThat(string).contains(testStatus);
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two TaskSubmissionResponse instances with same values
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When & Then: Should be equal
            assertThat(response1).isEqualTo(response2);
            assertThat(response1).isEqualTo(response1);
            assertThat(response1).isNotEqualTo(null);
            assertThat(response1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement equals with different values")
        void shouldImplementEqualsWithDifferentValues() {
            // Given: TaskSubmissionResponse instances with different values
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(
                "different-id", testTaskId, testStatus);
            TaskSubmissionResponse response3 = new TaskSubmissionResponse(
                testCorrelationId, "different-task", testStatus);
            TaskSubmissionResponse response4 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, "different-status");

            // When & Then: Should not be equal
            assertThat(response1).isNotEqualTo(response2);
            assertThat(response1).isNotEqualTo(response3);
            assertThat(response1).isNotEqualTo(response4);
        }

        @Test
        @DisplayName("Should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Given: Two TaskSubmissionResponse instances with same values
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When: Calling hashCode
            int hashCode1 = response1.hashCode();
            int hashCode2 = response2.hashCode();

            // Then: Should have same hash codes
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("Should implement hashCode with different values")
        void shouldImplementHashCodeWithDifferentValues() {
            // Given: TaskSubmissionResponse instances with different values
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(
                "different-id", testTaskId, testStatus);

            // When: Calling hashCode
            int hashCode1 = response1.hashCode();
            int hashCode2 = response2.hashCode();

            // Then: Should have different hash codes
            assertThat(hashCode1).isNotEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("Field Handling")
    class FieldHandling {

        @Test
        @DisplayName("Should handle correlationId field")
        void shouldHandleCorrelationIdField() {
            // Given: Different correlation IDs
            String id1 = "corr-123";
            String id2 = "corr-456";

            // When: Creating responses with different correlation IDs
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(id1, testTaskId, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(id2, testTaskId, testStatus);

            // Then: Should have different correlation IDs
            assertThat(response1.correlationId()).isEqualTo(id1);
            assertThat(response2.correlationId()).isEqualTo(id2);
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle taskId field")
        void shouldHandleTaskIdField() {
            // Given: Different task IDs
            String task1 = "task-123";
            String task2 = "task-456";

            // When: Creating responses with different task IDs
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(testCorrelationId, task1, testStatus);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(testCorrelationId, task2, testStatus);

            // Then: Should have different task IDs
            assertThat(response1.taskId()).isEqualTo(task1);
            assertThat(response2.taskId()).isEqualTo(task2);
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle status field")
        void shouldHandleStatusField() {
            // Given: Different statuses
            String status1 = "SUBMITTED";
            String status2 = "FAILED";

            // When: Creating responses with different statuses
            TaskSubmissionResponse response1 = new TaskSubmissionResponse(testCorrelationId, testTaskId, status1);
            TaskSubmissionResponse response2 = new TaskSubmissionResponse(testCorrelationId, testTaskId, status2);

            // Then: Should have different statuses
            assertThat(response1.status()).isEqualTo(status1);
            assertThat(response2.status()).isEqualTo(status2);
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null taskId (failed submission)")
        void shouldHandleNullTaskIdFailedSubmission() {
            // When: Creating response for failed submission (null taskId)
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                testCorrelationId, null, "FAILED");

            // Then: Should have null taskId and FAILED status
            assertThat(response.correlationId()).isEqualTo(testCorrelationId);
            assertThat(response.taskId()).isNull();
            assertThat(response.status()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("Common Scenarios")
    class CommonScenarios {

        @Test
        @DisplayName("Should represent successful task submission")
        void shouldRepresentSuccessfulTaskSubmission() {
            // When: Creating successful submission response
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                "submit-123", "task-456", "SUBMITTED");

            // Then: Should represent successful submission
            assertThat(response.correlationId()).isEqualTo("submit-123");
            assertThat(response.taskId()).isEqualTo("task-456");
            assertThat(response.status()).isEqualTo("SUBMITTED");
        }

        @Test
        @DisplayName("Should represent failed task submission")
        void shouldRepresentFailedTaskSubmission() {
            // When: Creating failed submission response
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                "submit-789", null, "FAILED");

            // Then: Should represent failed submission
            assertThat(response.correlationId()).isEqualTo("submit-789");
            assertThat(response.taskId()).isNull();
            assertThat(response.status()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("Should represent pending task submission")
        void shouldRepresentPendingTaskSubmission() {
            // When: Creating pending submission response
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                "submit-999", "task-888", "PENDING");

            // Then: Should represent pending submission
            assertThat(response.correlationId()).isEqualTo("submit-999");
            assertThat(response.taskId()).isEqualTo("task-888");
            assertThat(response.status()).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            // When: Creating response with empty strings
            TaskSubmissionResponse response = new TaskSubmissionResponse("", "", "");

            // Then: Should handle empty strings
            assertThat(response.correlationId()).isEmpty();
            assertThat(response.taskId()).isEmpty();
            assertThat(response.status()).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given: Very long strings
            String longCorrelationId = "a".repeat(1000);
            String longTaskId = "b".repeat(1000);
            String longStatus = "c".repeat(1000);

            // When: Creating response with long strings
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                longCorrelationId, longTaskId, longStatus);

            // Then: Should handle long strings
            assertThat(response.correlationId()).hasSize(1000);
            assertThat(response.taskId()).hasSize(1000);
            assertThat(response.status()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle special characters in strings")
        void shouldHandleSpecialCharactersInStrings() {
            // Given: Strings with special characters
            String specialCorrelationId = "corr-123_special@test.com";
            String specialTaskId = "task_456-789";
            String specialStatus = "STATUS_1.0_beta";

            // When: Creating response with special characters
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                specialCorrelationId, specialTaskId, specialStatus);

            // Then: Should handle special characters
            assertThat(response.correlationId()).isEqualTo(specialCorrelationId);
            assertThat(response.taskId()).isEqualTo(specialTaskId);
            assertThat(response.status()).isEqualTo(specialStatus);
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given: Strings with unicode characters
            String unicodeCorrelationId = "corr-测试";
            String unicodeTaskId = "task-🚀";
            String unicodeStatus = "状态-✓";

            // When: Creating response with unicode characters
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                unicodeCorrelationId, unicodeTaskId, unicodeStatus);

            // Then: Should handle unicode characters
            assertThat(response.correlationId()).isEqualTo(unicodeCorrelationId);
            assertThat(response.taskId()).isEqualTo(unicodeTaskId);
            assertThat(response.status()).isEqualTo(unicodeStatus);
        }
    }

    @Nested
    @DisplayName("Record Immutability")
    class RecordImmutability {

        @Test
        @DisplayName("Should be immutable - no setters available")
        void shouldBeImmutableNoSettersAvailable() {
            // Given: TaskSubmissionResponse instance
            TaskSubmissionResponse response = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When & Then: Should only have getters, no setters
            // This test verifies that the record is immutable by design
            assertThat(response.correlationId()).isEqualTo(testCorrelationId);
            assertThat(response.taskId()).isEqualTo(testTaskId);
            assertThat(response.status()).isEqualTo(testStatus);

            // Attempting to call setters would result in compilation error
            // response.correlationId("new-id"); // This would not compile
        }

        @Test
        @DisplayName("Should create new instance for different values")
        void shouldCreateNewInstanceForDifferentValues() {
            // Given: Original response
            TaskSubmissionResponse original = new TaskSubmissionResponse(
                testCorrelationId, testTaskId, testStatus);

            // When: Creating new instance with different values
            TaskSubmissionResponse modified = new TaskSubmissionResponse(
                "new-correlation", "new-task", "new-status");

            // Then: Should be different instances with different values
            assertThat(original).isNotEqualTo(modified);
            assertThat(original.correlationId()).isNotEqualTo(modified.correlationId());
            assertThat(original.taskId()).isNotEqualTo(modified.taskId());
            assertThat(original.status()).isNotEqualTo(modified.status());
        }
    }
}
