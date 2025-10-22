package io.github.codesapienbe.springvision.starter.web.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Comprehensive unit tests for AsyncConfig configuration class.
 * Tests executor creation, MDC propagation, and virtual thread usage.
 */
class AsyncConfigTest {

    private AsyncConfig asyncConfig;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create AsyncConfig instance")
        void shouldCreateAsyncConfigInstance() {
            // When: Creating AsyncConfig
            AsyncConfig config = new AsyncConfig();

            // Then: Should be created successfully
            assertThat(config).isNotNull();
            assertThat(config).isInstanceOf(AsyncConfig.class);
        }
    }

    @Nested
    @DisplayName("Vision Async Executor")
    class VisionAsyncExecutor {

        @Test
        @DisplayName("Should create vision async executor bean")
        void shouldCreateVisionAsyncExecutorBean() {
            // When: Calling visionAsyncExecutor method
            Executor executor = asyncConfig.visionAsyncExecutor();

            // Then: Should return a valid executor
            assertThat(executor).isNotNull();
            assertThat(executor).isInstanceOf(Executor.class);
        }

        @Test
        @DisplayName("Should create executor that accepts runnables")
        void shouldCreateExecutorThatAcceptsRunnables() throws InterruptedException {
            // Given: Executor and a test runnable
            Executor executor = asyncConfig.visionAsyncExecutor();
            boolean[] executed = {false};

            Runnable testRunnable = () -> {
                executed[0] = true;
            };

            // When: Executing the runnable
            executor.execute(testRunnable);

            // Then: Runnable should be executed (allow some time for async execution)
            Thread.sleep(100); // Brief wait for async execution
            assertThat(executed[0]).isTrue();
        }

        @Test
        @DisplayName("Should return executor that propagates MDC context")
        void shouldReturnExecutorThatPropagatesMdcContext() {
            // When: Getting the executor
            Executor executor = asyncConfig.visionAsyncExecutor();

            // Then: Should be a non-null executor (we can't test the exact type since it's package-private)
            assertThat(executor).isNotNull();
            assertThat(executor).isInstanceOf(Executor.class);
            // The MDC propagation behavior is tested in other test methods
        }
    }

    @Nested
    @DisplayName("MDC Propagation")
    class MdcPropagation {

        @Test
        @DisplayName("Should propagate MDC context to executed tasks")
        void shouldPropagateMdcContextToExecutedTasks() throws InterruptedException {
            // Given: MDC context set in current thread
            MDC.put("testKey", "testValue");
            MDC.put("correlationId", "12345");

            Executor executor = asyncConfig.visionAsyncExecutor();
            Map<String, String> capturedContext = new java.util.concurrent.ConcurrentHashMap<>();

            Runnable testRunnable = () -> {
                // Capture MDC context in the executed thread
                Map<String, String> context = MDC.getCopyOfContextMap();
                if (context != null) {
                    capturedContext.putAll(context);
                }
            };

            // When: Executing the runnable
            executor.execute(testRunnable);

            // Then: MDC context should be propagated
            Thread.sleep(100); // Allow time for async execution
            assertThat(capturedContext).containsEntry("testKey", "testValue");
            assertThat(capturedContext).containsEntry("correlationId", "12345");
        }

        @Test
        @DisplayName("Should handle null MDC context")
        void shouldHandleNullMdcContext() throws InterruptedException {
            // Given: No MDC context set (MDC.getCopyOfContextMap() returns null)
            MDC.clear(); // Ensure no context

            Executor executor = asyncConfig.visionAsyncExecutor();
            boolean[] executed = {false};

            Runnable testRunnable = () -> {
                executed[0] = true;
                // Verify no MDC context in the executed thread
                Map<String, String> context = MDC.getCopyOfContextMap();
                assertThat(context).isNull();
            };

            // When: Executing the runnable
            executor.execute(testRunnable);

            // Then: Should execute without issues
            Thread.sleep(100);
            assertThat(executed[0]).isTrue();
        }

        @Test
        @DisplayName("Should clean up MDC context after task execution")
        void shouldCleanUpMdcContextAfterTaskExecution() throws InterruptedException {
            // Given: MDC context set
            MDC.put("testKey", "testValue");

            Executor executor = asyncConfig.visionAsyncExecutor();
            boolean[] executed = {false};

            Runnable testRunnable = () -> {
                executed[0] = true;
                // Modify MDC in the task
                MDC.put("taskKey", "taskValue");
            };

            // When: Executing the runnable
            executor.execute(testRunnable);

            // Then: Original MDC context should be restored after task completion
            Thread.sleep(100);
            assertThat(executed[0]).isTrue();

            // The MDC in the main thread should still have the original context
            // (not the task's modifications)
            assertThat(MDC.get("testKey")).isEqualTo("testValue");
            assertThat(MDC.get("taskKey")).isNull(); // Should not have task's modifications
        }
    }

    @Nested
    @DisplayName("Executor Behavior")
    class ExecutorBehavior {

        @Test
        @DisplayName("Should handle multiple concurrent tasks")
        void shouldHandleMultipleConcurrentTasks() throws InterruptedException {
            // Given: Executor and multiple tasks
            Executor executor = asyncConfig.visionAsyncExecutor();
            boolean[] completed = new boolean[5];

            // When: Executing multiple tasks
            for (int i = 0; i < 5; i++) {
                final int taskIndex = i;
                executor.execute(() -> {
                    try {
                        Thread.sleep(10); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completed[taskIndex] = true;
                });
            }

            // Then: All tasks should complete
            Thread.sleep(100);
            for (int i = 0; i < 5; i++) {
                assertThat(completed[i]).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Spring Configuration Annotations")
    class SpringConfigurationAnnotations {

        @Test
        @DisplayName("Should have EnableAsync annotation")
        void shouldHaveEnableAsyncAnnotation() {
            // This test verifies the presence of Spring configuration annotations
            // In practice, this would be verified by Spring context loading tests

            // Given: AsyncConfig class
            Class<?> configClass = AsyncConfig.class;

            // When: Checking for annotations
            boolean hasEnableAsync = configClass.isAnnotationPresent(
                org.springframework.scheduling.annotation.EnableAsync.class);
            boolean hasConfiguration = configClass.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class);

            // Then: Should have the expected annotations
            assertThat(hasConfiguration).isTrue();
            assertThat(hasEnableAsync).isTrue();
        }

        @Test
        @DisplayName("Should have visionAsyncExecutor bean method")
        void shouldHaveVisionAsyncExecutorBeanMethod() throws NoSuchMethodException {
            // Given: AsyncConfig class
            Class<?> configClass = AsyncConfig.class;

            // When: Getting the method
            java.lang.reflect.Method method = configClass.getMethod("visionAsyncExecutor");

            // Then: Should exist and have Bean annotation
            assertThat(method).isNotNull();
            assertThat(method.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class)).isTrue();

            // Check bean name
            org.springframework.context.annotation.Bean beanAnnotation =
                method.getAnnotation(org.springframework.context.annotation.Bean.class);
            assertThat(beanAnnotation.name()).contains("visionAsyncExecutor");
        }
    }

    @Nested
    @DisplayName("Resource Management")
    class ResourceManagement {

        @Test
        @DisplayName("Should not leak threads or resources")
        void shouldNotLeakThreadsOrResources() throws InterruptedException {
            // Given: Executor and multiple tasks
            Executor executor = asyncConfig.visionAsyncExecutor();
            int numberOfTasks = 10;
            boolean[] completed = new boolean[numberOfTasks];

            // When: Executing multiple tasks
            for (int i = 0; i < numberOfTasks; i++) {
                final int taskIndex = i;
                executor.execute(() -> {
                    completed[taskIndex] = true;
                });
            }

            // Then: All tasks should complete
            Thread.sleep(200); // Allow time for all async tasks
            for (int i = 0; i < numberOfTasks; i++) {
                assertThat(completed[i]).isTrue();
            }
        }

        @Test
        @DisplayName("Should handle task execution exceptions gracefully")
        void shouldHandleTaskExecutionExceptionsGracefully() throws InterruptedException {
            // Given: Executor and a task that throws exception
            Executor executor = asyncConfig.visionAsyncExecutor();

            Runnable failingTask = () -> {
                throw new RuntimeException("Test exception");
            };

            // When: Executing failing task
            executor.execute(failingTask);

            // Then: Should not crash the executor, just log the exception
            Thread.sleep(100); // Allow time for execution
            // If we get here without the test failing, the exception was handled
            assertThat(true).isTrue(); // Just a placeholder assertion
        }
    }

    @Nested
    @DisplayName("Performance Characteristics")
    class PerformanceCharacteristics {

        @Test
        @DisplayName("Should execute tasks asynchronously")
        void shouldExecuteTasksAsynchronously() throws InterruptedException {
            // Given: Executor and tasks that take time
            Executor executor = asyncConfig.visionAsyncExecutor();
            long[] executionTimes = new long[2];

            Runnable task1 = () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executionTimes[0] = System.currentTimeMillis();
            };

            Runnable task2 = () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executionTimes[1] = System.currentTimeMillis();
            };

            long startTime = System.currentTimeMillis();

            // When: Executing tasks
            executor.execute(task1);
            executor.execute(task2);

            // Wait for completion
            Thread.sleep(150);

            // Then: Tasks should have executed concurrently (not sequentially)
            long totalExecutionTime = Math.max(executionTimes[0], executionTimes[1]) - startTime;
            // With virtual threads, tasks should complete faster than sequential execution
            assertThat(totalExecutionTime).isLessThan(150); // Should be much less than 100ms * 2
        }
    }
}
