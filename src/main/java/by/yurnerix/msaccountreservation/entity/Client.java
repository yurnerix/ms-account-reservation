package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    @PrePersist
    private void beforeCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (status == null) {
            status = ClientStatus.ACTIVE;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
