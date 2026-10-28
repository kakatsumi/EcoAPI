package pe.upeu.ecorecicla;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final UsuarioSeguridadService usuarios;

    public UsuarioController(UsuarioSeguridadService usuarios) { this.usuarios = usuarios; }

    @GetMapping("/me")
    public UsuarioAutenticado me(Authentication authentication) {
        return usuarios.buscarActivo(authentication.getName());
    }
}
