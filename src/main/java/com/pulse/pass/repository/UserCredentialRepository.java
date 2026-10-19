package com.pulse.pass.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse.pass.domain.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
