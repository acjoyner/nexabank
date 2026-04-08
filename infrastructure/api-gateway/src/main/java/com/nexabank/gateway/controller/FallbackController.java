package com.nexabank.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Fallback controller for circuit breaker responses.
 * When a downstream service is unavailable, the gateway routes here
 * instead of returning a raw 503 to the client.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/unavailable")
    public ProblemDetail serviceUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Service Temporarily Unavailable");
        problem.setDetail("The requested service is temporarily unavailable. Please try again shortly.");
        problem.setType(URI.create("https://nexabank.com/errors/service-unavailable"));
        return problem;
    }
}
