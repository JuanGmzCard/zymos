package com.alera.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoteFormDto {

    @NotBlank(message = "El estilo es obligatorio")
    @Size(max = 100, message = "El estilo no puede superar 100 caracteres")
    private String estilo;

    private Long equipoFermentadorId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaElaboracion;

    @DecimalMin(value = "0.0", message = "El agua utilizada no puede ser negativa")
    @DecimalMax(value = "99999.999", message = "El agua utilizada es demasiado alta")
    private BigDecimal aguaUtilizada;

    @DecimalMin(value = "0.0", message = "El pH no puede ser negativo")
    @DecimalMax(value = "14.0", message = "El pH no puede superar 14")
    private BigDecimal phAgua;

    @DecimalMin(value = "0.0", message = "Los litros finales no pueden ser negativos")
    private BigDecimal litrosFinales;

    @Size(max = 200, message = "El clarificante no puede superar 200 caracteres")
    private String clarificante;

    @Min(value = 1000, message = "La densidad inicial mínima es 1000")
    @Max(value = 1150, message = "La densidad inicial máxima es 1150")
    private Integer densidadInicial;

    @Min(value = 990,  message = "La densidad final mínima es 990")
    @Max(value = 1060, message = "La densidad final máxima es 1060")
    private Integer densidadFinal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate densidadFinalFecha;

    private List<InsumoDto> maltas = new ArrayList<>();
    private List<InsumoDto> lupulos = new ArrayList<>();
    private List<InsumoDto> levaduras = new ArrayList<>();
    private List<InsumoDto> clarificantes = new ArrayList<>();

    // Fermentación
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fermFechaInicial;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fermFechaFinalIdeal;
    private BigDecimal fermTemperatura;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fermFechaFinal;

    // Acondicionamiento
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acondFechaInicial;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acondFechaFinalIdeal;
    private BigDecimal acondTemperatura;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acondFechaFinal;

    // Maduración
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate madurFechaInicial;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate madurFechaFinalIdeal;
    private BigDecimal madurTemperatura;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate madurFechaFinal;

    // Carbonatación
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaInicial;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaFinalIdeal;
    private BigDecimal carbTemperatura;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaFinal;

    private String observaciones;
    private String notasCata;
    private Long recetaId;
    private List<Long> itemsIds = new ArrayList<>();
    private List<BigDecimal> itemsCantidades = new ArrayList<>();

    public static LoteFormDto empty() {
        LoteFormDto dto = new LoteFormDto();
        dto.maltas.add(new InsumoDto());
        dto.lupulos.add(new InsumoDto());
        dto.levaduras.add(new InsumoDto());
        dto.clarificantes.add(new InsumoDto());
        return dto;
    }

    // Getters & Setters
    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = estilo; }
    public Long getEquipoFermentadorId() { return equipoFermentadorId; }
    public void setEquipoFermentadorId(Long equipoFermentadorId) { this.equipoFermentadorId = equipoFermentadorId; }
    public LocalDate getFechaElaboracion() { return fechaElaboracion; }
    public void setFechaElaboracion(LocalDate fechaElaboracion) { this.fechaElaboracion = fechaElaboracion; }
    public BigDecimal getAguaUtilizada() { return aguaUtilizada; }
    public void setAguaUtilizada(BigDecimal aguaUtilizada) { this.aguaUtilizada = aguaUtilizada; }
    public BigDecimal getPhAgua() { return phAgua; }
    public void setPhAgua(BigDecimal phAgua) { this.phAgua = phAgua; }
    public BigDecimal getLitrosFinales() { return litrosFinales; }
    public void setLitrosFinales(BigDecimal litrosFinales) { this.litrosFinales = litrosFinales; }
    public String getClarificante() { return clarificante; }
    public void setClarificante(String clarificante) { this.clarificante = clarificante; }
    public Integer getDensidadInicial() { return densidadInicial; }
    public void setDensidadInicial(Integer densidadInicial) { this.densidadInicial = densidadInicial; }
    public Integer getDensidadFinal() { return densidadFinal; }
    public void setDensidadFinal(Integer densidadFinal) { this.densidadFinal = densidadFinal; }
    public LocalDate getDensidadFinalFecha() { return densidadFinalFecha; }
    public void setDensidadFinalFecha(LocalDate densidadFinalFecha) { this.densidadFinalFecha = densidadFinalFecha; }
    public List<InsumoDto> getMaltas() { return maltas; }
    public void setMaltas(List<InsumoDto> maltas) { this.maltas = maltas; }
    public List<InsumoDto> getLupulos() { return lupulos; }
    public void setLupulos(List<InsumoDto> lupulos) { this.lupulos = lupulos; }
    public List<InsumoDto> getLevaduras() { return levaduras; }
    public void setLevaduras(List<InsumoDto> levaduras) { this.levaduras = levaduras; }
    public List<InsumoDto> getClarificantes() { return clarificantes; }
    public void setClarificantes(List<InsumoDto> clarificantes) { this.clarificantes = clarificantes; }
    public LocalDate getFermFechaInicial() { return fermFechaInicial; }
    public void setFermFechaInicial(LocalDate fermFechaInicial) { this.fermFechaInicial = fermFechaInicial; }
    public LocalDate getFermFechaFinalIdeal() { return fermFechaFinalIdeal; }
    public void setFermFechaFinalIdeal(LocalDate fermFechaFinalIdeal) { this.fermFechaFinalIdeal = fermFechaFinalIdeal; }
    public BigDecimal getFermTemperatura() { return fermTemperatura; }
    public void setFermTemperatura(BigDecimal fermTemperatura) { this.fermTemperatura = fermTemperatura; }
    public LocalDate getFermFechaFinal() { return fermFechaFinal; }
    public void setFermFechaFinal(LocalDate fermFechaFinal) { this.fermFechaFinal = fermFechaFinal; }
    public LocalDate getAcondFechaInicial() { return acondFechaInicial; }
    public void setAcondFechaInicial(LocalDate acondFechaInicial) { this.acondFechaInicial = acondFechaInicial; }
    public LocalDate getAcondFechaFinalIdeal() { return acondFechaFinalIdeal; }
    public void setAcondFechaFinalIdeal(LocalDate acondFechaFinalIdeal) { this.acondFechaFinalIdeal = acondFechaFinalIdeal; }
    public BigDecimal getAcondTemperatura() { return acondTemperatura; }
    public void setAcondTemperatura(BigDecimal acondTemperatura) { this.acondTemperatura = acondTemperatura; }
    public LocalDate getAcondFechaFinal() { return acondFechaFinal; }
    public void setAcondFechaFinal(LocalDate acondFechaFinal) { this.acondFechaFinal = acondFechaFinal; }
    public LocalDate getMadurFechaInicial() { return madurFechaInicial; }
    public void setMadurFechaInicial(LocalDate madurFechaInicial) { this.madurFechaInicial = madurFechaInicial; }
    public LocalDate getMadurFechaFinalIdeal() { return madurFechaFinalIdeal; }
    public void setMadurFechaFinalIdeal(LocalDate madurFechaFinalIdeal) { this.madurFechaFinalIdeal = madurFechaFinalIdeal; }
    public BigDecimal getMadurTemperatura() { return madurTemperatura; }
    public void setMadurTemperatura(BigDecimal madurTemperatura) { this.madurTemperatura = madurTemperatura; }
    public LocalDate getMadurFechaFinal() { return madurFechaFinal; }
    public void setMadurFechaFinal(LocalDate madurFechaFinal) { this.madurFechaFinal = madurFechaFinal; }
    public LocalDate getCarbFechaInicial() { return carbFechaInicial; }
    public void setCarbFechaInicial(LocalDate carbFechaInicial) { this.carbFechaInicial = carbFechaInicial; }
    public LocalDate getCarbFechaFinalIdeal() { return carbFechaFinalIdeal; }
    public void setCarbFechaFinalIdeal(LocalDate carbFechaFinalIdeal) { this.carbFechaFinalIdeal = carbFechaFinalIdeal; }
    public BigDecimal getCarbTemperatura() { return carbTemperatura; }
    public void setCarbTemperatura(BigDecimal carbTemperatura) { this.carbTemperatura = carbTemperatura; }
    public LocalDate getCarbFechaFinal() { return carbFechaFinal; }
    public void setCarbFechaFinal(LocalDate carbFechaFinal) { this.carbFechaFinal = carbFechaFinal; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getNotasCata() { return notasCata; }
    public void setNotasCata(String notasCata) { this.notasCata = notasCata; }
    public Long getRecetaId() { return recetaId; }
    public void setRecetaId(Long recetaId) { this.recetaId = recetaId; }
    public List<Long> getItemsIds() { return itemsIds; }
    public void setItemsIds(List<Long> itemsIds) { this.itemsIds = itemsIds; }
    public List<BigDecimal> getItemsCantidades() { return itemsCantidades; }
    public void setItemsCantidades(List<BigDecimal> itemsCantidades) { this.itemsCantidades = itemsCantidades; }
}
