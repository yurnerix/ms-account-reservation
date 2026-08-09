package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.exception.ClientAlreadyExistsException;
import by.yurnerix.msaccountreservation.exception.ClientHasActiveAccountException;
import by.yurnerix.msaccountreservation.exception.ClientNotFoundException;
import by.yurnerix.msaccountreservation.generated.dto.*;
import by.yurnerix.msaccountreservation.mapper.ClientMapper;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private UUID clientId;
    private Client client;

    @BeforeEach
    void setUp()
    {
        clientId = UUID.randomUUID();
        client = new Client();

        client.setId(clientId);
        client.setMdmId(1234567890L);
        client.setFirstName("Иван");
        client.setLastName("Петров");
        client.setStatus(ClientStatus.ACTIVE);
    }

    @Test
    void createClientShouldReturnCreatedClient()
    {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setMdmId(1234567890L);

        ClientResponseDto expectedResponse = new ClientResponseDto();
        expectedResponse.setId(clientId);

        when(clientRepository.existsByMdmId(1234567890L))
                .thenReturn(false);

        when(clientMapper.toEntity(request))
                .thenReturn(client);

        when(clientRepository.saveAndFlush(client))
                .thenReturn(client);

        when(clientMapper.toResponse(client))
                .thenReturn(expectedResponse);

        ClientResponseDto actualResponse = clientService.createClient(request);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository)
                .saveAndFlush(client);

        verify(clientMapper)
                .toResponse(client);

    }

    @Test
    void createClientShouldThrowWhenMdmIdAlreadyExists()
    {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setMdmId(1234567890L);

        when(clientRepository.existsByMdmId(1234567890L))
                .thenReturn(true);

        assertThrows(ClientAlreadyExistsException.class, () -> clientService.createClient(request));

        verify(clientRepository, never())
                .saveAndFlush(any(Client.class));

        verifyNoInteractions(clientMapper);

    }

    @Test
    void getClientShouldThrowWhenClientDoesNotExist()
    {
        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.getClient(clientId));

        verifyNoInteractions(clientMapper);

    }

    @Test
    void getClientShouldReturnClientWhenClientExists()
    {
        ClientDetailsResponseDto expectedResponse = new ClientDetailsResponseDto();

        expectedResponse.setId(clientId);

        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED
        )).thenReturn(Optional.of(client));

        when(clientMapper.toDetailsResponse(client))
                .thenReturn(expectedResponse);

        ClientDetailsResponseDto actualResponse = clientService.getClient(clientId);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository)
                .findByIdAndStatusNot(clientId, ClientStatus.DELETED);

        verify(clientMapper)
                .toDetailsResponse(client);
    }

    @Test
    void updateClientShouldUpdateAndReturnClient()
    {
        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Иван");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        ClientResponseDto expectedResponse = new ClientResponseDto();
        expectedResponse.setId(clientId);

        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));

        when(clientRepository.saveAndFlush(client))
                .thenReturn(client);

        when(clientMapper.toResponse(client))
                .thenReturn(expectedResponse);

        ClientResponseDto actualResponse = clientService.updateClient(clientId, request);

        assertSame(expectedResponse, actualResponse);

        verify(clientMapper)
                .updateEntity(client, request);

        verify(clientRepository)
                .saveAndFlush(client);

    }

    @Test
    void updateClientShouldThrowWhenClientDoesNotExist()
    {
        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Иван");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED
        )).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.updateClient(clientId, request));

        verify(clientMapper, never())
                .updateEntity(any(Client.class), any(UpdateClientRequestDto.class));

        verify(clientRepository, never())
                .saveAndFlush(any(Client.class));
    }

    @Test
    void deleteClientShouldThrowWhenClientHasActiveAccounts()
    {
        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));

        when(accountRepository.existsByClientIdAndStatusNameIn(eq(clientId), anyCollection()
        )).thenReturn(true);

        assertThrows(ClientHasActiveAccountException.class, () -> clientService.deleteClient(clientId));

        assertEquals(ClientStatus.ACTIVE, client.getStatus());

        verify(clientRepository, never())
                .save(any(Client.class));
    }

    @Test
    void checkClientExistsShouldReturnResponseWhenClientExists()
    {
        ClientExistsResponseDto expectedResponse = new ClientExistsResponseDto();

        expectedResponse.setExists(true);
        expectedResponse.setClientId(clientId);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(client));

        when(clientMapper.toExistsResponse(clientId, client))
                .thenReturn(expectedResponse);

        ClientExistsResponseDto actualResponse = clientService.checkClientExists(clientId);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository)
                .findById(clientId);

        verify(clientMapper)
                .toExistsResponse(clientId, client);
    }

    @Test
    void checkClientExistsShouldReturnResponseWhenClientDoesNotExist()
    {
        ClientExistsResponseDto expectedResponse = new ClientExistsResponseDto();

        expectedResponse.setExists(false);
        expectedResponse.setClientId(clientId);

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.empty());

        when(clientMapper.toExistsResponse(clientId, null))
                .thenReturn(expectedResponse);

        ClientExistsResponseDto actualResponse = clientService.checkClientExists(clientId);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository)
                .findById(clientId);

        verify(clientMapper)
                .toExistsResponse(clientId, null);
    }

    @Test
    void searchClientsShouldReturnPageResponse()
    {
        Page<Client> clientsPage = new PageImpl<>(List.of(client));

        ClientPageResponseDto expectedResponse = new ClientPageResponseDto();

        when(clientRepository.findAll(any(Specification.class), any(Pageable.class)
        )).thenReturn(clientsPage);

        when(clientMapper.toPageResponse(clientsPage))
                .thenReturn(expectedResponse);

        ClientPageResponseDto actualResponse = clientService.searchClients(0, 20, null, null);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository).findAll(any(Specification.class), any(Pageable.class));

        verify(clientMapper)
                .toPageResponse(clientsPage);
    }

    @Test
    void deleteClientShouldSetDeletedStatus()
    {
        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));

        when(accountRepository.existsByClientIdAndStatusNameIn(eq(clientId), anyCollection()
        )).thenReturn(false);

        clientService.deleteClient(clientId);

        assertEquals(ClientStatus.DELETED, client.getStatus());

        verify(clientRepository)
                .save(client);
    }

}
