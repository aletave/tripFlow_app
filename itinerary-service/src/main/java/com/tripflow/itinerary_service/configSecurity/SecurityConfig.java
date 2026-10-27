package com.tripflow.itinerary_service.configSecurity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Security del itinerary-service con Keycloak come Resource Server:
//la validazione dei JWT avviene via JWKS, i ruoli si leggono da realm_access.roles.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${tripflow.swagger.public:false}") boolean swaggerPublic
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Public itinerary access (no auth required)
                    auth.requestMatchers("/api/itineraries/public/**").permitAll();

                    // Swagger/OpenAPI pubblico solo se esplicitamente abilitato
                    // (es. profilo dev). In produzione resta protetto da JWT.
                    if (swaggerPublic) {
                        auth.requestMatchers(SWAGGER_PATHS).permitAll();
                    }

                    // Tutto il resto richiede un JWT valido
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    //CORS globale con origini esplicite configurabili.
    //Non viene usato il wildcard "*": l'API e' bearer-based, quindi di default
    //allowCredentials resta false. Se un client frontend richiede cookie/TLS
    //client certificate, abilitarlo esplicitamente via CORS_ALLOW_CREDENTIALS.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${tripflow.cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:8080}") String allowedOrigins,
            @Value("${tripflow.cors.allow-credentials:false}") boolean allowCredentials
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins(String allowedOrigins) {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    //I ruoli arrivano nel claim annidato realm_access.roles:
    //il converter di default non legge oggetti annidati, quindi serve questo custom.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<String> ruoli = (List<String>) realmAccess.get("roles");
            return ruoli.stream()
                    .map(ruolo -> new SimpleGrantedAuthority("ROLE_" + ruolo))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
