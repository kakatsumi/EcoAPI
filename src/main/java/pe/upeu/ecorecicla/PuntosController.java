package pe.upeu.ecorecicla;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.OperacionDtos.PaginaPuntos;

@RestController
@RequestMapping("/api/v1/puntos")
public class PuntosController {
    private final OperacionService operaciones;
    public PuntosController(OperacionService operaciones) { this.operaciones = operaciones; }

    @GetMapping
    public PaginaPuntos listar(@RequestParam(required = false) String zona,
                               @RequestParam(defaultValue = "0") int pagina,
                               @RequestParam(defaultValue = "20") int tamano) {
        return operaciones.puntos(zona, pagina, tamano);
    }
}
