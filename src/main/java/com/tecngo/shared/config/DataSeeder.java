package com.tecngo.shared.config;

import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final ServiceCategoryRepository categories;

    @Bean
    CommandLineRunner seedCategories() {
        return args -> {
            seed("Electricista", "electricista", "Instalaciones y reparaciones eléctricas");
            seed("Plomero", "plomero", "Fugas, tuberías y aparatos sanitarios");
            seed("Técnico de computadores", "tecnico-computadores", "Soporte, mantenimiento y reparación");
            seed("Técnico de celulares", "tecnico-celulares", "Diagnóstico y reparación de dispositivos");
            seed("Aire acondicionado", "aire-acondicionado", "Instalación y mantenimiento");
            seed("Cámaras de seguridad", "camaras-seguridad", "Instalación y mantenimiento de videovigilancia");
            seed("Internet / redes", "internet-redes", "Redes, Wi-Fi y conectividad");
            seed("Cerrajería", "cerrajeria", "Apertura, cambio e instalación de cerraduras");
        };
    }

    private void seed(String name, String slug, String description) {
        if (!categories.existsBySlug(slug)) categories.save(category(name, slug, description));
    }

    private ServiceCategory category(String name, String slug, String description) {
        return ServiceCategory.builder().name(name).slug(slug).description(description).active(true).build();
    }
}
