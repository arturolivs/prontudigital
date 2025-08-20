package com.prontudigital.auth_service.config;

import com.prontudigital.auth_service.model.Role;
import com.prontudigital.auth_service.model.User;
import com.prontudigital.auth_service.repository.RoleRepository;
import com.prontudigital.auth_service.repository.UserRepository;
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
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            if (roleRepository.count() == 0) {
                List<Role> roles = List.of(
                        Role.builder()
                                .name("ADMIN")
                                .description("Administrador do sistema com acesso total")
                                .users(new HashSet<>())
                                .build(),
                        Role.builder()
                                .name("NURSE")
                                .description("Profissional de enfermagem")
                                .users(new HashSet<>())
                                .build(),
                        Role.builder()
                                .name("RECEP")
                                .description("Recepcionista da clínica")
                                .users(new HashSet<>())
                                .build(),
                        Role.builder()
                                .name("FINANCE")
                                .description("Responsável financeiro")
                                .users(new HashSet<>())
                                .build(),
                        Role.builder()
                                .name("PATIENT")
                                .description("Paciente da clínica")
                                .users(new HashSet<>())
                                .build()
                );

                roleRepository.saveAll(roles);
            }

            // Criar usuário admin padrão se não existir
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("Perfil ADMIN não encontrado"));

                User admin = User.builder()
                        .username("admin")
                        .email("admin@clinica.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .fullName("Administrador do Sistema")
                        .isActive(true)
                        .roles(new HashSet<>())
                        .build();

                admin.addRole(adminRole);
                userRepository.save(admin);
            }
        };
    }
}