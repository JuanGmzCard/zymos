package com.alera.service;

import com.alera.dto.InsumoDto;
import com.alera.dto.RecetaFormDto;
import com.alera.model.AdicionHervor;
import com.alera.model.EscalonMacerado;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;

@Service
public class BeerXmlService {

    // ── Export ────────────────────────────────────────────────────────────────

    public byte[] exportar(Receta receta) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element recipes = doc.createElement("RECIPES");
            doc.appendChild(recipes);
            Element recipe = doc.createElement("RECIPE");
            recipes.appendChild(recipe);

            addText(doc, recipe, "NAME", receta.getNombre());
            addText(doc, recipe, "VERSION", "1");
            addText(doc, recipe, "TYPE", "All Grain");
            addText(doc, recipe, "BREWER", "Zymos");

            if (receta.getEstilo() != null) {
                Element style = doc.createElement("STYLE");
                recipe.appendChild(style);
                addText(doc, style, "NAME", receta.getEstilo());
                addText(doc, style, "VERSION", "1");
                addText(doc, style, "CATEGORY", receta.getEstilo());
                addText(doc, style, "CATEGORY_NUMBER", "1");
                addText(doc, style, "STYLE_LETTER", "A");
                addText(doc, style, "STYLE_GUIDE", "BJCP");
                addText(doc, style, "TYPE", "Ale");
            }

            if (receta.getVolumenBase() != null) {
                addText(doc, recipe, "BATCH_SIZE", fmt(receta.getVolumenBase()));
            }
            if (receta.getTiempoHervorMinutos() != null) {
                addText(doc, recipe, "BOIL_TIME", String.valueOf(receta.getTiempoHervorMinutos()));
            }
            addText(doc, recipe, "EFFICIENCY", "75");
            if (receta.getOgObjetivo() != null) {
                addText(doc, recipe, "OG", String.format(Locale.US, "%.3f", receta.getOgObjetivo() / 1000.0));
            }
            if (receta.getFgObjetivo() != null) {
                addText(doc, recipe, "FG", String.format(Locale.US, "%.3f", receta.getFgObjetivo() / 1000.0));
            }
            if (receta.getNotas() != null && !receta.getNotas().isBlank()) {
                addText(doc, recipe, "NOTES", receta.getNotas());
            }

            // FERMENTABLES
            if (!receta.getMaltas().isEmpty()) {
                Element fermentables = doc.createElement("FERMENTABLES");
                recipe.appendChild(fermentables);
                for (RecetaIngrediente ri : receta.getMaltas()) {
                    Element f = doc.createElement("FERMENTABLE");
                    fermentables.appendChild(f);
                    addText(doc, f, "NAME", ri.getNombre());
                    addText(doc, f, "VERSION", "1");
                    addText(doc, f, "TYPE", "Grain");
                    addText(doc, f, "AMOUNT", toKg(ri.getCantidad()));
                    addText(doc, f, "YIELD", "78.0");
                    addText(doc, f, "COLOR", "3.0");
                }
            }

            // HOPS — from adicionesHervor (with timing) + lupulos not covered there
            List<AdicionHervor> adiciones = receta.getAdicionesHervor();
            Set<String> hopNamesInAdiciones = new HashSet<>();
            adiciones.forEach(a -> hopNamesInAdiciones.add(a.getNombre().toLowerCase()));

            List<RecetaIngrediente> lupulosExtra = receta.getLupulos().stream()
                    .filter(ri -> !hopNamesInAdiciones.contains(ri.getNombre().toLowerCase()))
                    .toList();

