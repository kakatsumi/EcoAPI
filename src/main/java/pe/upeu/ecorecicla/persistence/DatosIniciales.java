package pe.upeu.ecorecicla.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "ecoapi.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {
    // BCrypt de la contraseña de demostración indicada en el enunciado.
    private static final String DEMO_HASH = "$2b$12$yicBWAudAiQjy91on8sznehXAnbbszgC02c5dQEVAIg/94CoGaXXK";
    private static final String[] CODIGOS = {
        "PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR",
        "PESAJE_ANULAR", "RUTA_CERRAR", "PUNTO_GESTIONAR"
    };
    private static final String[][] PUNTOS = {
        {"Punto Norte 1", "Av. Norte 101", "NORTE"},
        {"Punto Norte 2", "Av. Norte 202", "NORTE"},
        {"Punto Norte 3", "Jr. Los Pinos 303", "NORTE"},
        {"Punto Norte 4", "Calle Sol 404", "NORTE"},
        {"Punto Centro 1", "Av. Central 105", "CENTRO"},
        {"Punto Centro 2", "Jr. Unión 206", "CENTRO"},
        {"Punto Centro 3", "Calle Comercio 307", "CENTRO"},
        {"Punto Centro 4", "Av. Mercado 408", "CENTRO"},
        {"Punto Sur 1", "Av. Sur 109", "SUR"},
        {"Punto Sur 2", "Jr. Flores 210", "SUR"},
        {"Punto Sur 3", "Calle Lima 311", "SUR"},
        {"Punto Sur 4", "Av. Parque 412", "SUR"}
    };

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PrivilegioRepository privilegios;
    private final PuntoAcopioRepository puntos;
    private final RutaRepository rutas;
    private final RutaPuntoRepository rutaPuntos;
    private final PesajeRepository pesajes;

    public DatosIniciales(UsuarioRepository usuarios, RolRepository roles, PrivilegioRepository privilegios,
                          PuntoAcopioRepository puntos, RutaRepository rutas,
                          RutaPuntoRepository rutaPuntos, PesajeRepository pesajes) {
        this.usuarios = usuarios; this.roles = roles; this.privilegios = privilegios;
        this.puntos = puntos; this.rutas = rutas; this.rutaPuntos = rutaPuntos; this.pesajes = pesajes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Privilegio> permisos = new HashMap<>();
        for (String codigo : CODIGOS) {
            permisos.put(codigo, privilegios.findByCodigo(codigo)
                    .orElseGet(() -> privilegios.save(new Privilegio(codigo))));
        }

        Map<NombreRol, Rol> rolesPorNombre = new EnumMap<>(NombreRol.class);
        for (NombreRol nombre : NombreRol.values()) {
            Rol rol = roles.findByNombre(nombre).orElseGet(() -> roles.save(new Rol(nombre)));
            rol.privilegios.add(permisos.get("PUNTO_LEER"));
            rol.privilegios.add(permisos.get("PESAJE_LEER"));
            if (nombre == NombreRol.OPERARIO || nombre == NombreRol.SUPERVISOR) {
                rol.privilegios.add(permisos.get("PESAJE_CREAR"));
                rol.privilegios.add(permisos.get("PESAJE_EDITAR"));
            }
            if (nombre == NombreRol.SUPERVISOR) {
                rol.privilegios.add(permisos.get("PESAJE_ANULAR"));
                rol.privilegios.add(permisos.get("RUTA_CERRAR"));
            }
            if (nombre == NombreRol.ADMINISTRADOR) {
                rol.privilegios.add(permisos.get("PUNTO_GESTIONAR"));
            }
            rolesPorNombre.put(nombre, rol);
        }

        Usuario operario = usuario("operario1", rolesPorNombre.get(NombreRol.OPERARIO));
        usuario("supervisor1", rolesPorNombre.get(NombreRol.SUPERVISOR));
        usuario("admin", rolesPorNombre.get(NombreRol.ADMINISTRADOR));

        PuntoAcopio[] puntosPorOrden = new PuntoAcopio[PUNTOS.length];
        for (int i = 0; i < PUNTOS.length; i++) {
            String[] dato = PUNTOS[i];
            puntosPorOrden[i] = puntos.findByNombre(dato[0])
                    .orElseGet(() -> puntos.save(new PuntoAcopio(dato[0], dato[1], dato[2])));
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        Ruta abierta = rutas.findByFechaAndOperario(hoy, operario)
                .orElseGet(() -> rutas.save(new Ruta(hoy, EstadoRuta.ABIERTA, operario)));
        LocalDate ayer = hoy.minusDays(1);
        Ruta cerrada = rutas.findByFechaAndOperario(ayer, operario)
                .orElseGet(() -> rutas.save(new Ruta(ayer, EstadoRuta.CERRADA, operario)));
        for (int i = 0; i < 6; i++) {
            agregarPunto(abierta, puntosPorOrden[i], i + 1);
            agregarPunto(cerrada, puntosPorOrden[i + 6], i + 1);
        }

        String[] materiales = {"PAPEL", "PLASTICO", "VIDRIO", "CARTON", "METAL"};
        for (int i = 0; i < 6; i++) {
            agregarPesaje(abierta, puntosPorOrden[i], operario, materiales[i % materiales.length],
                    hoy.atTime(9 + i, 0), "HOY-" + hoy + "-" + (i + 1), i);
        }
        for (int i = 0; i < 9; i++) {
            agregarPesaje(cerrada, puntosPorOrden[6 + i % 6], operario, materiales[(i + 2) % materiales.length],
                    ayer.atTime(8 + i, 0), "AYER-" + ayer + "-" + (i + 1), i + 6);
        }
    }

    private Usuario usuario(String username, Rol rol) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseGet(() -> usuarios.save(new Usuario(username, DEMO_HASH)));
        usuario.roles.add(rol);
        return usuario;
    }

    private void agregarPunto(Ruta ruta, PuntoAcopio punto, int orden) {
        if (!rutaPuntos.existsByRutaAndPunto(ruta, punto)) {
            rutaPuntos.save(new RutaPunto(ruta, punto, orden));
        }
    }

    private void agregarPesaje(Ruta ruta, PuntoAcopio punto, Usuario operario, String material,
                               LocalDateTime fechaHora, String codigo, int indice) {
        if (!pesajes.existsByCodigoMuestra(codigo)) {
            BigDecimal bruto = BigDecimal.valueOf(12 + indice * 2L).setScale(3);
            BigDecimal tara = BigDecimal.valueOf(1.5).setScale(3);
            pesajes.save(new Pesaje(ruta, punto, operario, material, bruto, tara, fechaHora, codigo));
        }
    }
}
