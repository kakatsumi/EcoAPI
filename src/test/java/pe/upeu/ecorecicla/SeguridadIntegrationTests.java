package pe.upeu.ecorecicla;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.persistence.NombreRol;
import pe.upeu.ecorecicla.persistence.Privilegio;
import pe.upeu.ecorecicla.persistence.Rol;
import pe.upeu.ecorecicla.persistence.Usuario;
import pe.upeu.ecorecicla.persistence.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "DB_PASSWORD=test-only",
        "JWT_SECRET=clave-de-prueba-con-mas-de-32-caracteres-unicamente",
        "ecoapi.seed.enabled=false"
})
@AutoConfigureMockMvc
@Import(SeguridadIntegrationTests.RequiereAnularController.class)
class SeguridadIntegrationTests {
    private static final String HASH = "$2b$12$yicBWAudAiQjy91on8sznehXAnbbszgC02c5dQEVAIg/94CoGaXXK";

    @Autowired MockMvc mvc;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @MockitoBean UsuarioRepository usuarios;

    @BeforeEach
    void prepararUsuario() {
        Usuario operario = new Usuario("operario1", HASH);
        Rol rol = new Rol(NombreRol.OPERARIO);
        rol.getPrivilegios().add(new Privilegio("PUNTO_LEER"));
        rol.getPrivilegios().add(new Privilegio("PESAJE_CREAR"));
        operario.getRoles().add(rol);
        when(usuarios.findByUsername("operario1")).thenReturn(Optional.of(operario));
    }

    @Test
    void loginCorrectoEntregaJwtValidoPorUnaHora() throws Exception {
        String token = login();
        var jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("operario1");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(jwt.getClaims()).doesNotContainKeys("roles", "privilegios");
    }

    @Test
    void contrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"operario1\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        String token = login();
        String firmaAlterada = token.substring(0, token.lastIndexOf('.') + 1) + "firma-falsa";
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + firmaAlterada))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        Instant antes = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ecoapi").subject("operario1")
                .issuedAt(antes).expiresAt(antes.plus(Duration.ofHours(1))).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saludSigueSiendoPublico() throws Exception {
        mvc.perform(get("/api/v1/salud")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    void usuarioAutenticadoVePermisosDeBaseDeDatos() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("operario1"))
                .andExpect(jsonPath("$.privilegios[?(@ == 'PESAJE_CREAR')]").exists());
    }

    @Test
    void privilegioAusenteDevuelve403YPresenteAutoriza() throws Exception {
        String token = login();
        mvc.perform(get("/api/v1/test/requiere-anular").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pesajes/1/anular").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        Usuario supervisor = new Usuario("operario1", HASH);
        Rol rol = new Rol(NombreRol.SUPERVISOR);
        rol.getPrivilegios().add(new Privilegio("PESAJE_ANULAR"));
        supervisor.getRoles().add(rol);
        when(usuarios.findByUsername("operario1")).thenReturn(Optional.of(supervisor));
        mvc.perform(get("/api/v1/test/requiere-anular").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String login() throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"operario1\",\"contrasena\":\"Eco2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.split("\"token\":\"")[1].split("\"")[0];
    }

    @RestController
    static class RequiereAnularController {
        @GetMapping("/api/v1/test/requiere-anular")
        @PreAuthorize("hasAuthority('PESAJE_ANULAR')")
        String prueba() { return "OK"; }
    }
}
