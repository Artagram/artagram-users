package com.artagram.users.kpi;

import com.artagram.users.common.ApiException;
import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.kpi.dto.KpiDetalleDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** HU-37: los 4 KPIs de negocio, calculados solos, con su formula y periodo. */
@RestController
@Tag(name = "Administración", description = "HU-37/38: KPIs de negocio y panel restringido a ADMIN")
public class KpiController {

    private final KpiService kpiService;
    private final JwtUserResolver userResolver;

    public KpiController(KpiService kpiService, JwtUserResolver userResolver) {
        this.kpiService = kpiService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/admin/kpis")
    public List<KpiDetalleDto> todos(HttpServletRequest request) {
        exigirAdmin(request);
        return kpiService.calcularTodos();
    }

    @GetMapping("/api/admin/kpis/{id}")
    public KpiDetalleDto detalle(HttpServletRequest request, @PathVariable String id) {
        exigirAdmin(request);
        return kpiService.calcular(id);
    }

    private void exigirAdmin(HttpServletRequest request) {
        JwtUserResolver.UsuarioAutenticado principal = userResolver.resolverRequerido(request);
        if (!"ADMIN".equals(principal.roles())) {
            throw ApiException.forbidden("SOLO_ADMIN", "Este panel es solo para administradores");
        }
    }
}
