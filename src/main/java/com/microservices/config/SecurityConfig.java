package com.microservices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig{  	
	
	@Bean
	public UserDetailsService users() {
	    UserDetails user1 = User.builder()
	        .username("org")
	        .password(passwordEncoder().encode("org"))
	        .roles("ORGANISATEUR")
	        .build();
	    UserDetails user2 = User.builder()
	        .username("pra")
	        .password(passwordEncoder().encode("pra"))
	        .roles("PRATICIEN")
	        .build();
	    return new InMemoryUserDetailsManager(user1, user2);
	}

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeRequests(requests -> requests
                .requestMatchers("/afficher-patients").hasAnyRole("ORGANISATEUR", "PRATICIEN")
                .requestMatchers("/afficher-details/**").hasAnyRole("ORGANISATEUR", "PRATICIEN")
                .requestMatchers("/modifier-adresse/**", "/modifier-numero/**", "/ajouter-patient").hasRole("ORGANISATEUR")
                .requestMatchers("/ajouter-note/**").hasRole("PRATICIEN"));
                
        http
            .formLogin()
            .loginPage("/login") 
            .loginProcessingUrl("/process-login") 
            .defaultSuccessUrl("/afficher-patients") 
            .failureUrl("/login?error=true")
            .permitAll() ;
                
        return http.build();
    }
}
