package by.yurnerix.msaccountreservation.repository;

import by.yurnerix.msaccountreservation.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

}
