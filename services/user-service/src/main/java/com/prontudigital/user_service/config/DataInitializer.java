package com.prontudigital.user_service.config;

import com.prontudigital.user_service.model.Profile;
import com.prontudigital.user_service.model.User;
import com.prontudigital.user_service.repository.ProfileRepository;
import com.prontudigital.user_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   ProfileRepository profileRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            if (profileRepository.count() == 0) {
                List<Profile> profiles = List.of(
                        Profile.builder()
                                .name("ADMIN")
                                .description("Administrador do sistema com acesso total")
                                .users(new HashSet<>())
                                .build(),
                        Profile.builder()
                                .name("NURSE")
                                .description("Profissional de enfermagem")
                                .users(new HashSet<>())
                                .build(),
                        Profile.builder()
                                .name("RECEP")
                                .description("Recepcionista da clínica")
                                .users(new HashSet<>())
                                .build(),
                        Profile.builder()
                                .name("FINANCE")
                                .description("Responsável financeiro")
                                .users(new HashSet<>())
                                .build(),
                        Profile.builder()
                                .name("PATIENT")
                                .description("Paciente da clínica")
                                .users(new HashSet<>())
                                .build()
                );

                profileRepository.saveAll(profiles);
            }

            // Criar usuário admin padrão se não existir
            if (userRepository.count() == 0) {
                Profile adminProfile = profileRepository.findByName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("Perfil ADMIN não encontrado"));

                User admin = User.builder()
                        .userName("admin")
                        .email("admin@clinica.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .fullName("Administrador do Sistema")
                        .isActive(true)
                        .profiles(new HashSet<>())
                        .build();

                admin.addProfile(adminProfile);
                userRepository.save(admin);
            }
        };
    }
}