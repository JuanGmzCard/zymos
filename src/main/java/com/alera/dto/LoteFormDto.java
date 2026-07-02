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

    @Min(value = 1, message = "El número de elaboraciones debe ser entre 1 y 3")
    @Max(value = 3, message = "El número de elaboraciones debe ser entre 1 y 3")
    private Integer numeroElaboraciones = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaSegundaElaboracion;

    @DecimalMin(value = "0.0", message = "El agua de la segunda elaboración no puede ser negativa")
    @DecimalMax(value = "99999.999", message = "El agua de la segunda elaboración es demasiado alta")
    private BigDecimal aguaSegundaElaboracion;

    @Min(value = 1000, message = "La densidad de la primera elaboración mínima es 1000")
    @Max(value = 1150, message = "La densidad de la primera elaboración máxima es 1150")
    private Integer ogPrimeraElaboracion;

    @Min(value = 1000, message = "La densidad de la segunda elaboración mínima es 1000")
    @Max(value = 1150, message = "La densidad de la segunda elaboración máxima es 1150")
    private Integer ogSegundaElaboracion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaTerceraElaboracion;

    @DecimalMin(value = "0.0", message = "El agua de la tercera elaboración no puede ser negativa")
    @DecimalMax(value = "99999.999", message = "El agua de la tercera elaboración es demasiado alta")
    private BigDecimal aguaTerceraElaboracion;

    @Min(value = 1000, message = "La densidad de la tercera elaboración mínima es 1000")
    @Max(value = 1150, message = "La densidad de la tercera elaboración máxima es 1150")
    private Integer ogTerceraElaboracion;

    @DecimalMin(value = "1.0", message = "El °Brix de la segunda elaboración debe ser mayor a 1")
    @DecimalMax(value = "40.0", message = "El °Brix de la segunda elaboración no puede superar 40")
    private BigDecimal ogBrixSegundaElaboracion;

    @DecimalMin(value = "1.0", message = "El °Brix de la tercera elaboración debe ser mayor a 1")
    @DecimalMax(value = "40.0", message = "El °Brix de la tercera elaboración no puede superar 40")
    private BigDecimal ogBrixTerceraElaboracion;

    @DecimalMin(value = "0.0", message = "El volumen final de la primera elaboración no puede ser negativo")
    private BigDecimal volumenFinalPrimeraElaboracion;

    @DecimalMin(value = "0.0", message = "El volumen final de la segunda elaboración no puede ser negativo")
    private BigDecimal volumenFinalSegundaElaboracion;

    @DecimalMin(value = "0.0", message = "El volumen final de la tercera elaboración no puede ser negativo")
    private BigDecimal volumenFinalTerceraElaboracion;

    private java.time.LocalTime horaInicioPrimeraElaboracion;
    private java.time.LocalTime horaFinPrimeraElaboracion;
    private java.time.LocalTime horaInicioSegundaElaboracion;
    private java.time.LocalTime horaFinSegundaElaboracion;
    private java.time.LocalTime horaInicioTerceraElaboracion;
    private java.time.LocalTime horaFinTerceraElaboracion;

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

    // Instrumento de medición — "SG" (hidrómetro, default) o "BRIX" (refractómetro)
    private String instrumentoMedicion;

    @Min(value = 1000, message = "La densidad inicial mínima es 1000")
    @Max(value = 1150, message = "La densidad inicial máxima es 1150")
    private Integer densidadInicial;

    @Min(value = 990,  message = "La densidad final mínima es 990")
    @Max(value = 1060, message = "La densidad final máxima es 1060")
    private Integer densidadFinal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate densidadFinalFecha;

    // Medición con refractómetro
    @DecimalMin(value = "1.0",  message = "El °Brix inicial debe ser mayor a 1")
    @DecimalMax(value = "40.0", message = "El °Brix inicial no puede superar 40")
    private BigDecimal ogBrix;

    @DecimalMin(value = "1.0",  message = "El °Brix final debe ser mayor a 1")
    @DecimalMax(value = "40.0", message = "El °Brix final no puede superar 40")
    private BigDecimal fgBrix;

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

    // Carbonatación — fechas y temperatura
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaInicial;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaFinalIdeal;
    private BigDecimal carbTemperatura;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carbFechaFinal;

    // Carbonatación avanzada — capa 1: variables iniciales
    private String carbMetodo;
    private BigDecimal carbCo2Objetivo;
    private BigDecimal carbCo2Real;

    // Carbonatación avanzada — capa 2: Natural (priming)
    private String carbAzucarTipo;
    private BigDecimal carbAzucarGramos;

    // Carbonatación avanzada — capa 2: Forzada
    private BigDecimal carbPresionPsi;
    private Integer carbTiempoHoras;
    private String carbTecnica;

    // Carbonatación avanzada — capa 3: control de calidad
    private String carbValidacion;
    private String carbDestino;

    private String observaciones;
    private String notasCata;
    private Long recetaId;
    private Long receta2Id;
    private Long receta3Id;
    private List<Long> itemsIds = new ArrayList<>();
    private List<BigDecimal> itemsCantidades = new ArrayList<>();

    public static LoteFormDto empty() {
        LoteFormDto dto = new LoteFormDto();
        dto.fechaElaboracion = LocalDate.now();
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
    public Integer getNumeroElaboraciones() { return numeroElaboraciones; }
    public void setNumeroElaboraciones(Integer numeroElaboraciones) { this.numeroElaboraciones = numeroElaboraciones; }
    public LocalDate getFechaSegundaElaboracion() { return fechaSegundaElaboracion; }
    public void setFechaSegundaElaboracion(LocalDate fechaSegundaElaboracion) { this.fechaSegundaElaboracion = fechaSegundaElaboracion; }
    public BigDecimal getAguaSegundaElaboracion() { return aguaSegundaElaboracion; }
    public void setAguaSegundaElaboracion(BigDecimal aguaSegundaElaboracion) { this.aguaSegundaElaboracion = aguaSegundaElaboracion; }
    public Integer getOgSegundaElaboracion() { return ogSegundaElaboracion; }
    public void setOgSegundaElaboracion(Integer ogSegundaElaboracion) { this.ogSegundaElaboracion = ogSegundaElaboracion; }
    public LocalDate getFechaTerceraElaboracion() { return fechaTerceraElaboracion; }
    public void setFechaTerceraElaboracion(LocalDate fechaTerceraElaboracion) { this.fechaTerceraElaboracion = fechaTerceraElaboracion; }
    public BigDecimal getAguaTerceraElaboracion() { return aguaTerceraElaboracion; }
    public void setAguaTerceraElaboracion(BigDecimal aguaTerceraElaboracion) { this.aguaTerceraElaboracion = aguaTerceraElaboracion; }
    public Integer getOgTerceraElaboracion() { return ogTerceraElaboracion; }
    public void setOgTerceraElaboracion(Integer ogTerceraElaboracion) { this.ogTerceraElaboracion = ogTerceraElaboracion; }
    public Integer getOgPrimeraElaboracion() { return ogPrimeraElaboracion; }
    public void setOgPrimeraElaboracion(Integer v) { this.ogPrimeraElaboracion = v; }
    public BigDecimal getVolumenFinalPrimeraElaboracion() { return volumenFinalPrimeraElaboracion; }
    public void setVolumenFinalPrimeraElaboracion(BigDecimal v) { this.volumenFinalPrimeraElaboracion = v; }
    public BigDecimal getOgBrixSegundaElaboracion() { return ogBrixSegundaElaboracion; }
    public void setOgBrixSegundaElaboracion(BigDecimal v) { this.ogBrixSegundaElaboracion = v; }
    public BigDecimal getOgBrixTerceraElaboracion() { return ogBrixTerceraElaboracion; }
    public void setOgBrixTerceraElaboracion(BigDecimal v) { this.ogBrixTerceraElaboracion = v; }
    public BigDecimal getVolumenFinalSegundaElaboracion() { return volumenFinalSegundaElaboracion; }
    public void setVolumenFinalSegundaElaboracion(BigDecimal v) { this.volumenFinalSegundaElaboracion = v; }
    public BigDecimal getVolumenFinalTerceraElaboracion() { return volumenFinalTerceraElaboracion; }
    public void setVolumenFinalTerceraElaboracion(BigDecimal v) { this.volumenFinalTerceraElaboracion = v; }
    public java.time.LocalTime getHoraInicioPrimeraElaboracion() { return horaInicioPrimeraElaboracion; }
    public void setHoraInicioPrimeraElaboracion(java.time.LocalTime v) { this.horaInicioPrimeraElaboracion = v; }
    public java.time.LocalTime getHoraFinPrimeraElaboracion() { return horaFinPrimeraElaboracion; }
    public void setHoraFinPrimeraElaboracion(java.time.LocalTime v) { this.horaFinPrimeraElaboracion = v; }
    public java.time.LocalTime getHoraInicioSegundaElaboracion() { return horaInicioSegundaElaboracion; }
    public void setHoraInicioSegundaElaboracion(java.time.LocalTime v) { this.horaInicioSegundaElaboracion = v; }
    public java.time.LocalTime getHoraFinSegundaElaboracion() { return horaFinSegundaElaboracion; }
    public void setHoraFinSegundaElaboracion(java.time.LocalTime v) { this.horaFinSegundaElaboracion = v; }
    public java.time.LocalTime getHoraInicioTerceraElaboracion() { return horaInicioTerceraElaboracion; }
    public void setHoraInicioTerceraElaboracion(java.time.LocalTime v) { this.horaInicioTerceraElaboracion = v; }
    public java.time.LocalTime getHoraFinTerceraElaboracion() { return horaFinTerceraElaboracion; }
    public void setHoraFinTerceraElaboracion(java.time.LocalTime v) { this.horaFinTerceraElaboracion = v; }
    public BigDecimal getAguaUtilizada() { return aguaUtilizada; }
    public void setAguaUtilizada(BigDecimal aguaUtilizada) { this.aguaUtilizada = aguaUtilizada; }
    public BigDecimal getPhAgua() { return phAgua; }
    public void setPhAgua(BigDecimal phAgua) { this.phAgua = phAgua; }
    public BigDecimal getLitrosFinales() { return litrosFinales; }
    public void setLitrosFinales(BigDecimal litrosFinales) { this.litrosFinales = litrosFinales; }
    public String getClarificante() { return clarificante; }
    public void setClarificante(String clarificante) { this.clarificante = clarificante; }
    public String getInstrumentoMedicion() { return instrumentoMedicion; }
    public void setInstrumentoMedicion(String instrumentoMedicion) { this.instrumentoMedicion = instrumentoMedicion; }
    public Integer getDensidadInicial() { return densidadInicial; }
    public void setDensidadInicial(Integer densidadInicial) { this.densidadInicial = densidadInicial; }
    public Integer getDensidadFinal() { return densidadFinal; }
    public void setDensidadFinal(Integer densidadFinal) { this.densidadFinal = densidadFinal; }
    public LocalDate getDensidadFinalFecha() { return densidadFinalFecha; }
    public void setDensidadFinalFecha(LocalDate densidadFinalFecha) { this.densidadFinalFecha = densidadFinalFecha; }
    public BigDecimal getOgBrix() { return ogBrix; }
    public void setOgBrix(BigDecimal ogBrix) { this.ogBrix = ogBrix; }
    public BigDecimal getFgBrix() { return fgBrix; }
    public void setFgBrix(BigDecimal fgBrix) { this.fgBrix = fgBrix; }
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
    public String getCarbMetodo() { return carbMetodo; }
    public void setCarbMetodo(String carbMetodo) { this.carbMetodo = carbMetodo; }
    public BigDecimal getCarbCo2Objetivo() { return carbCo2Objetivo; }
    public void setCarbCo2Objetivo(BigDecimal carbCo2Objetivo) { this.carbCo2Objetivo = carbCo2Objetivo; }
    public BigDecimal getCarbCo2Real() { return carbCo2Real; }
    public void setCarbCo2Real(BigDecimal carbCo2Real) { this.carbCo2Real = carbCo2Real; }
    public String getCarbAzucarTipo() { return carbAzucarTipo; }
    public void setCarbAzucarTipo(String carbAzucarTipo) { this.carbAzucarTipo = carbAzucarTipo; }
    public BigDecimal getCarbAzucarGramos() { return carbAzucarGramos; }
    public void setCarbAzucarGramos(BigDecimal carbAzucarGramos) { this.carbAzucarGramos = carbAzucarGramos; }
    public BigDecimal getCarbPresionPsi() { return carbPresionPsi; }
    public void setCarbPresionPsi(BigDecimal carbPresionPsi) { this.carbPresionPsi = carbPresionPsi; }
    public Integer getCarbTiempoHoras() { return carbTiempoHoras; }
    public void setCarbTiempoHoras(Integer carbTiempoHoras) { this.carbTiempoHoras = carbTiempoHoras; }
    public String getCarbTecnica() { return carbTecnica; }
    public void setCarbTecnica(String carbTecnica) { this.carbTecnica = carbTecnica; }
    public String getCarbValidacion() { return carbValidacion; }
    public void setCarbValidacion(String carbValidacion) { this.carbValidacion = carbValidacion; }
    public String getCarbDestino() { return carbDestino; }
    public void setCarbDestino(String carbDestino) { this.carbDestino = carbDestino; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getNotasCata() { return notasCata; }
    public void setNotasCata(String notasCata) { this.notasCata = notasCata; }
    public Long getRecetaId() { return recetaId; }
    public void setRecetaId(Long recetaId) { this.recetaId = recetaId; }
    public Long getReceta2Id() { return receta2Id; }
    public void setReceta2Id(Long receta2Id) { this.receta2Id = receta2Id; }
    public Long getReceta3Id() { return receta3Id; }
    public void setReceta3Id(Long receta3Id) { this.receta3Id = receta3Id; }
    public List<Long> getItemsIds() { return itemsIds; }
    public void setItemsIds(List<Long> itemsIds) { this.itemsIds = itemsIds; }
    public List<BigDecimal> getItemsCantidades() { return itemsCantidades; }
    public void setItemsCantidades(List<BigDecimal> itemsCantidades) { this.itemsCantidades = itemsCantidades; }
}
