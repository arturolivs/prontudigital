package com.prontudigital.schedule_service.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Schedule Service API")
                        .description("""
                            API de agendamentos do sistema ProntuDigital.
                            
                            ## Funcionalidades:
                            * **Agendar consulta** - Cria um novo agendamento
                            * **Cancelar consulta** - Cancela um agendamento existente
                            * **Visualizar agenda** - Consulta agenda por profissional e período
                            
                            ## Autenticação:
                            Todos os endpoints exigem token JWT no header Authorization.
                            O token deve ser obtido no Auth Service.
                            """)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Suporte ProntuDigital")
                                .email("suporte@prontudigital.com")
                                .url("https://prontudigital.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT no formato: Bearer {token}")));
    }
}