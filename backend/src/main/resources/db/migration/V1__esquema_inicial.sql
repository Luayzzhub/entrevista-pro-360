-- Contrato físico del Incremento 1: 8 tablas de negocio, 68 columnas.
CREATE TABLE usuarios (
    id_usuario BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    rol ENUM('POSTULANTE','ADMIN','DOCENTE') NOT NULL DEFAULT 'POSTULANTE',
    estado ENUM('ACTIVO','INACTIVO','BLOQUEADO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro DATETIME NOT NULL,
    fecha_actualizacion DATETIME NULL,
    resumen_perfil VARCHAR(500) NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuarios_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE puestos (
    id_puesto INT NOT NULL AUTO_INCREMENT,
    nombre_puesto VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    descripcion TEXT NULL,
    nivel_experiencia VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_puesto),
    UNIQUE KEY uk_puestos_nombre (nombre_puesto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE preguntas (
    id_pregunta INT NOT NULL AUTO_INCREMENT,
    id_puesto INT NOT NULL,
    enunciado TEXT NOT NULL,
    competencia_evaluada VARCHAR(100) NOT NULL,
    orden INT NOT NULL,
    tiempo_limite_seg INT NOT NULL DEFAULT 60,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    aplica_star BOOLEAN NOT NULL,
    PRIMARY KEY (id_pregunta),
    UNIQUE KEY uk_preguntas_puesto_orden (id_puesto, orden),
    CONSTRAINT fk_preguntas_puesto FOREIGN KEY (id_puesto) REFERENCES puestos(id_puesto)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sesiones_entrevista (
    id_sesion BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    id_puesto INT NOT NULL,
    estado_sesion ENUM('INICIADA','EN_PROCESO','COMPLETADA','CANCELADA') NOT NULL DEFAULT 'INICIADA',
    puntaje_promedio DECIMAL(5,2) NULL DEFAULT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NULL,
    PRIMARY KEY (id_sesion),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_sesiones_puesto FOREIGN KEY (id_puesto) REFERENCES puestos(id_puesto)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE respuestas_video (
    id_respuesta BIGINT NOT NULL AUTO_INCREMENT,
    id_sesion BIGINT NOT NULL,
    id_pregunta INT NOT NULL,
    public_id_cloud VARCHAR(150) NOT NULL,
    duracion_segundos DECIMAL(6,3) NOT NULL,
    formato VARCHAR(20) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    estado_procesamiento ENUM('SUBIDO','AUDIO_EXTRAIDO','TRANSCRITO','EVALUADO','FALLIDO') NOT NULL DEFAULT 'SUBIDO',
    fecha_grabacion DATETIME NOT NULL,
    procesamiento_contexto JSON NOT NULL,
    PRIMARY KEY (id_respuesta),
    UNIQUE KEY uk_respuestas_sesion_pregunta (id_sesion, id_pregunta),
    CONSTRAINT fk_respuestas_sesion FOREIGN KEY (id_sesion) REFERENCES sesiones_entrevista(id_sesion)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_respuestas_pregunta FOREIGN KEY (id_pregunta) REFERENCES preguntas(id_pregunta)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transcripciones (
    id_transcripcion BIGINT NOT NULL AUTO_INCREMENT,
    id_respuesta BIGINT NOT NULL,
    texto_transcrito MEDIUMTEXT NOT NULL,
    confianza_stt DECIMAL(5,4) NULL DEFAULT NULL,
    idioma VARCHAR(10) NOT NULL,
    fecha_transcripcion DATETIME NOT NULL,
    PRIMARY KEY (id_transcripcion),
    UNIQUE KEY uk_transcripciones_respuesta (id_respuesta),
    CONSTRAINT fk_transcripciones_respuesta FOREIGN KEY (id_respuesta) REFERENCES respuestas_video(id_respuesta)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE metricas_comunicacion (
    id_metrica BIGINT NOT NULL AUTO_INCREMENT,
    id_respuesta BIGINT NOT NULL,
    conteo_palabras INT NOT NULL,
    palabras_por_minuto DECIMAL(6,2) NOT NULL,
    conteo_muletillas INT NOT NULL,
    detalle_muletillas JSON NULL,
    version_metricas VARCHAR(20) NOT NULL,
    fecha_calculo DATETIME NOT NULL,
    PRIMARY KEY (id_metrica),
    UNIQUE KEY uk_metricas_respuesta (id_respuesta),
    CONSTRAINT fk_metricas_respuesta FOREIGN KEY (id_respuesta) REFERENCES respuestas_video(id_respuesta)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evaluaciones_star (
    id_evaluacion BIGINT NOT NULL AUTO_INCREMENT,
    id_respuesta BIGINT NOT NULL,
    puntaje_general DECIMAL(5,2) NOT NULL,
    puntaje_relevancia DECIMAL(5,2) NOT NULL,
    puntaje_claridad DECIMAL(5,2) NOT NULL,
    puntaje_estructura DECIMAL(5,2) NOT NULL,
    puntaje_evidencias DECIMAL(5,2) NOT NULL,
    aplica_star BOOLEAN NOT NULL,
    detalle_star JSON NULL,
    version_rubrica VARCHAR(20) NOT NULL,
    fortaleza_principal TEXT NOT NULL,
    areas_mejora JSON NOT NULL,
    recomendaciones TEXT NOT NULL,
    fecha_evaluacion DATETIME NOT NULL,
    PRIMARY KEY (id_evaluacion),
    UNIQUE KEY uk_evaluaciones_respuesta (id_respuesta),
    CONSTRAINT fk_evaluaciones_respuesta FOREIGN KEY (id_respuesta) REFERENCES respuestas_video(id_respuesta)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
