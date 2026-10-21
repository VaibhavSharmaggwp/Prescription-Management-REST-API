package com.vectoxlabs.prescription_api.repository;

import com.vectoxlabs.prescription_api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
