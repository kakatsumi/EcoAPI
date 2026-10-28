package pe.upeu.ecorecicla;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pe.upeu.ecorecicla.OperacionDtos.PesajeCorreccion;
import pe.upeu.ecorecicla.OperacionDtos.PesajeEntrada;
import pe.upeu.ecorecicla.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacionServiceTests {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    @Mock UsuarioRepository usuarios;
    @Mock PuntoAcopioRepository puntos;
    @Mock RutaRepository rutas;
    @Mock RutaPuntoRepository rutaPuntos;
    @Mock PesajeRepository pesajes;
    @InjectMocks OperacionService servicio;

    private final Usuario operario = new Usuario("operario1", "hash");
    private final Usuario otro = new Usuario("otro", "hash");
    private final PuntoAcopio punto = new PuntoAcopio("Punto 1", "Av. 1", "NORTE");

    @Test
    void rn01NoRegistraNiCorrigeEnRutaCerrada() {
        Ruta cerrada = new Ruta(LocalDate.now(LIMA), EstadoRuta.CERRADA, operario);
        when(rutas.findById(1L)).thenReturn(Optional.of(cerrada));
        assertThatThrownBy(() -> servicio.registrar(entrada(), "operario1"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        Pesaje existente = pesaje(cerrada, operario, LocalDateTime.now(LIMA));
        when(pesajes.findById(2L)).thenReturn(Optional.of(existente));
        assertThatThrownBy(() -> servicio.corregir(2L, correccion(), actor("OPERARIO")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(pesajes, never()).save(any());
    }

    @Test
    void rn02OperarioNoCorrigePesajeAjenoNiDeOtroDia() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        when(pesajes.findById(2L)).thenReturn(Optional.of(pesaje(abierta, otro, LocalDateTime.now(LIMA))));
        assertThatThrownBy(() -> servicio.corregir(2L, correccion(), actor("OPERARIO")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        when(pesajes.findById(2L)).thenReturn(Optional.of(pesaje(abierta, operario, LocalDateTime.now(LIMA).minusDays(1))));
        assertThatThrownBy(() -> servicio.corregir(2L, correccion(), actor("OPERARIO")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(pesajes, never()).save(any());
    }

    @Test
    void rn02SupervisorCorrigePesajeAjenoDeOtroDia() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        Pesaje existente = pesaje(abierta, otro, LocalDateTime.now(LIMA).minusDays(1));
        when(pesajes.findById(2L)).thenReturn(Optional.of(existente));
        when(pesajes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var resultado = servicio.corregir(2L, correccion(), actor("SUPERVISOR"));
        assertThat(resultado.pesoNeto()).isEqualByComparingTo("8.000");
    }

    @Test
    void rn03CalculaNetoEnServidorYRechazaCeroONegativoConCampo() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        when(rutas.findById(1L)).thenReturn(Optional.of(abierta));
        when(puntos.findById(3L)).thenReturn(Optional.of(punto));
        when(rutaPuntos.existsByRutaAndPunto(abierta, punto)).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(new PesajeEntrada(1L, 3L, "PAPEL",
                new BigDecimal("5.000"), new BigDecimal("5.000"), null), "operario1"))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(((java.util.Map<?, ?>) ex.getDetalles().get("campos")).containsKey("pesoNeto")).isTrue();
                });
        verify(pesajes, never()).save(any());

        when(usuarios.findByUsername("operario1")).thenReturn(Optional.of(operario));
        when(pesajes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var resultado = servicio.registrar(entrada(), "operario1");
        assertThat(resultado.pesoNeto()).isEqualByComparingTo("8.000");
    }

    @Test
    void rn03TambienValidaCorreccionYRechazaTaraNegativa() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        when(pesajes.findById(2L)).thenReturn(Optional.of(pesaje(abierta, operario, LocalDateTime.now(LIMA))));
        assertThatThrownBy(() -> servicio.corregir(2L, new PesajeCorreccion("PAPEL",
                new BigDecimal("10.000"), new BigDecimal("-1.000"), null), actor("OPERARIO")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(((java.util.Map<?, ?>) ex.getDetalles().get("campos")).containsKey("tara")).isTrue();
                });
        verify(pesajes, never()).save(any());
    }

    @Test
    void rn04NoCierraMientrasHayaPuntosPendientesEIndicaCantidad() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        PuntoAcopio segundo = new PuntoAcopio("Punto 2", "Av. 2", "NORTE");
        RutaPunto primero = new RutaPunto(abierta, punto, 1);
        RutaPunto pendiente = new RutaPunto(abierta, segundo, 2);
        when(rutas.findById(1L)).thenReturn(Optional.of(abierta));
        when(rutaPuntos.findByRutaOrderByOrdenVisitaAsc(abierta)).thenReturn(List.of(primero, pendiente));
        when(pesajes.existsByRutaAndPuntoAndEstado(abierta, punto, EstadoPesaje.ACTIVO)).thenReturn(true);
        assertThatThrownBy(() -> servicio.cerrar(1L))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getDetalles()).containsEntry("puntosPendientes", 1L);
                });
        verify(rutas, never()).save(any());

        pendiente.setMotivoOmision("Punto inaccesible");
        var resultado = servicio.cerrar(1L);
        assertThat(resultado.estado()).isEqualTo(EstadoRuta.CERRADA);
    }

    @Test
    void guardaMotivoDeOmisionEnPuntoPlanificado() {
        Ruta abierta = new Ruta(LocalDate.now(LIMA), EstadoRuta.ABIERTA, operario);
        RutaPunto plan = new RutaPunto(abierta, punto, 1);
        when(rutas.findById(1L)).thenReturn(Optional.of(abierta));
        when(puntos.findById(3L)).thenReturn(Optional.of(punto));
        when(rutaPuntos.findByRutaAndPunto(abierta, punto)).thenReturn(Optional.of(plan));
        when(rutaPuntos.save(plan)).thenReturn(plan);
        var resultado = servicio.omitir(1L, 3L, "  Punto inaccesible  ");
        assertThat(resultado.motivoOmision()).isEqualTo("Punto inaccesible");
    }

    @Test
    void anulacionEsLogica() {
        Ruta ruta = new Ruta(LocalDate.now(LIMA), EstadoRuta.CERRADA, operario);
        Pesaje existente = pesaje(ruta, operario, LocalDateTime.now(LIMA));
        when(pesajes.findById(2L)).thenReturn(Optional.of(existente));
        when(pesajes.save(existente)).thenReturn(existente);
        var resultado = servicio.anular(2L);
        assertThat(resultado.estado()).isEqualTo(EstadoPesaje.ANULADO);
        verify(pesajes).save(existente);
    }

    @Test
    void filtraPuntosActivosPorZonaConPaginacion() {
        when(puntos.findByActivoTrueAndZonaIgnoreCase(any(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(punto), PageRequest.of(0, 2), 1));
        var resultado = servicio.puntos(" norte ", 0, 2);
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.contenido()).extracting(OperacionDtos.Punto::nombre).containsExactly("Punto 1");
        verify(puntos).findByActivoTrueAndZonaIgnoreCase(org.mockito.ArgumentMatchers.eq("norte"), any());
    }

    private PesajeEntrada entrada() {
        return new PesajeEntrada(1L, 3L, "PAPEL", new BigDecimal("10.000"), new BigDecimal("2.000"), null);
    }
    private PesajeCorreccion correccion() {
        return new PesajeCorreccion("PAPEL", new BigDecimal("10.000"), new BigDecimal("2.000"), null);
    }
    private Pesaje pesaje(Ruta ruta, Usuario autor, LocalDateTime fecha) {
        return new Pesaje(ruta, punto, autor, "PAPEL", new BigDecimal("10.000"), new BigDecimal("2.000"), fecha, null);
    }
    private UsuarioAutenticado actor(String rol) {
        return new UsuarioAutenticado(null, "operario1", Set.of(rol), Set.of("PESAJE_EDITAR"));
    }
}
