package pe.upeu.ecorecicla;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.OperacionDtos.OmisionEntrada;
import pe.upeu.ecorecicla.OperacionDtos.RutaDetalle;
import pe.upeu.ecorecicla.OperacionDtos.RutaPuntoDetalle;

@RestController
@RequestMapping("/api/v1/rutas")
public class RutasController {
    private final OperacionService operaciones;
    public RutasController(OperacionService operaciones) { this.operaciones = operaciones; }

    @GetMapping("/hoy")
    public RutaDetalle hoy(Authentication authentication) {
        return operaciones.rutaHoy(authentication.getName());
    }

    @PutMapping("/{rutaId}/puntos/{puntoId}/omision")
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    public RutaPuntoDetalle omitir(@PathVariable Long rutaId, @PathVariable Long puntoId,
                                   @Valid @RequestBody OmisionEntrada entrada) {
        return operaciones.omitir(rutaId, puntoId, entrada.motivo());
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('RUTA_CERRAR')")
    public RutaDetalle cerrar(@PathVariable Long id) {
        return operaciones.cerrar(id);
    }
}
