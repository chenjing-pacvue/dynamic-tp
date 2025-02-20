package org.dromara.dynamictp.spring;

import java.util.concurrent.ThreadPoolExecutor;
import static org.dromara.dynamictp.core.DtpRegistry.DEFAULT_DTP;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.support.DynamicTp;
import org.dromara.dynamictp.core.support.ThreadPoolBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;


@Configuration
public class DtpDefaultConfiguration {

    @Order(10)
    @DynamicTp(DEFAULT_DTP)
    @Bean
    public DtpExecutor defaultEagerDtpExecutor() {
        return ThreadPoolBuilder.newBuilder()
            .threadPoolName(DEFAULT_DTP)
            .threadFactory("test-eager")
            .corePoolSize(1)
            .maximumPoolSize(1)
            .queueCapacity(0)
            .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
            .eager(true)
            .buildDynamic();
    }
}
