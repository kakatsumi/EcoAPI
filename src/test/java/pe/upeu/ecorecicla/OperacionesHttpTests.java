package pe.upeu.ecorecicla;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.upeu.ecorecicla.persistence.NombreRol;
import pe.upeu.ecorecicla.persistence.Privilegio;
import pe.upeu.ecorecicla.persistence.Rol;
import pe.upeu.ecorecicla.persistence.Usuario;
import pe.upeu.ecorecicla.persistence.UsuarioRepository;

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
class OperacionesHttpTests {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean OperacionService operaciones;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/rutas/hoy")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorNoPuedeRegistrarPesajes() throws Exception {
        mvc.perform(post("/api/v1/pesajes").header("Authorization", token(NombreRol.ADMINISTRADOR, "PUNTO_GESTIONAR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void solicitudInvalidaDevuelve400ConCampo() throws Exception {
        mvc.perform(post("/api/v1/pesajes").header("Authorization", token(NombreRol.OPERARIO, "PESAJE_CREAR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":1,\"puntoId\":2,\"material\":\"PAPEL\",\"tara\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pesoBruto").exists());
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        when(operaciones.rutaHoy("operario1"))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "No hay ruta asignada para hoy"));
        mvc.perform(get("/api/v1/rutas/hoy").header("Authorization", token(NombreRol.OPERARIO, "PUNTO_LEER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cierreConPendientesDevuelve409YCantidad() throws Exception {
        when(operaciones.cerrar(1L)).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.CONFLICT, "La ruta tiene puntos pendientes",
                Map.of("puntosPendientes", 2L)));
        mvc.perform(post("/api/v1/rutas/1/cerrar")
                        .header("Authorization", token(NombreRol.SUPERVISOR, "RUTA_CERRAR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.puntosPendientes").value(2));
    }

    private String token(NombreRol nombre, String privilegio) {
        Usuario usuario = new Usuario("operario1", "hash-de-prueba");
        Rol rol = new Rol(nombre);
        rol.getPrivilegios().add(new Privilegio(privilegio));
        usuario.getRoles().add(rol);
        when(usuarios.findByUsername("operario1")).thenReturn(Optional.of(usuario));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ecoapi").subject("operario1")
                .issuedAt(ahora).expiresAt(ahora.plusSeconds(3600)).build();
        String jwt = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return "Bearer " + jwt;
    }
}
