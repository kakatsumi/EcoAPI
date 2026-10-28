package pe.upeu.ecorecicla;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.OperacionDtos.PesajeCorreccion;
import pe.upeu.ecorecicla.OperacionDtos.PesajeDetalle;
import pe.upeu.ecorecicla.OperacionDtos.PesajeEntrada;

@RestController
@RequestMapping("/api/v1/pesajes")
public class PesajesController {
    private final OperacionService operaciones;
    private final UsuarioSeguridadService usuarios;
    public PesajesController(OperacionService operaciones, UsuarioSeguridadService usuarios) {
        this.operaciones = operaciones; this.usuarios = usuarios;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    public ResponseEntity<PesajeDetalle> registrar(@Valid @RequestBody PesajeEntrada entrada,
                                                    Authentication authentication) {
        PesajeDetalle creado = operaciones.registrar(entrada, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/pesajes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PESAJE_EDITAR')")
    public PesajeDetalle corregir(@PathVariable Long id, @Valid @RequestBody PesajeCorreccion entrada,
                                   Authentication authentication) {
        return operaciones.corregir(id, entrada, usuarios.buscarActivo(authentication.getName()));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('PESAJE_ANULAR')")
    public PesajeDetalle anular(@PathVariable Long id) {
        return operaciones.anular(id);
    }
}
