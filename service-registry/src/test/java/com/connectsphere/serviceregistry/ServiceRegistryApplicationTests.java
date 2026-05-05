package com.connectsphere.serviceregistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ServiceRegistryApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void applicationContext_ShouldNotBeNull(ApplicationContext context) {
        assertNotNull(context);
    }

    @Test
    void eurekaServerAnnotation_ShouldBePresent() {
        boolean hasAnnotation = context.getBeansWithAnnotation(EnableEurekaServer.class).size() > 0;

        assertTrue(hasAnnotation, "Eureka Server should be enabled");
    }
}