            if (!adiciones.isEmpty() || !lupulosExtra.isEmpty()) {
                Element hops = doc.createElement("HOPS");
                recipe.appendChild(hops);
                for (AdicionHervor a : adiciones) {
                    Element h = doc.createElement("HOP");
                    hops.appendChild(h);
                    addText(doc, h, "NAME", a.getNombre());
                    addText(doc, h, "VERSION", "1");
                    addText(doc, h, "ALPHA", "5.0");
                    addText(doc, h, "AMOUNT", adicionToKg(a));
                    addText(doc, h, "USE", "Boil");
                    addText(doc, h, "TIME", String.valueOf(a.getMinutosRestantes() != null ? a.getMinutosRestantes() : 60));
                    addText(doc, h, "FORM", "Pellet");
                }
                for (RecetaIngrediente ri : lupulosExtra) {
                    Element h = doc.createElement("HOP");
                    hops.appendChild(h);
                    addText(doc, h, "NAME", ri.getNombre());
                    addText(doc, h, "VERSION", "1");
                    addText(doc, h, "ALPHA", "5.0");
                    addText(doc, h, "AMOUNT", toKg(ri.getCantidad()));
                    addText(doc, h, "USE", "Boil");
                    addText(doc, h, "TIME", "60");
                    addText(doc, h, "FORM", "Pellet");
                }
            }

            // YEASTS
            if (!receta.getLevaduras().isEmpty()) {
                Element yeasts = doc.createElement("YEASTS");
                recipe.appendChild(yeasts);
                for (RecetaIngrediente ri : receta.getLevaduras()) {
                    Element y = doc.createElement("YEAST");
                    yeasts.appendChild(y);
                    addText(doc, y, "NAME", ri.getNombre());
                    addText(doc, y, "VERSION", "1");
                    addText(doc, y, "TYPE", "Ale");
                    addText(doc, y, "FORM", "Dry");
                    addText(doc, y, "AMOUNT", toKg(ri.getCantidad()));
                    addText(doc, y, "AMOUNT_IS_WEIGHT", "TRUE");
                }
            }

            // MISCS (clarificantes)
            if (!receta.getClarificantes().isEmpty()) {
                Element miscs = doc.createElement("MISCS");
                recipe.appendChild(miscs);
                for (RecetaIngrediente ri : receta.getClarificantes()) {
                    Element m = doc.createElement("MISC");
                    miscs.appendChild(m);
                    addText(doc, m, "NAME", ri.getNombre());
                    addText(doc, m, "VERSION", "1");
                    addText(doc, m, "USE", "Boil");
                    addText(doc, m, "TIME", "15");
                    String[] parts = ri.getCantidad() != null ? ri.getCantidad().trim().split("\\s+") : new String[]{"0", "gr"};
                    String unit = parts.length > 1 ? parts[1].toLowerCase() : "gr";
                    boolean liquid = unit.equals("ml") || unit.equals("l");
                    addText(doc, m, "AMOUNT", toKg(ri.getCantidad()));
                    addText(doc, m, "AMOUNT_IS_WEIGHT", liquid ? "FALSE" : "TRUE");
                    addText(doc, m, "TYPE", "Fining");
                }
            }

            // MASH
            if (!receta.getEscalones().isEmpty()) {
                Element mash = doc.createElement("MASH");
                recipe.appendChild(mash);
                addText(doc, mash, "NAME", "Mash");
                addText(doc, mash, "VERSION", "1");
                addText(doc, mash, "GRAIN_TEMP", "22.0");
                Element mashSteps = doc.createElement("MASH_STEPS");
                mash.appendChild(mashSteps);
                for (EscalonMacerado e : receta.getEscalones()) {
                    Element ms = doc.createElement("MASH_STEP");
                    mashSteps.appendChild(ms);
                    addText(doc, ms, "NAME", e.getNombre());
                    addText(doc, ms, "VERSION", "1");
                    addText(doc, ms, "TYPE", "Infusion");
                    addText(doc, ms, "STEP_TEMP", e.getTemperaturaC() != null ? fmt(e.getTemperaturaC()) : "67.0");
                    addText(doc, ms, "STEP_TIME", e.getDuracionMinutos() != null ? String.valueOf(e.getDuracionMinutos()) : "60");
                }
            }

