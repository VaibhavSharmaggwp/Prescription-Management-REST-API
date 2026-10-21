package com.vectoxlabs.prescription_api.repository;

import com.vectoxlabs.prescription_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
