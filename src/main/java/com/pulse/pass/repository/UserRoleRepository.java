package com.pulse.pass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse.pass.domain.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {

    List<UserRole> findByUserId(Long userId);
}