            // WATERS (water profile)
            boolean hasWater = receta.getAguaCalcio() != null || receta.getAguaMagnesio() != null
                    || receta.getAguaSodio() != null || receta.getAguaSulfatos() != null
                    || receta.getAguaCloruros() != null || receta.getAguaBicarbonatos() != null;
            if (hasWater) {
                Element waters = doc.createElement("WATERS");
                recipe.appendChild(waters);
                Element water = doc.createElement("WATER");
                waters.appendChild(water);
                addText(doc, water, "NAME", "Perfil de agua");
                addText(doc, water, "VERSION", "1");
                addText(doc, water, "AMOUNT",
                        receta.getVolumenBase() != null ? fmt(receta.getVolumenBase()) : "25.0");
                if (receta.getAguaCalcio() != null)       addText(doc, water, "CALCIUM",      String.valueOf(receta.getAguaCalcio()));
                if (receta.getAguaMagnesio() != null)     addText(doc, water, "MAGNESIUM",    String.valueOf(receta.getAguaMagnesio()));
                if (receta.getAguaSodio() != null)        addText(doc, water, "SODIUM",       String.valueOf(receta.getAguaSodio()));
                if (receta.getAguaSulfatos() != null)     addText(doc, water, "SULFATE",      String.valueOf(receta.getAguaSulfatos()));
                if (receta.getAguaCloruros() != null)     addText(doc, water, "CHLORIDE",     String.valueOf(receta.getAguaCloruros()));
                if (receta.getAguaBicarbonatos() != null) addText(doc, water, "BICARBONATE",  String.valueOf(receta.getAguaBicarbonatos()));
                if (receta.getAguaNotas() != null && !receta.getAguaNotas().isBlank())
                    addText(doc, water, "NOTES", receta.getAguaNotas());
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al exportar BeerXML: " + e.getMessage(), e);
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    public RecetaFormDto importar(InputStream xmlStream) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // XXE protection
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlStream);
            doc.getDocumentElement().normalize();

            NodeList recipeList = doc.getElementsByTagName("RECIPE");
            if (recipeList.getLength() == 0) {
                throw new RuntimeException("No se encontró elemento RECIPE en el XML");
            }
            Element recipe = (Element) recipeList.item(0);

            RecetaFormDto dto = new RecetaFormDto();

            dto.setNombre(text(recipe, "NAME"));
            dto.setActiva(true);

            // Style
            NodeList styleNodes = recipe.getElementsByTagName("STYLE");
            if (styleNodes.getLength() > 0) {
                dto.setEstilo(text((Element) styleNodes.item(0), "NAME"));
            }
            if (dto.getEstilo() == null || dto.getEstilo().isBlank()) {
                dto.setEstilo(text(recipe, "STYLE")); // fallback: plain text
            }

            String batchSize = text(recipe, "BATCH_SIZE");
            if (!batchSize.isEmpty()) {
                try { dto.setVolumenBase(new BigDecimal(batchSize)); } catch (NumberFormatException ignored) {}
            }
            String boilTime = text(recipe, "BOIL_TIME");
            if (!boilTime.isEmpty()) {
                try { dto.setTiempoHervorMinutos((int) dbl(boilTime)); } catch (NumberFormatException ignored) {}
            }
            String notes = text(recipe, "NOTES");
            if (!notes.isEmpty()) dto.setDescripcion(notes);

            String og = text(recipe, "OG");
            if (!og.isEmpty()) {
                try { dto.setOgObjetivo((int) Math.round(dbl(og) * 1000)); } catch (NumberFormatException ignored) {}
            }
            String fg = text(recipe, "FG");
            if (!fg.isEmpty()) {
                try { dto.setFgObjetivo((int) Math.round(dbl(fg) * 1000)); } catch (NumberFormatException ignored) {}
            }

