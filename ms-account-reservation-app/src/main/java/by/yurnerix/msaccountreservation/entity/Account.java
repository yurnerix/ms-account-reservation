package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    public Account(AccountStatus status, Client client, String accountType, String currencyCode) {
        this.status = status;
        this.client = client;
        this.accountType = accountType;
        this.currencyCode = currencyCode;
    }

}
