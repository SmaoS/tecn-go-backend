package com.tecngo.shared.config;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAdmin(@Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            if (!users.existsByEmailIgnoreCase(email)) {
                users.save(User.builder()
                        .fullName("TecnGo Admin")
                        .email(email.toLowerCase())
                        .password(passwordEncoder.encode(password))
                        .role(Role.ADMIN)
                        .build());
            }
        };
    }
}
