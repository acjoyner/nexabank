package com.nexabank.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.cloud.config.server.git.uri=file:./target/config-repo",
    "spring.cloud.config.server.git.clone-on-start=false"
})
@ActiveProfiles("test")
class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
    }
}
