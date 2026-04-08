package com.nexabank.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * NexaBank Service Registry — Eureka Server
 *
 * All microservices register here at startup and use it to discover
 * each other by name (e.g. "account-service") instead of hardcoded URLs.
 * This is a core Spring Cloud pattern for microservice architectures.
 *
 * See docs/learning/02-spring-cloud-eureka-gateway.md for more details.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
