package by.yurnerix.msaccountreservation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "account_status")
public class AccountStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    public AccountStatus() {

    }

    public AccountStatus(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
