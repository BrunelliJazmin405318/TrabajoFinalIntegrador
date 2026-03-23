package ar.edu.utn.tfi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PublicJwtFilter publicJwtFilter(JwtService jwtService) {
        return new PublicJwtFilter(jwtService);
    }

    // 1) ADMIN: con Basic
    @Bean
    @Order(1)
    SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    // 2) PUBLIC + STATIC: sin Basic (para que NO salga el popup)
    @Bean
    @Order(2)
    SecurityFilterChain appChain(HttpSecurity http, PublicJwtFilter publicJwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/index.html",
                                "/login.html",
                                "/consulta.html",
                                "/home.html",
                                "/historial.html",
                                "/presupuesto.html",
                                "/estado-solicitud.html",
                                "/admin-solicitudes.html",
                                "/admin-presupuestos.html",
                                "/faq.html",
                                "/terminos.html",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/favicon.ico",
                                "/css/**", "/js/**", "/img/**"
                        ).permitAll()

                        .requestMatchers("/public/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/public/presupuestos/solicitud").permitAll()
                        .requestMatchers("/pagos/webhook-mp/**").permitAll()

                        .requestMatchers("/public/**").hasAnyRole("PUBLIC", "ADMIN")
                        .anyRequest().permitAll()
                );

        http.addFilterBefore(publicJwtFilter, UsernamePasswordAuthenticationFilter.class);

        // 👇 clave: NO basic acá
        // http.httpBasic(...)  <-- NO

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