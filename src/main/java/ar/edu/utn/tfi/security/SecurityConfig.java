package ar.edu.utn.tfi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/").permitAll()

                        // Páginas públicas y recursos
                        .requestMatchers(
                                "/", "/index.html",
                                "/login.html",
                                "/consulta.html",
                                "/historial.html",
                                "/css/", "/js/", "/images/", "/favicon.ico",
                                "/error", "/presupuesto.html",
                                "/estado-solicitud.html",
                                "/admin-solicitudes.html",
                                "/admin-presupuestos.html"
                        ).permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/",
                                "/v3/api-docs/", "/v3/api-docs.yaml"
                        ).permitAll()

                        // ✅ Endpoints públicos reales (Checkout API cliente)
                        .requestMatchers("/public/").permitAll() // CAMBIO

                        // ✅ Webhook MP
                        .requestMatchers("/pagos/webhook-mp").permitAll()

                        // Zona admin
                        .requestMatchers("/admin/").hasRole("ADMIN") // CAMBIO

                        // Resto autenticado
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