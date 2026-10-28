package pe.upeu.ecorecicla;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.persistence.Privilegio;
import pe.upeu.ecorecicla.persistence.Rol;
import pe.upeu.ecorecicla.persistence.Usuario;
import pe.upeu.ecorecicla.persistence.UsuarioRepository;

@Service
public class UsuarioSeguridadService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSeguridadService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticado autenticar(String username, String password) {
        Usuario usuario = usuarios.findByUsername(username)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return detalle(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticado buscarActivo(String username) {
        return usuarios.findByUsername(username)
                .filter(Usuario::isActivo)
                .map(this::detalle)
                .orElseThrow(() -> new BadCredentialsException("Usuario no disponible"));
    }

    private UsuarioAutenticado detalle(Usuario usuario) {
        Set<String> roles = new TreeSet<>();
        Set<String> privilegios = new TreeSet<>();
        for (Rol rol : usuario.getRoles()) {
            roles.add(rol.getNombre().name());
            for (Privilegio privilegio : rol.getPrivilegios()) {
                privilegios.add(privilegio.getCodigo());
            }
        }
        return new UsuarioAutenticado(usuario.getId(), usuario.getUsername(), roles, privilegios);
    }
}
