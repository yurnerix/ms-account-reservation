package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client", uniqueConstraints = {@UniqueConstraint(name = "uk_client_mdm_id", columnNames = "mdm_id")})
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "mdm_id", nullable = false)
    private Long mdmId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", nullable = false, length = 100)
    private String middleName;

    @Column(name = "citizenship", length = 100)
    private String citizenship;

    @Column(name = "client_type", nullable = false, length = 50)
    private String clientType;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Column(name = "document_series", length = 100)
    private String documentSeries;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Client(Long mdmId, String firstName, String lastName, String middleName, String citizenship, String clientType, String documentNumber, String documentSeries, String documentType) {
        this.mdmId = mdmId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.citizenship = citizenship;
        this.clientType = clientType;
        this.documentNumber = documentNumber;
        this.documentSeries = documentSeries;
        this.documentType = documentType;
        this.status = ClientStatus.ACTIVE;

    }

    @PrePersist
    private void beforeCreate()
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (status == null)
        {
            status = ClientStatus.ACTIVE;
        }

        if (createdAt == null)
        {
            createdAt = now;
        }

        updatedAt = now;

    }

    @PreUpdate
    private void beforeUpdate()
    {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
