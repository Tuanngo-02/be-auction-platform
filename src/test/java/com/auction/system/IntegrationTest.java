package com.auction.system;

import com.auction.system.config.AsyncSyncConfiguration;
import com.auction.system.config.DatabaseTestcontainer;
import com.auction.system.config.RedisTestContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        AuctionSystemApp.class,
        AsyncSyncConfiguration.class,
        com.auction.system.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
    }
)
public @interface IntegrationTest {}
