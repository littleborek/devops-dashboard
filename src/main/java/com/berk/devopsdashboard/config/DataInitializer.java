package com.berk.devopsdashboard.config;

import com.berk.devopsdashboard.entity.User;
import com.berk.devopsdashboard.entity.enums.Role;
import com.berk.devopsdashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@devops.com")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Varsayılan admin hesabı oluşturuldu: admin / admin");
        }
    }
}
