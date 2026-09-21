package com.example.oms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Order Management System API",
                version = "1.0",
                description = "Products, cart, orders, payments and SQL reports"),
        servers = @Server(url = "/", description = "Current host"))
public class OpenApiConfig {
}