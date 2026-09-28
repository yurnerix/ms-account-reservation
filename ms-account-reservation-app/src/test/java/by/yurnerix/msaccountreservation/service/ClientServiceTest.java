package by.yurnerix.msaccountreservation.service;

import by.yurnerix.msaccountreservation.entity.AccountStatusName;
import by.yurnerix.msaccountreservation.entity.Client;
import by.yurnerix.msaccountreservation.entity.ClientStatus;
import by.yurnerix.msaccountreservation.exception.ClientAlreadyExistsException;
import by.yurnerix.msaccountreservation.exception.ClientHasActiveAccountException;
import by.yurnerix.msaccountreservation.exception.ClientNotFoundException;
import by.yurnerix.msaccountreservation.generated.dto.*;
import by.yurnerix.msaccountreservation.mapper.ClientMapper;
import by.yurnerix.msaccountreservation.repository.AccountRepository;
import by.yurnerix.msaccountreservation.repository.ClientRepository;
import by.yurnerix.msaccountreservation.repository.projection.ClientSearchProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void setUp() {
        clientId = UUID.randomUUID();

        client = Client.builder()
                .id(clientId)
                .mdmId(1234567890L)
                .firstName("Иван")
                .lastName("Петров")
                .status(ClientStatus.ACTIVE)
                .build();
    }

    @Test
    void createClientShouldReturnCreatedClient() {
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
    void createClientShouldThrowWhenMdmIdAlreadyExists() {
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
    void getClientShouldThrowWhenClientDoesNotExist() {
        when(clientRepository.findDetailsByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.getClient(clientId));

        verify(clientRepository)
                .findDetailsByIdAndStatusNot(clientId, ClientStatus.DELETED);

        verifyNoInteractions(clientMapper);

    }

    @Test
    void getClientShouldReturnClientWhenClientExists() {
        ClientDetailsResponseDto expectedResponse = new ClientDetailsResponseDto();

        expectedResponse.setId(clientId);

        when(clientRepository.findDetailsByIdAndStatusNot(clientId, ClientStatus.DELETED
        )).thenReturn(Optional.of(client));

        when(clientMapper.toDetailsResponse(client))
                .thenReturn(expectedResponse);

        ClientDetailsResponseDto actualResponse = clientService.getClient(clientId);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository)
                .findDetailsByIdAndStatusNot(clientId, ClientStatus.DELETED);

        verify(clientMapper)
                .toDetailsResponse(client);
    }

    @Test
    void updateClientShouldUpdateAndReturnClient() {
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
                .updateEntity(request, client);

        verify(clientRepository)
                .saveAndFlush(client);

    }

    @Test
    void updateClientShouldThrowWhenClientDoesNotExist() {
        UpdateClientRequestDto request = new UpdateClientRequestDto();

        request.setFirstName("Иван");
        request.setLastName("Сидоров");
        request.setMiddleName("Александрович");

        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED
        )).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.updateClient(clientId, request));

        verify(clientMapper, never())
                .updateEntity(any(UpdateClientRequestDto.class), any(Client.class));

        verify(clientRepository, never())
                .saveAndFlush(any(Client.class));
    }

    @Test
    void deleteClientShouldThrowWhenClientHasActiveAccounts() {
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
    void checkClientExistsShouldReturnResponseWhenClientExists() {
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
    void checkClientExistsShouldReturnResponseWhenClientDoesNotExist() {
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
    void searchClientsShouldReturnPageResponse() {
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("lastName", "firstName", "id"));

        ClientSearchProjection projection = mock(ClientSearchProjection.class);

        Page<ClientSearchProjection> clientsPage = new PageImpl<>(List.of(projection), pageRequest, 1L);

        ClientPageResponseDto expectedResponse = new ClientPageResponseDto();

        when(clientRepository.searchWithActiveAccountsCount(
                ClientStatus.DELETED,
                AccountStatusName.activeStatuses(),
                null,
                null,
                pageRequest
        )).thenReturn(clientsPage);

        when(clientMapper.toPageResponse(clientsPage))
                .thenReturn(expectedResponse);

        ClientPageResponseDto actualResponse = clientService.searchClients(0, 20, null, null);

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository).searchWithActiveAccountsCount(
                ClientStatus.DELETED,
                AccountStatusName.activeStatuses(),
                null,
                null,
                pageRequest
        );

        verify(clientMapper).toPageResponse(clientsPage);
    }

    @Test
    void deleteClientShouldSetDeletedStatus() {
        when(clientRepository.findByIdAndStatusNot(clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));

        when(accountRepository.existsByClientIdAndStatusNameIn(eq(clientId), anyCollection()
        )).thenReturn(false);

        clientService.deleteClient(clientId);

        assertEquals(ClientStatus.DELETED, client.getStatus());

        verify(clientRepository)
                .save(client);
    }

    @Test
    void searchClientsShouldNormalizeLastNameAndPassMdmId() {
        Long mdmId = 1234567890L;

        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("lastName", "firstName", "id"));

        Page<ClientSearchProjection> clientsPage = Page.empty(pageRequest);

        ClientPageResponseDto expectedResponse = new ClientPageResponseDto();

        when(clientRepository.searchWithActiveAccountsCount(
                ClientStatus.DELETED,
                AccountStatusName.activeStatuses(),
                "%иван%",
                mdmId,
                pageRequest
        )).thenReturn(clientsPage);

        when(clientMapper.toPageResponse(clientsPage))
                .thenReturn(expectedResponse);

        ClientPageResponseDto actualResponse = clientService.searchClients(
                        0,
                        20,
                        "  ИВАН  ",
                        mdmId
                );

        assertSame(expectedResponse, actualResponse);

        verify(clientRepository).searchWithActiveAccountsCount(
                ClientStatus.DELETED,
                AccountStatusName.activeStatuses(),
                "%иван%",
                mdmId,
                pageRequest
        );

        verify(clientMapper).toPageResponse(clientsPage);
    }

}
