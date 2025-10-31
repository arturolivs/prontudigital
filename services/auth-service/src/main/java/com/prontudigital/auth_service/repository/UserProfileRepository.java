package com.prontudigital.auth_service.repository;


import com.prontudigital.auth_service.entity.UserRole;
import com.prontudigital.auth_service.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserRole, UserRoleId> {
}