            // FERMENTABLES → maltas
            NodeList fermentables = recipe.getElementsByTagName("FERMENTABLE");
            for (int i = 0; i < fermentables.getLength(); i++) {
                Element f = (Element) fermentables.item(i);
                InsumoDto insumo = new InsumoDto();
                insumo.setNombre(text(f, "NAME"));
                insumo.setCantidad(String.valueOf(Math.round(dbl(text(f, "AMOUNT")) * 1000)));
                insumo.setUnidad("gr");
                dto.getMaltas().add(insumo);
            }

            // HOPS → lupulos + adicionesHervor (Boil)
            NodeList hops = recipe.getElementsByTagName("HOP");
            Set<String> hopNamesSeen = new LinkedHashSet<>();
            for (int i = 0; i < hops.getLength(); i++) {
                Element h = (Element) hops.item(i);
                String nombre = text(h, "NAME");
                long amtGr = Math.round(dbl(text(h, "AMOUNT")) * 1000);
                String use = text(h, "USE");
                int time = (int) dbl(text(h, "TIME"));

                if ("Boil".equalsIgnoreCase(use) || "First Wort".equalsIgnoreCase(use) || use.isEmpty()) {
                    RecetaFormDto.AdicionHervorDto adic = new RecetaFormDto.AdicionHervorDto();
                    adic.setNombre(nombre);
                    adic.setMinutosRestantes(time);
                    adic.setCantidad(BigDecimal.valueOf(amtGr));
                    adic.setUnidad("gr");
                    dto.getAdicionesHervor().add(adic);
                }

                // Ingredient list — one entry per unique hop (cumulative quantity for first occurrence)
                if (hopNamesSeen.add(nombre.toLowerCase())) {
                    InsumoDto insumo = new InsumoDto();
                    insumo.setNombre(nombre);
                    insumo.setCantidad(String.valueOf(amtGr));
                    insumo.setUnidad("gr");
                    dto.getLupulos().add(insumo);
                }
            }

            // YEASTS → levaduras
            NodeList yeasts = recipe.getElementsByTagName("YEAST");
            for (int i = 0; i < yeasts.getLength(); i++) {
                Element y = (Element) yeasts.item(i);
                InsumoDto insumo = new InsumoDto();
                insumo.setNombre(text(y, "NAME"));
                insumo.setCantidad(String.valueOf(Math.round(dbl(text(y, "AMOUNT")) * 1000)));
                insumo.setUnidad("gr");
                dto.getLevaduras().add(insumo);
            }

            // MISCS → clarificantes (+ adicionesHervor when USE=Boil)
            NodeList miscs = recipe.getElementsByTagName("MISC");
            for (int i = 0; i < miscs.getLength(); i++) {
                Element m = (Element) miscs.item(i);
                String nombre = text(m, "NAME");
                double amt = dbl(text(m, "AMOUNT"));
                boolean isWeight = !"FALSE".equalsIgnoreCase(text(m, "AMOUNT_IS_WEIGHT"));
                String use = text(m, "USE");

                InsumoDto insumo = new InsumoDto();
                insumo.setNombre(nombre);
                if (isWeight) {
                    insumo.setCantidad(String.valueOf(Math.round(amt * 1000)));
                    insumo.setUnidad("gr");
                } else {
                    insumo.setCantidad(String.valueOf(Math.round(amt * 1000)));
                    insumo.setUnidad("mL");
                }
                dto.getClarificantes().add(insumo);

                if ("Boil".equalsIgnoreCase(use)) {
                    int time = (int) dbl(text(m, "TIME"));
                    RecetaFormDto.AdicionHervorDto adic = new RecetaFormDto.AdicionHervorDto();
                    adic.setNombre(nombre);
                    adic.setMinutosRestantes(time);
                    adic.setCantidad(isWeight ? BigDecimal.valueOf(Math.round(amt * 1000)) : BigDecimal.valueOf(Math.round(amt * 1000)));
                    adic.setUnidad(isWeight ? "gr" : "mL");
                    dto.getAdicionesHervor().add(adic);
                }
            }

