package com.hospital.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reloj del sistema para el seguimiento de turnos.
 *
 * Cada 30 segundos ejecuta el motor SQL que cambia los estados según la fecha
 * y hora capturadas y genera las notificaciones de inicio/finalización. El
 * panel también lo invoca al consultar, así que los cambios se reflejan antes
 * incluso de que transcurra el intervalo.
 */
@Component
public class SeguimientoScheduler {

    private final NotificacionService notificacionService;

    public SeguimientoScheduler(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Scheduled(fixedDelayString = "${hospital.seguimiento.intervalo:30000}", initialDelay = 5000)
    public void actualizarTurnos() {
        notificacionService.sincronizar();
    }
}
