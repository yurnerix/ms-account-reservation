package by.yurnerix.msaccountreservation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AsyncProperties.class)
public class ClientReportAsyncConfiguration {

    public static final String CLIENT_REPORT_EXECUTOR = "clientReportExecutor";

    @Bean(name = CLIENT_REPORT_EXECUTOR)
    public ThreadPoolTaskExecutor clientReportExecutor(AsyncProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(properties.getCorePoolSize());

        executor.setMaxPoolSize(properties.getMaxPoolSize());

        executor.setQueueCapacity(properties.getQueueCapacity());

        executor.setThreadNamePrefix("client-report-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        return executor;
    }

}
