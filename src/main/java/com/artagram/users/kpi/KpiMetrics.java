package com.artagram.users.kpi;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Republica los 4 KPIs de negocio (HU-37) como gauges de Prometheus, para
 * que un panel de negocio en Grafana los grafique con el mismo datasource
 * que el panel tecnico. Prometheus recalcula cada valor en cada scrape
 * llamando a KpiService directamente — a esta escala de datos no hace falta
 * cachear el resultado entre scrapes.
 */
@Component
public class KpiMetrics {

    public KpiMetrics(MeterRegistry registry, KpiService kpiService) {
        registrar(registry, kpiService, "tasa-colaboracion", "kpi_tasa_colaboracion",
                "Usuarios activos que entraron a un lienzo o sala de fiesta / usuarios activos, en la semana");
        registrar(registry, kpiService, "duracion-sesion", "kpi_duracion_sesion_minutos",
                "Duracion promedio de una sesion colaborativa, en minutos, en la semana");
        registrar(registry, kpiService, "retencion-7-dias", "kpi_retencion_7_dias",
                "Artistas que volvieron a entrar entre el dia 1 y el dia 7 / artistas registrados en el periodo");
        registrar(registry, kpiService, "conversion-premium", "kpi_conversion_premium",
                "Usuarios activos del mes con premium activo / usuarios activos del mes");
    }

    private void registrar(MeterRegistry registry, KpiService kpiService, String kpiId, String nombreMetrica, String descripcion) {
        Gauge.builder(nombreMetrica, kpiService, s -> s.calcular(kpiId).valorActual())
                .description(descripcion)
                .register(registry);
    }
}
