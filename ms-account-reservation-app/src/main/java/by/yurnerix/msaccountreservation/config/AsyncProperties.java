package by.yurnerix.msaccountreservation.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

    @Min(1)
    private int corePoolSize = 3;

    @Min(1)
    private int maxPoolSize = 6;

    @Min(0)
    private int queueCapacity = 100;

    @NotNull
    private Duration taskTimeout = Duration.ofSeconds(5);

    @AssertTrue(message = "app.async.max-pool-size must be greater than or equal " + "to app.async.core-pool-size")
    public boolean isPoolSizeValid() {
        return maxPoolSize >= corePoolSize;
    }

    @AssertTrue(message = "app.async.task-timeout must be greater than zero")
    public boolean isTaskTimeoutValid() {
        return taskTimeout == null || (!taskTimeout.isZero() && !taskTimeout.isNegative());
    }
}
