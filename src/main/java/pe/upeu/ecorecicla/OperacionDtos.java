package pe.upeu.ecorecicla;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.upeu.ecorecicla.persistence.EstadoPesaje;
import pe.upeu.ecorecicla.persistence.EstadoRuta;

public final class OperacionDtos {
    private OperacionDtos() {}

    public record Punto(Long id, String nombre, String direccion, String zona) {}
    public record PaginaPuntos(List<Punto> contenido, long totalElementos, int totalPaginas,
                               int pagina, int tamano) {}
    public record RutaPuntoDetalle(Long puntoId, String nombre, String direccion, String zona,
                                   int ordenVisita, String motivoOmision) {}
    public record RutaDetalle(Long id, LocalDate fecha, EstadoRuta estado, String operario,
                              List<RutaPuntoDetalle> puntos) {}
    public record PesajeEntrada(@NotNull Long rutaId, @NotNull Long puntoId,
                                @NotBlank @Size(max = 80) String material,
                                @NotNull @Digits(integer = 7, fraction = 3) BigDecimal pesoBruto,
                                @NotNull @Digits(integer = 7, fraction = 3) BigDecimal tara,
                                @Size(max = 500) String observaciones) {}
    public record PesajeCorreccion(@NotBlank @Size(max = 80) String material,
                                   @NotNull @Digits(integer = 7, fraction = 3) BigDecimal pesoBruto,
                                   @NotNull @Digits(integer = 7, fraction = 3) BigDecimal tara,
                                   @Size(max = 500) String observaciones) {}
    public record PesajeDetalle(Long id, Long rutaId, Long puntoId, String operario, String material,
                                BigDecimal pesoBruto, BigDecimal tara, BigDecimal pesoNeto,
                                String observaciones, LocalDateTime fechaHora, EstadoPesaje estado) {}
    public record OmisionEntrada(@NotBlank @Size(max = 300) String motivo) {}
}
