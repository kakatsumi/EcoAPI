package pe.upeu.ecorecicla;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtKey(@Value("${JWT_SECRET}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes UTF-8");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("ecoapi"));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioSeguridadService usuarios) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/salud").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/pesajes/*/anular").hasAuthority("PESAJE_ANULAR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/rutas/*/cerrar").hasAuthority("RUTA_CERRAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/puntos", "/api/v1/puntos/**").hasAuthority("PUNTO_LEER")
                        .requestMatchers("/api/v1/puntos", "/api/v1/puntos/**").hasAuthority("PUNTO_GESTIONAR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pesajes", "/api/v1/pesajes/**").hasAuthority("PESAJE_LEER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pesajes", "/api/v1/pesajes/**").hasAuthority("PESAJE_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pesajes/*").hasAuthority("PESAJE_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/pesajes/*").hasAuthority("PESAJE_EDITAR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                    UsuarioAutenticado usuario = usuarios.buscarActivo(token.getSubject());
                    var authorities = usuario.privilegios().stream().map(SimpleGrantedAuthority::new).toList();
                    return new JwtAuthenticationToken(token, authorities, usuario.usuario());
                })))
                .build();
    }
}
