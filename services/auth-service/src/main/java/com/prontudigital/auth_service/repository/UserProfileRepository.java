package com.prontudigital.auth_service.repository;


import com.prontudigital.auth_service.model.UserRole;
import com.prontudigital.auth_service.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserRole, UserRoleId> {
}