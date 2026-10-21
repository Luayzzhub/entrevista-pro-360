package pe.utp.entrevistapro360.application;

import pe.utp.entrevistapro360.domain.Puesto;
import pe.utp.entrevistapro360.domain.SesionEntrevista;

public record ResumenSesionItem(SesionEntrevista sesion, Puesto puesto, int respuestasConfirmadas,
                                int respuestasEvaluadas, Integer ordenPreguntaHabilitada) { }
