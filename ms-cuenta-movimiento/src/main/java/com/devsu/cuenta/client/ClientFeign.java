package com.devsu.cuenta.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-cliente-persona", url = "${cliente.service.url}")
public interface ClientFeign {
    @GetMapping("/{id}")
    Object getClientById(@PathVariable("id") Long id);
}