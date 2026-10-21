package za.ac.capaciti.capacitionlineregisterbackend.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.prompt.PromptListResponse;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.prompt.SubmitPromptRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.prompt.SubmitPromptResponse;
import za.ac.capaciti.capacitionlineregisterbackend.service.PromptService;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private final PromptService promptService;

    @Autowired
    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PromptListResponse> active(Authentication authentication) {
        return new ResponseEntity<>(promptService.active(authentication.getName()), HttpStatus.OK);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<PromptListResponse> today(Authentication authentication) {
        return new ResponseEntity<>(promptService.today(authentication.getName()), HttpStatus.OK);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<SubmitPromptResponse> submit(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SubmitPromptRequest request) {
        return new ResponseEntity<>(promptService.submit(authentication.getName(), id, request), HttpStatus.CREATED);
    }
}