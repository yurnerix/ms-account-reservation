package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

}
