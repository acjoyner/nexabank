package com.nexabank.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * NexaBank Config Server — Centralized Configuration Management
 *
 * All microservices fetch their configuration from here at startup
 * via spring.config.import: configserver:http://config-server:8888
 *
 * Uses native filesystem backend (config/ directory) — no Git dependency
 * needed for local dev. In production, this would point to a Git repo.
 *
 * See docs/learning/02-spring-cloud-eureka-gateway.md for more details.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
