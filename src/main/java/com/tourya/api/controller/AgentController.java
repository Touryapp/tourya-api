package com.tourya.api.controller;

import com.tourya.api.agents.concierge.ConciergeChatRequest;
import com.tourya.api.agents.concierge.ConciergeChatResponse;
import com.tourya.api.agents.concierge.TravelConciergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IA-02: endpoints REST de los agentes IA. Hoy solo expone Travel Concierge;
 * futuros agentes (Support 24/7, Operator Support, etc.) se agregaran aca.
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "Agents IA", description = "Agentes IA (Travel Concierge y siguientes)")
public class AgentController {

    private final TravelConciergeService travelConciergeService;

    @Operation(
            operationId = "travelConciergeChat",
            summary = "Chat con el agente Travel Concierge (IA-02)",
            description = "Envia un mensaje del turista al agente. El agente puede responder texto natural o ejecutar function calls contra busqueda/carrito. Requiere JWT."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/travel-concierge/chat")
    public ResponseEntity<ConciergeChatResponse> chat(
            @Valid @RequestBody ConciergeChatRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(travelConciergeService.chat(request, connectedUser));
    }
}
