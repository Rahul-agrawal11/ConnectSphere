package com.connectsphere.serviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ConnectSphere Service Registry
 *
 * Acts as the Eureka Server for all microservices in the ConnectSphere platform.
 * All services register here on startup and use this registry for discovery.
 *
 * Dashboard: http://localhost:8761
 */

@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }

}
