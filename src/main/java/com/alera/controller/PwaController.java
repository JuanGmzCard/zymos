package com.alera.controller;

import com.alera.config.TenantFilter;
import com.alera.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PwaController {

    @GetMapping(value = "/manifest.webmanifest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> manifest(HttpServletRequest request) {
        String name       = "Zymos";
        String themeColor = "#1e293b";

        Object attr = request.getAttribute(TenantFilter.TENANT_ATTR);
        if (attr instanceof Tenant t) {
            if (t.getName()       != null) name       = t.getName();
            if (t.getColorNavbar() != null) themeColor = t.getColorNavbar();
        }

        String shortName = name.length() > 12 ? name.substring(0, 12) : name;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name",             name);
        body.put("short_name",       shortName);
        body.put("description",      "Sistema de Gestión de Trazabilidad de Cerveza Artesanal");
        body.put("start_url",        "/");
        body.put("display",          "standalone");
        body.put("orientation",      "portrait-primary");
        body.put("theme_color",      themeColor);
        body.put("background_color", themeColor);
        body.put("icons", List.of(
            Map.of("src", "/img/icon.svg",     "sizes", "any",     "type", "image/svg+xml",
                   "purpose", "any"),
            Map.of("src", "/img/icon-192.png", "sizes", "192x192", "type", "image/png",
                   "purpose", "any maskable"),
            Map.of("src", "/img/icon-512.png", "sizes", "512x512", "type", "image/png",
                   "purpose", "any maskable")
        ));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/manifest+json"))
                .body(body);
    }
}
