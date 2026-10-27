package com.devsu.cuenta.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final RestTemplate restTemplate;

    @Value("${cliente.service.url}")
    private String clientServiceUrl;

    public ClientDTO findById(Long clientId) {
        String url = clientServiceUrl + "/" + clientId;
        return restTemplate.getForObject(url, ClientDTO.class);
    }

    public String getClientName(Long clientId) {
        try {
            return findById(clientId).getName();
        } catch (Exception e) {
            return "Cliente " + clientId;
        }
    }
}