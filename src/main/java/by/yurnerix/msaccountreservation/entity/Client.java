package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

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

    @Column(name = "mdm_code", nullable = false)
    private Long mdmCode;


    public Client(String fullName, String citizenship, String clientType, String documentNumber, String documentSeries, String documentType, Long mdmCode) {
        this.fullName = fullName;
        this.citizenship = citizenship;
        this.clientType = clientType;
        this.documentNumber = documentNumber;
        this.documentSeries = documentSeries;
        this.documentType = documentType;
        this.mdmCode = mdmCode;
    }


}
