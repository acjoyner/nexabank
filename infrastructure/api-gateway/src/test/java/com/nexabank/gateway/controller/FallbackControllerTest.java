package com.nexabank.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void serviceUnavailable_returns503ProblemDetail() {
        ProblemDetail detail = controller.serviceUnavailable();

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void serviceUnavailable_hasDescriptiveTitle() {
        ProblemDetail detail = controller.serviceUnavailable();

        assertThat(detail.getTitle()).isNotBlank();
    }

    @Test
    void serviceUnavailable_hasDetail() {
        ProblemDetail detail = controller.serviceUnavailable();

        assertThat(detail.getDetail()).isNotBlank();
    }
}
