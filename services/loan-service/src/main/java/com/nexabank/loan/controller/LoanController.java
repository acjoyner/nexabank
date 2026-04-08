package com.nexabank.loan.controller;

import com.nexabank.loan.dto.LoanApplicationRequest;
import com.nexabank.loan.dto.LoanApplicationResponse;
import com.nexabank.loan.model.LoanStatus;
import com.nexabank.loan.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan application management")
public class LoanController {

    private final LoanApplicationService loanService;

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a loan application (AI-assisted eligibility scoring)")
    public ResponseEntity<LoanApplicationResponse> apply(
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.apply(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan application by ID")
    public ResponseEntity<LoanApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loan applications for a customer")
    public ResponseEntity<List<LoanApplicationResponse>> getByCustomer(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(loanService.getByCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update loan status (manual override by officer)")
    public ResponseEntity<LoanApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Email", defaultValue = "system") String reviewerEmail) {
        LoanStatus newStatus = LoanStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(loanService.updateStatus(id, newStatus, reviewerEmail));
    }
}
