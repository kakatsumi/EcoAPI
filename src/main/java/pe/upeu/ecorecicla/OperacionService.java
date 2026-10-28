package pe.upeu.ecorecicla;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.OperacionDtos.*;
import pe.upeu.ecorecicla.persistence.*;

@Service
public class OperacionService {
    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private final UsuarioRepository usuarios;
    private final PuntoAcopioRepository puntos;
    private final RutaRepository rutas;
    private final RutaPuntoRepository rutaPuntos;
    private final PesajeRepository pesajes;

    public OperacionService(UsuarioRepository usuarios, PuntoAcopioRepository puntos,
                           RutaRepository rutas, RutaPuntoRepository rutaPuntos, PesajeRepository pesajes) {
        this.usuarios = usuarios;
        this.puntos = puntos;
        this.rutas = rutas;
        this.rutaPuntos = rutaPuntos;
        this.pesajes = pesajes;
    }

    @Transactional(readOnly = true)
    public PaginaPuntos puntos(String zona, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación inválida",
                    Map.of("campos", Map.of("pagina", "Debe ser mayor o igual a 0",
                            "tamano", "Debe estar entre 1 y 100")));
        }
        var pageable = PageRequest.of(pagina, tamano, Sort.by("nombre").ascending());
        Page<PuntoAcopio> resultado = zona == null || zona.isBlank()
                ? puntos.findByActivoTrue(pageable)
                : puntos.findByActivoTrueAndZonaIgnoreCase(zona.trim(), pageable);
        return new PaginaPuntos(resultado.getContent().stream().map(this::puntoDto).toList(),
                resultado.getTotalElements(), resultado.getTotalPages(), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public RutaDetalle rutaHoy(String username) {
        Usuario operario = usuario(username);
        Ruta ruta = rutas.findByFechaAndOperario(LocalDate.now(ZONA), operario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No hay ruta asignada para hoy"));
        return rutaDto(ruta);
    }

    @Transactional
    public PesajeDetalle registrar(PesajeEntrada entrada, String username) {
        Ruta ruta = ruta(entrada.rutaId());
        asegurarAbierta(ruta);
        PuntoAcopio punto = punto(entrada.puntoId());
        asegurarPlanificado(ruta, punto);
        validarPesos(entrada.pesoBruto(), entrada.tara());
        Usuario operario = usuario(username);
        Pesaje pesaje = new Pesaje(ruta, punto, operario, entrada.material().trim(),
                entrada.pesoBruto(), entrada.tara(), LocalDateTime.now(ZONA), null);
        pesaje.corregir(entrada.material().trim(), entrada.pesoBruto(), entrada.tara(), entrada.observaciones());
        return pesajeDto(pesajes.save(pesaje));
    }

    @Transactional
    public PesajeDetalle corregir(Long id, PesajeCorreccion entrada, UsuarioAutenticado actor) {
        Pesaje pesaje = pesaje(id);
        asegurarAbierta(pesaje.getRuta());
        if (pesaje.getEstado() == EstadoPesaje.ANULADO) {
            throw new ApiException(HttpStatus.CONFLICT, "El pesaje está anulado");
        }
        boolean supervisor = actor.roles().contains(NombreRol.SUPERVISOR.name());
        boolean propio = pesaje.getOperario().getUsername().equals(actor.usuario());
        boolean mismoDia = pesaje.getFechaHora().toLocalDate().equals(LocalDate.now(ZONA));
        if (!supervisor && (!propio || !mismoDia)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El operario solo puede corregir pesajes propios del mismo día");
        }
        validarPesos(entrada.pesoBruto(), entrada.tara());
        pesaje.corregir(entrada.material().trim(), entrada.pesoBruto(), entrada.tara(), entrada.observaciones());
        return pesajeDto(pesajes.save(pesaje));
    }

    @Transactional
    public PesajeDetalle anular(Long id) {
        Pesaje pesaje = pesaje(id);
        if (pesaje.getEstado() == EstadoPesaje.ANULADO) {
            throw new ApiException(HttpStatus.CONFLICT, "El pesaje ya está anulado");
        }
        pesaje.anular();
        return pesajeDto(pesajes.save(pesaje));
    }

    @Transactional
    public RutaPuntoDetalle omitir(Long rutaId, Long puntoId, String motivo) {
        Ruta ruta = ruta(rutaId);
        asegurarAbierta(ruta);
        PuntoAcopio punto = punto(puntoId);
        RutaPunto plan = rutaPuntos.findByRutaAndPunto(ruta, punto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El punto no está planificado en la ruta"));
        plan.setMotivoOmision(motivo.trim());
        return rutaPuntoDto(rutaPuntos.save(plan));
    }

    @Transactional
    public RutaDetalle cerrar(Long id) {
        Ruta ruta = ruta(id);
        asegurarAbierta(ruta);
        List<RutaPunto> plan = rutaPuntos.findByRutaOrderByOrdenVisitaAsc(ruta);
        long pendientes = plan.stream().filter(item ->
                (item.getMotivoOmision() == null || item.getMotivoOmision().isBlank())
                        && !pesajes.existsByRutaAndPuntoAndEstado(ruta, item.getPunto(), EstadoPesaje.ACTIVO)).count();
        if (pendientes > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "La ruta tiene puntos pendientes",
                    Map.of("puntosPendientes", pendientes));
        }
        ruta.setEstado(EstadoRuta.CERRADA);
        rutas.save(ruta);
        return rutaDto(ruta);
    }

    private void validarPesos(BigDecimal bruto, BigDecimal tara) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (bruto == null || bruto.signum() <= 0) errores.put("pesoBruto", "Debe ser mayor que cero");
        if (tara == null || tara.signum() < 0) errores.put("tara", "Debe ser mayor o igual a cero");
        BigDecimal neto = bruto == null || tara == null ? null : bruto.subtract(tara);
        if (neto == null || neto.signum() <= 0) errores.put("pesoNeto", "Debe ser mayor que cero");
        if (!errores.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pesos inválidos", Map.of("campos", errores));
        }
    }

    private Usuario usuario(String username) {
        return usuarios.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
    private Ruta ruta(Long id) {
        return rutas.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    }
    private PuntoAcopio punto(Long id) {
        return puntos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Punto no encontrado"));
    }
    private Pesaje pesaje(Long id) {
        return pesajes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pesaje no encontrado"));
    }
    private void asegurarAbierta(Ruta ruta) {
        if (ruta.getEstado() == EstadoRuta.CERRADA) {
            throw new ApiException(HttpStatus.CONFLICT, "La ruta está cerrada");
        }
    }
    private void asegurarPlanificado(Ruta ruta, PuntoAcopio punto) {
        if (!rutaPuntos.existsByRutaAndPunto(ruta, punto)) {
            throw new ApiException(HttpStatus.CONFLICT, "El punto no está planificado en la ruta");
        }
    }
    private Punto puntoDto(PuntoAcopio punto) {
        return new Punto(punto.getId(), punto.getNombre(), punto.getDireccion(), punto.getZona());
    }
    private RutaPuntoDetalle rutaPuntoDto(RutaPunto item) {
        PuntoAcopio punto = item.getPunto();
        return new RutaPuntoDetalle(punto.getId(), punto.getNombre(), punto.getDireccion(), punto.getZona(),
                item.getOrdenVisita(), item.getMotivoOmision());
    }
    private RutaDetalle rutaDto(Ruta ruta) {
        return new RutaDetalle(ruta.getId(), ruta.getFecha(), ruta.getEstado(), ruta.getOperario().getUsername(),
                rutaPuntos.findByRutaOrderByOrdenVisitaAsc(ruta).stream().map(this::rutaPuntoDto).toList());
    }
    private PesajeDetalle pesajeDto(Pesaje pesaje) {
        return new PesajeDetalle(pesaje.getId(), pesaje.getRuta().getId(), pesaje.getPunto().getId(),
                pesaje.getOperario().getUsername(), pesaje.getMaterial(), pesaje.getPesoBruto(), pesaje.getTara(),
                pesaje.getPesoNeto(), pesaje.getObservaciones(), pesaje.getFechaHora(), pesaje.getEstado());
    }
}
