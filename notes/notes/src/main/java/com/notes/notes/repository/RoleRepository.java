package com.notes.notes.repository;

import com.notes.notes.entity.AppRole;
import com.notes.notes.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole appRole);

}

