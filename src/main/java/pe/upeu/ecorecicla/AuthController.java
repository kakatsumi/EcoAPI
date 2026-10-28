package pe.upeu.ecorecicla;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UsuarioSeguridadService usuarios;
    private final JwtEncoder jwtEncoder;

    public AuthController(UsuarioSeguridadService usuarios, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.jwtEncoder = jwtEncoder;
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String contrasena) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            UsuarioAutenticado usuario = usuarios.autenticar(request.usuario(), request.contrasena());
            Instant ahora = Instant.now();
            Instant expiracion = ahora.plus(1, ChronoUnit.HOURS);
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("ecoapi")
                    .issuedAt(ahora)
                    .expiresAt(expiracion)
                    .subject(usuario.usuario())
                    .build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            return ResponseEntity.ok(Map.of("token", token, "tipo", "Bearer", "expiraEn", expiracion.toString()));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas"));
        }
    }
}
