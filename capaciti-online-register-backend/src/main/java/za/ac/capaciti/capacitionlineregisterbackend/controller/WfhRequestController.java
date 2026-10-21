package za.ac.capaciti.capacitionlineregisterbackend.controller;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.wfh.CreateWfhRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.wfh.WfhActionResponse;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.wfh.WfhDecisionRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.wfh.WfhRequestDto;
import za.ac.capaciti.capacitionlineregisterbackend.domain.enums.WfhStatus;
import za.ac.capaciti.capacitionlineregisterbackend.service.WfhRequestService;

@RestController
@RequestMapping("/wfh-requests")
public class WfhRequestController {
    private final WfhRequestService wfhRequestService;

    @Autowired
    public WfhRequestController(WfhRequestService wfhRequestService) {
        this.wfhRequestService = wfhRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<WfhActionResponse> create(@Valid @RequestBody CreateWfhRequest request,
                                                    Authentication authentication) {
        WfhActionResponse response = wfhRequestService.create(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Page<WfhRequestDto>> mine(Authentication authentication,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(wfhRequestService.mine(authentication.getName(), PageRequest.of(page, size)),
                HttpStatus.OK);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<WfhRequestDto> cancel(Authentication authentication, @PathVariable UUID id) {
        return new ResponseEntity<>(wfhRequestService.cancel(id, authentication.getName()), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<Page<WfhRequestDto>> search(@RequestParam(required = false) WfhStatus status,
                                                      @RequestParam(required = false) UUID userId,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(wfhRequestService.search(status, userId, from, to, PageRequest.of(page, size)),
                HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<WfhRequestDto> getById(@PathVariable UUID id) {
        return new ResponseEntity<>(wfhRequestService.getById(id), HttpStatus.OK);
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<WfhRequestDto> decide(Authentication authentication,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody WfhDecisionRequest request) {
        return new ResponseEntity<>(wfhRequestService.decide(id, request, authentication.getName()), HttpStatus.OK);
    }
}
