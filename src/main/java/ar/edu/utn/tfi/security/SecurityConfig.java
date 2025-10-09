package ar.edu.utn.tfi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // páginas públicas y recursos estáticos
                        .requestMatchers(
                                "/", "/index.html",
                                "/login.html",
                                "/consulta.html",
                                "/historial.html",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/error", "/presupuesto.html",
                                "/estado-solicitud.html",
                                "/admin-solicitudes.html"
                        ).permitAll()

                        // Swagger abierto
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/v3/api-docs.yaml"
                        ).permitAll()

                        // API pública abierta
                        .requestMatchers("/public/**").permitAll()

                        // zona admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // cualquier otra ruta autenticada
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    InMemoryUserDetailsManager users(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}