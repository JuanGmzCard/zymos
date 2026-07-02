package com.alera.model;

import com.alera.model.enums.TipoIngrediente;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "lotes_cerveza")
@SQLRestriction("deleted_at IS NULL")
public class LoteCerveza extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigoLote;

    @Column(nullable = false)
    private String estilo;

    private LocalDate fechaElaboracion;

    private Integer numeroElaboraciones = 1;
    @Column(name = "og_primera_elaboracion")
    private Integer ogPrimeraElaboracion;
    private LocalDate fechaSegundaElaboracion;
    private BigDecimal aguaSegundaElaboracion;
    @Column(name = "og_segunda_elaboracion")
    private Integer ogSegundaElaboracion;
    @Column(name = "og_brix_segunda_elaboracion", precision = 5, scale = 2)
    private BigDecimal ogBrixSegundaElaboracion;
    private LocalDate fechaTerceraElaboracion;
    private BigDecimal aguaTerceraElaboracion;
    @Column(name = "og_tercera_elaboracion")
    private Integer ogTerceraElaboracion;
    @Column(name = "og_brix_tercera_elaboracion", precision = 5, scale = 2)
    private BigDecimal ogBrixTerceraElaboracion;
    private BigDecimal volumenFinalPrimeraElaboracion;
    private BigDecimal volumenFinalSegundaElaboracion;
    private BigDecimal volumenFinalTerceraElaboracion;

    private java.time.LocalTime horaInicioPrimeraElaboracion;
    private java.time.LocalTime horaFinPrimeraElaboracion;
    private java.time.LocalTime horaInicioSegundaElaboracion;
    private java.time.LocalTime horaFinSegundaElaboracion;
    private java.time.LocalTime horaInicioTerceraElaboracion;
    private java.time.LocalTime horaFinTerceraElaboracion;

    private BigDecimal aguaUtilizada;
    private BigDecimal phAgua;
    private BigDecimal litrosFinales;
    private String clarificante;

    private Integer densidadInicial;
    private Integer densidadFinal;
    private LocalDate densidadFinalFecha;

    // Medición con refractómetro — valores Brix originales
    @Column(name = "og_brix", precision = 5, scale = 2)
    private BigDecimal ogBrix;
    @Column(name = "fg_brix", precision = 5, scale = 2)
    private BigDecimal fgBrix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_fermentador_id")
    private Equipo equipoFermentador;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ingrediente> ingredientes = new ArrayList<>();

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoteItemFactura> itemsFactura = new ArrayList<>();

    // Fermentación
    private LocalDate fermFechaInicial;
    private LocalDate fermFechaFinalIdeal;
    private BigDecimal fermTemperatura;
    private LocalDate fermFechaFinal;

    // Acondicionamiento
    private LocalDate acondFechaInicial;
    private LocalDate acondFechaFinalIdeal;
    private BigDecimal acondTemperatura;
    private LocalDate acondFechaFinal;

    // Maduración
    private LocalDate madurFechaInicial;
    private LocalDate madurFechaFinalIdeal;
    private BigDecimal madurTemperatura;
    private LocalDate madurFechaFinal;

    // Carbonatación — fechas y temperatura
    private LocalDate carbFechaInicial;
    private LocalDate carbFechaFinalIdeal;
    private BigDecimal carbTemperatura;
    private LocalDate carbFechaFinal;

    // Carbonatación avanzada — capa 1: variables iniciales
    private String carbMetodo;           // NATURAL / FORZADA
    @Column(name = "carb_co2_objetivo")
    private BigDecimal carbCo2Objetivo;
    @Column(name = "carb_co2_real")
    private BigDecimal carbCo2Real;

    // Carbonatación avanzada — capa 2: método Natural (priming)
    private String carbAzucarTipo;
    private BigDecimal carbAzucarGramos;

    // Carbonatación avanzada — capa 2: método Forzada
    private BigDecimal carbPresionPsi;
    private Integer carbTiempoHoras;
    private String carbTecnica;          // PIEDRA / PRESION_FIJA

    // Carbonatación avanzada — capa 3: control de calidad
    private String carbValidacion;       // ADECUADA / RETENCION_CORRECTA / SOBRECARBONATADA / BAJA_CARBONATACION
    private String carbDestino;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(columnDefinition = "TEXT", name = "notas_cata")
    private String notasCata;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta2_id")
    private Receta receta2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta3_id")
    private Receta receta3;


    // Filtros de ingredientes
    public List<Ingrediente> getMaltas() {
        return ingredientes.stream()
                .filter(i -> i.getTipo() == TipoIngrediente.MALTA)
                .collect(Collectors.toList());
    }

    public List<Ingrediente> getLupulos() {
        return ingredientes.stream()
                .filter(i -> i.getTipo() == TipoIngrediente.LUPULO)
                .collect(Collectors.toList());
    }

    public List<Ingrediente> getLevaduras() {
        return ingredientes.stream()
                .filter(i -> i.getTipo() == TipoIngrediente.LEVADURA)
                .collect(Collectors.toList());
    }

    public List<Ingrediente> getClarificantes() {
        return ingredientes.stream()
                .filter(i -> i.getTipo() == TipoIngrediente.CLARIFICANTE)
                .collect(Collectors.toList());
    }

    public String getFaseActual() {
        if (carbFechaInicial != null) return "Carbonatación";
        if (madurFechaInicial != null) return "Maduración";
        if (acondFechaInicial != null) return "Acondicionamiento";
        if (fermFechaInicial != null) return "Fermentación";
        return "Inicio";
    }

    public boolean isCompletado() {
        return carbFechaFinal != null;
    }

    // Costo de producción — suma del valor proporcional de cada ítem asignado
    public BigDecimal getCostoTotal() {
        if (itemsFactura == null || itemsFactura.isEmpty()) return null;
        return itemsFactura.stream()
                .map(LoteItemFactura::getValorAsignado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getCostoPorLitro() {
        BigDecimal costo = getCostoTotal();
        if (costo == null || litrosFinales == null || litrosFinales.compareTo(BigDecimal.ZERO) == 0)
            return null;
        return costo.divide(litrosFinales, 2, RoundingMode.HALF_UP);
    }

    // Kanban: días transcurridos en la fase actual
    public long getDiasEnFaseActual() {
        LocalDate inicio = null;
        if (carbFechaInicial  != null) inicio = carbFechaInicial;
        else if (madurFechaInicial != null) inicio = madurFechaInicial;
        else if (acondFechaInicial != null) inicio = acondFechaInicial;
        else if (fermFechaInicial  != null) inicio = fermFechaInicial;
        else if (fechaElaboracion  != null) inicio = fechaElaboracion;
        return inicio != null ? ChronoUnit.DAYS.between(inicio, LocalDate.now()) : 0;
    }

    // Conversión Brix → SG (mosto sin fermentar)
    public Integer getOgSgFromBrix() {
        if (ogBrix == null) return null;
        double b = ogBrix.doubleValue();
        double sg = b / (258.6 - (b / 258.2 * 227.1)) + 1.0;
        return (int) Math.round(sg * 1000);
    }

    // Corrección de Sean Terrill — FG real cuando FG fue medido con refractómetro
    public Integer getFgSgTerrill() {
        if (ogBrix == null || fgBrix == null) return null;
        double o = ogBrix.doubleValue();
        double f = fgBrix.doubleValue();
        double fg = 1.0000
                - 0.0044993 * o
                + 0.011774  * f
                + 0.00027581 * o * o
                - 0.0012717  * f * f
                - 0.0000072800 * o * o * o
                + 0.000063293  * f * f * f;
        return (int) Math.round(fg * 1000);
    }

    // ABV usando OG desde Brix + FG corregido por Terrill
    public BigDecimal getAbvTerrill() {
        Integer ogSg = getOgSgFromBrix();
        Integer fgSg = getFgSgTerrill();
        if (ogSg == null || fgSg == null) return null;
        return BigDecimal.valueOf((ogSg - fgSg) * 131.25 / 1000.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Cálculos de calidad — densidades en formato XXXX (ej: 1056, 1015)
    public BigDecimal getAbv() {
        if (densidadInicial == null || densidadFinal == null) return null;
        return BigDecimal.valueOf((densidadInicial - densidadFinal) * 131.25 / 1000.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAtenuacionAparente() {
        if (densidadInicial == null || densidadFinal == null) return null;
        int denominador = densidadInicial - 1000;
        if (denominador <= 0) return null;
        return BigDecimal.valueOf((densidadInicial - densidadFinal) * 100.0 / denominador)
                .setScale(1, RoundingMode.HALF_UP);
    }

    // Eficiencia de macerado: (OG_puntos × litros) / (kg_malta × 308) × 100
    public BigDecimal getEficienciaMacerado() {
        if (densidadInicial == null || litrosFinales == null) return null;
        BigDecimal totalMaltaKg = BigDecimal.ZERO;
        for (Ingrediente ing : ingredientes) {
            if (ing.getTipo() != TipoIngrediente.MALTA || ing.getCantidad() == null) continue;
            String[] partes = ing.getCantidad().trim().split("\\s+");
            if (partes.length < 2) continue;
            try {
                BigDecimal valor = new BigDecimal(partes[0]);
                String unidad = partes[1].toLowerCase();
                if (unidad.equals("gr") || unidad.equals("g")) {
                    totalMaltaKg = totalMaltaKg.add(valor.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP));
                } else if (unidad.equals("kg")) {
                    totalMaltaKg = totalMaltaKg.add(valor);
                }
            } catch (NumberFormatException ignored) {}
        }
        if (totalMaltaKg.compareTo(BigDecimal.ZERO) == 0) return null;
        BigDecimal ogPuntos = BigDecimal.valueOf(densidadInicial - 1000); // ya en puntos
        BigDecimal potencial = totalMaltaKg.multiply(new BigDecimal("308"));
        return ogPuntos.multiply(litrosFinales)
                .divide(potencial, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoLote() { return codigoLote; }
    public void setCodigoLote(String codigoLote) { this.codigoLote = codigoLote; }
    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = estilo; }
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
    public Integer getDensidadInicial() { return densidadInicial; }
    public void setDensidadInicial(Integer densidadInicial) { this.densidadInicial = densidadInicial; }
    public Integer getDensidadFinal() { return densidadFinal; }
    public void setDensidadFinal(Integer densidadFinal) { this.densidadFinal = densidadFinal; }
    public LocalDate getDensidadFinalFecha() { return densidadFinalFecha; }
    public void setDensidadFinalFecha(LocalDate densidadFinalFecha) { this.densidadFinalFecha = densidadFinalFecha; }
    public Equipo getEquipoFermentador() { return equipoFermentador; }
    public void setEquipoFermentador(Equipo equipoFermentador) { this.equipoFermentador = equipoFermentador; }
    public List<Ingrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<Ingrediente> ingredientes) { this.ingredientes = ingredientes; }
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
    public BigDecimal getOgBrix() { return ogBrix; }
    public void setOgBrix(BigDecimal ogBrix) { this.ogBrix = ogBrix; }
    public BigDecimal getFgBrix() { return fgBrix; }
    public void setFgBrix(BigDecimal fgBrix) { this.fgBrix = fgBrix; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getNotasCata() { return notasCata; }
    public void setNotasCata(String notasCata) { this.notasCata = notasCata; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }
    public Receta getReceta2() { return receta2; }
    public void setReceta2(Receta receta2) { this.receta2 = receta2; }
    public Receta getReceta3() { return receta3; }
    public void setReceta3(Receta receta3) { this.receta3 = receta3; }
    public List<LoteItemFactura> getItemsFactura() { return itemsFactura; }
    public void setItemsFactura(List<LoteItemFactura> itemsFactura) { this.itemsFactura = itemsFactura; }
}
