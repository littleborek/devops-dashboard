package com.berk.devopsdashboard.service.impl;

import com.berk.devopsdashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Special case for mTLS Agent
        if ("ai-ops-agent".equals(username)) {
            return org.springframework.security.core.userdetails.User.withUsername("ai-ops-agent")
                    .password("") // No password for cert-based auth
                    .roles("AGENT")
                    .build();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));
    }
}
