package pe.upeu.ecorecicla;

import java.util.Set;

public record UsuarioAutenticado(Long id, String usuario, Set<String> roles, Set<String> privilegios) {}
