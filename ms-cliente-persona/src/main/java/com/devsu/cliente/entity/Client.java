package com.devsu.cliente.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "client")
public class Client extends Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientId;
    @Column(nullable = false)
    private String password;
    private Boolean status;

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
}
