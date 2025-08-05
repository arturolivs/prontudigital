package com.prontudigital.user_service.repository;


import com.prontudigital.user_service.model.UserProfile;
import com.prontudigital.user_service.model.UserProfileId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UserProfileId> {
}