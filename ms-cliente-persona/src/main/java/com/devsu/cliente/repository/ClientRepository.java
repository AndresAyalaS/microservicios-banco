package com.devsu.cliente.repository;
import com.devsu.cliente.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDocumentId(String documentId);
}
