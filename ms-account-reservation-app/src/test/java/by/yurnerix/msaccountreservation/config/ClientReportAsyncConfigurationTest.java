package by.yurnerix.msaccountreservation.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class ClientReportAsyncConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withUserConfiguration(ClientReportAsyncConfiguration.class)
                    .withPropertyValues(
                            "app.async.core-pool-size=2",
                            "app.async.max-pool-size=4",
                            "app.async.queue-capacity=17",
                            "app.async.task-timeout=750ms"
                    );

    @Test
    void shouldCreateConfiguredClientReportExecutor() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(AsyncProperties.class);

            assertThat(context)
                    .hasSingleBean(ThreadPoolTaskExecutor.class);

            assertThat(context)
                    .hasBean(ClientReportAsyncConfiguration.CLIENT_REPORT_EXECUTOR);

            AsyncProperties properties = context.getBean(AsyncProperties.class);

            assertThat(properties.getCorePoolSize())
                    .isEqualTo(2);

            assertThat(properties.getMaxPoolSize())
                    .isEqualTo(4);

            assertThat(properties.getQueueCapacity())
                    .isEqualTo(17);

            assertThat(properties.getTaskTimeout())
                    .isEqualTo(Duration.ofMillis(750));

            ThreadPoolTaskExecutor executor = context.getBean(ClientReportAsyncConfiguration.CLIENT_REPORT_EXECUTOR, ThreadPoolTaskExecutor.class);

            assertThat(executor.getCorePoolSize())
                    .isEqualTo(2);

            assertThat(executor.getMaxPoolSize())
                    .isEqualTo(4);

            assertThat(executor.getThreadNamePrefix())
                    .isEqualTo("client-report-");

            assertThat(executor.getThreadPoolExecutor()
                    .getQueue()
                    .remainingCapacity())
                    .isEqualTo(17);

            assertThat(executor.getThreadPoolExecutor()
                    .getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        });
    }

    @Test
    void shouldExecuteTaskInClientReportThread() {
        contextRunner.run(context -> {
            ThreadPoolTaskExecutor executor = context.getBean(ClientReportAsyncConfiguration.CLIENT_REPORT_EXECUTOR, ThreadPoolTaskExecutor.class);

            String threadName = CompletableFuture
                    .supplyAsync(() -> Thread.currentThread().getName(), executor)
                    .join();

            assertThat(threadName)
                    .startsWith("client-report-");
        });
    }

    @Test
    void shouldFailWhenMaxPoolSizeIsLessThanCorePoolSize() {
        new ApplicationContextRunner()
                .withUserConfiguration(ClientReportAsyncConfiguration.class)
                .withPropertyValues(
                        "app.async.core-pool-size=5",
                        "app.async.max-pool-size=2",
                        "app.async.queue-capacity=10",
                        "app.async.task-timeout=1s"
                )
                .run(context -> {
                    assertThat(context)
                            .hasFailed();

                    Throwable startupFailure = context.getStartupFailure();

                    assertThat(startupFailure)
                            .isNotNull();

                    assertThat(collectExceptionMessages(startupFailure))
                            .contains("app.async.max-pool-size");
                });
    }

    @Test
    void shouldFailWhenTaskTimeoutIsZero() {
        new ApplicationContextRunner()
                .withUserConfiguration(ClientReportAsyncConfiguration.class)
                .withPropertyValues(
                        "app.async.core-pool-size=2",
                        "app.async.max-pool-size=4",
                        "app.async.queue-capacity=10",
                        "app.async.task-timeout=0s"
                )
                .run(context -> {
                    assertThat(context)
                            .hasFailed();

                    Throwable startupFailure = context.getStartupFailure();

                    assertThat(startupFailure)
                            .isNotNull();

                    assertThat(collectExceptionMessages(startupFailure))
                            .contains("app.async.task-timeout");
                });
    }

    private String collectExceptionMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();

        Throwable current = throwable;

        while (current != null) {
            if (current.getMessage() != null) {
                messages
                        .append(current.getMessage())
                        .append(System.lineSeparator());
            }

            if (current.getCause() == current) {
                break;
            }

            current = current.getCause();
        }

        return messages.toString();
    }

}
