package by.yurnerix.msaccountreservation.controller;


import by.yurnerix.msaccountreservation.generated.dto.ClientReportResponseDto;
import by.yurnerix.msaccountreservation.service.ClientReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientReportController {

    private final ClientReportService clientReportService;

    @GetMapping("/{clientId}/report")
    public CompletableFuture<ResponseEntity<ClientReportResponseDto>> getClientReport(@PathVariable("clientId") UUID clientId) {
        return clientReportService
                .getClientReport(clientId)
                .thenApply(ResponseEntity::ok);
    }
}
