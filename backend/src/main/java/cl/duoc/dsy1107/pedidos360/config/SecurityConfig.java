package cl.duoc.dsy1107.pedidos360.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new CognitoGrantedAuthoritiesConverter());
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
        JwtDecoder jwtDecoder(@Value("${app.security.cognito.issuer}") String issuer,
            @Value("${app.security.cognito.client-id}") String clientId) {
        String jwkSetUri = issuer.endsWith("/")
            ? issuer + ".well-known/jwks.json"
            : issuer + "/.well-known/jwks.json";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), new CognitoAudienceValidator(clientId)));
        return decoder;
    }

    private static final class CognitoGrantedAuthoritiesConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.addAll(scopes.convert(jwt));
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups != null) {
                groups.stream().map(group -> new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()))
                        .forEach(authorities::add);
            }
            return authorities;
        }
    }
}