            // MASH_STEPS → escalones
            NodeList mashSteps = recipe.getElementsByTagName("MASH_STEP");
            for (int i = 0; i < mashSteps.getLength(); i++) {
                Element ms = (Element) mashSteps.item(i);
                RecetaFormDto.EscalonDto esc = new RecetaFormDto.EscalonDto();
                esc.setNombre(text(ms, "NAME"));
                double stepTemp = dbl(text(ms, "STEP_TEMP"));
                if (stepTemp > 0) esc.setTemperaturaC(BigDecimal.valueOf(stepTemp));
                String stepTime = text(ms, "STEP_TIME");
                if (!stepTime.isEmpty()) esc.setDuracionMinutos((int) dbl(stepTime));
                dto.getEscalones().add(esc);
            }

            // WATERS → agua profile (first element)
            NodeList waters = recipe.getElementsByTagName("WATER");
            if (waters.getLength() > 0) {
                Element w = (Element) waters.item(0);
                setInt(w, "CALCIUM",     dto::setAguaCalcio);
                setInt(w, "MAGNESIUM",   dto::setAguaMagnesio);
                setInt(w, "SODIUM",      dto::setAguaSodio);
                setInt(w, "SULFATE",     dto::setAguaSulfatos);
                setInt(w, "CHLORIDE",    dto::setAguaCloruros);
                setInt(w, "BICARBONATE", dto::setAguaBicarbonatos);
                String wn = text(w, "NOTES");
                if (!wn.isEmpty()) dto.setAguaNotas(wn);
            }

            // Guarantee at least one row in each ingredient list
            if (dto.getMaltas().isEmpty())       dto.getMaltas().add(new InsumoDto());
            if (dto.getLupulos().isEmpty())      dto.getLupulos().add(new InsumoDto());
            if (dto.getLevaduras().isEmpty())    dto.getLevaduras().add(new InsumoDto());
            if (dto.getClarificantes().isEmpty()) dto.getClarificantes().add(new InsumoDto());

            return dto;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al importar BeerXML: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addText(Document doc, Element parent, String tag, String value) {
        Element el = doc.createElement(tag);
        el.setTextContent(value != null ? value : "");
        parent.appendChild(el);
    }

    private String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private double dbl(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    private String fmt(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private String toKg(String cantidadConUnidad) {
        if (cantidadConUnidad == null || cantidadConUnidad.isBlank()) return "0.000";
        String[] parts = cantidadConUnidad.trim().split("\\s+");
        double amount;
        try { amount = Double.parseDouble(parts[0]); } catch (NumberFormatException e) { return "0.000"; }
        String unit = parts.length > 1 ? parts[1].toLowerCase() : "gr";
        double kg = switch (unit) {
            case "kg" -> amount;
            case "l"  -> amount;
            case "ml" -> amount / 1000.0;
            default   -> amount / 1000.0; // gr, und, etc.
        };
        return String.format(Locale.US, "%.6f", kg);
    }

    private String adicionToKg(AdicionHervor a) {
        if (a.getCantidad() == null) return "0.000";
        String unit = a.getUnidad() != null ? a.getUnidad().toLowerCase() : "gr";
        double kg = switch (unit) {
            case "kg" -> a.getCantidad().doubleValue();
            case "l"  -> a.getCantidad().doubleValue();
            case "ml" -> a.getCantidad().doubleValue() / 1000.0;
            default   -> a.getCantidad().doubleValue() / 1000.0;
        };
        return String.format(Locale.US, "%.6f", kg);
    }

    private void setInt(Element el, String tag, Consumer<Integer> setter) {
        String val = text(el, tag);
        if (!val.isEmpty()) {
            try { setter.accept((int) Math.round(dbl(val))); } catch (Exception ignored) {}
        }
    }
}
