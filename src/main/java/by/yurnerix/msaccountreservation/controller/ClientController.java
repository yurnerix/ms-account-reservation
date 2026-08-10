package by.yurnerix.msaccountreservation.controller;

import by.yurnerix.msaccountreservation.generated.api.ClientsApi;
import by.yurnerix.msaccountreservation.generated.dto.*;
import by.yurnerix.msaccountreservation.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientController implements ClientsApi {

    private final ClientService clientService;


    @Override
    public ResponseEntity<ClientResponseDto> createClient(CreateClientRequestDto request) {
        ClientResponseDto response = clientService.createClient(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{clientId}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }


    @Override
    public ResponseEntity<ClientDetailsResponseDto> getClient(UUID clientId) {
        ClientDetailsResponseDto response = clientService.getClient(clientId);

        return ResponseEntity.ok(response);

    }

    @Override
    public ResponseEntity<ClientPageResponseDto> searchClients(Integer page, Integer size, String lastName, Long mdmId) {
        ClientPageResponseDto response = clientService.searchClients(page, size, lastName, mdmId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClientResponseDto> updateClient(UUID clientId, UpdateClientRequestDto request) {
        ClientResponseDto response = clientService.updateClient(clientId, request);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteClient(UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClientExistsResponseDto> checkClientExists(UUID clientId) {
        ClientExistsResponseDto response = clientService.checkClientExists(clientId);

        return ResponseEntity.ok(response);
    }


}
