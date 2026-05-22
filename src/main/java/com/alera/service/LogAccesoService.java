package com.alera.service;

import com.alera.model.LogAcceso;
import com.alera.repository.LogAccesoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class LogAccesoService {

    private static final Logger log = LoggerFactory.getLogger(LogAccesoService.class);
    @Value("${app.log-page-size:25}")
    private int pageSize;

    private final LogAccesoRepository repo;

    public LogAccesoService(LogAccesoRepository repo) {
        this.repo = repo;
    }

    // REQUIRES_NEW para que el log se guarde aunque haya un rollback en la tx principal
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String tipo, String ip,
                          String url, String userAgent, String detalles) {
        repo.save(LogAcceso.of(usuario, tipo, ip, url, userAgent, detalles));
        if ("LOGIN_FALLIDO".equals(tipo)) {
            log.warn("[SEGURIDAD] Login fallido — usuario='{}' ip={}", usuario, ip);
        } else if ("ACCESO_DENEGADO".equals(tipo)) {
            log.warn("[SEGURIDAD] Acceso denegado — usuario='{}' url={} ip={}", usuario, url, ip);
        }
    }

    @Transactional(readOnly = true)
    public Page<LogAcceso> listarPaginado(String tipo, int page) {
        var pageable = PageRequest.of(page, pageSize);
        if (tipo != null && !tipo.isBlank()) {
            return repo.findByTipoOrderByFechaDesc(tipo, pageable);
        }
        return repo.findAllByOrderByFechaDesc(pageable);
    }

    @Transactional(readOnly = true)
    public long fallidosUltimaHora() {
        return repo.countFallidosDesde(LocalDateTime.now().minusHours(1));
    }
}
