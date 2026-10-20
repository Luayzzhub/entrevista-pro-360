package pe.utp.entrevistapro360.presentation.dto;

import pe.utp.entrevistapro360.domain.Puesto;

public record PuestoResponse(Integer idPuesto, String nombrePuesto, String area,
                             String descripcion, String nivelExperiencia, boolean activo) {
    public static PuestoResponse from(Puesto puesto) {
        return new PuestoResponse(puesto.idPuesto(), puesto.nombrePuesto(), puesto.area(),
                puesto.descripcion(), puesto.nivelExperiencia(), puesto.activo());
    }
}
