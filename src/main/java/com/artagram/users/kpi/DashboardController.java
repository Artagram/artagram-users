package com.artagram.users.kpi;

import com.artagram.users.common.ApiException;
import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.kpi.dto.DashboardInfoDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-38: un solo lugar para las metricas tecnicas y los KPIs. Este endpoint
 * solo entrega las URLs (Grafana vive aparte); lo importante para la
 * historia es que solo un ADMIN llega hasta aqui y que el intento fallido
 * queda registrado.
 */
@RestController
@Tag(name = "Administración", description = "HU-37/38: KPIs de negocio y panel restringido a ADMIN")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final JwtUserResolver userResolver;

    @Value("${app.observability.grafana-tecnico-url:http://localhost:3001/d/artagram-tecnico}")
    private String panelTecnicoUrl;

    @Value("${app.observability.grafana-negocio-url:http://localhost:3001/d/artagram-negocio}")
    private String panelNegocioUrl;

    @Value("${app.observability.prometheus-url:http://localhost:9090}")
    private String prometheusUrl;

    public DashboardController(JwtUserResolver userResolver) {
        this.userResolver = userResolver;
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardInfoDto obtener(HttpServletRequest request) {
        JwtUserResolver.UsuarioAutenticado principal = userResolver.resolverRequerido(request);
        if (!"ADMIN".equals(principal.roles())) {
            log.warn("Intento de acceso al dashboard denegado a usuario={}", principal.id());
            throw ApiException.forbidden("SOLO_ADMIN", "Este panel es solo para administradores");
        }
        log.info("Acceso al dashboard concedido a usuario={}", principal.id());
        return new DashboardInfoDto(panelTecnicoUrl, panelNegocioUrl, prometheusUrl);
    }
}
