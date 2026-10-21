package za.ac.capaciti.capacitionlineregisterbackend.controller;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.ticket.CreateTicketRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.ticket.ResolveTicketRequest;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.ticket.TicketActionResponse;
import za.ac.capaciti.capacitionlineregisterbackend.domain.dtos.ticket.TicketDto;
import za.ac.capaciti.capacitionlineregisterbackend.domain.enums.TicketStatus;
import za.ac.capaciti.capacitionlineregisterbackend.domain.enums.TicketType;
import za.ac.capaciti.capacitionlineregisterbackend.domain.enums.WorkMode;
import za.ac.capaciti.capacitionlineregisterbackend.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<TicketActionResponse> create(Authentication authentication,
                                                     @Valid @RequestBody CreateTicketRequest request) {
        return new ResponseEntity<>(ticketService.create(authentication.getName(), request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Page<TicketDto>> mine(Authentication authentication,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(ticketService.mine(authentication.getName(), PageRequest.of(page, size)),
                HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<Page<TicketDto>> search(@RequestParam(required = false) TicketStatus status,
                                                  @RequestParam(required = false) TicketType type,
                                                  @RequestParam(required = false) WorkMode workMode,
                                                  @RequestParam(required = false) Boolean prolonged,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return new ResponseEntity<>(ticketService.search(status, type, workMode, prolonged,
                PageRequest.of(page, size)), HttpStatus.OK);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TECH_CHAMP','ADMIN')")
    public ResponseEntity<TicketDto> resolve(Authentication authentication,
                                            @PathVariable UUID id,
                                            @Valid @RequestBody ResolveTicketRequest request) {
        return new ResponseEntity<>(ticketService.resolve(id, request, authentication.getName()), HttpStatus.OK);
    }
}
