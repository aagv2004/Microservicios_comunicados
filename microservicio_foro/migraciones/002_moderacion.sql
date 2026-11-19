-- Ejecutar una sola vez con el usuario propietario de las tablas de Foro.
-- Agrega estado de moderacion sin borrar temas ni comentarios existentes.
ALTER TABLE foro_temas ADD (
    bloqueado NUMBER(1) DEFAULT 0 NOT NULL,
    motivo_bloqueo VARCHAR2(500 CHAR),
    moderador_id NUMBER(19),
    CONSTRAINT ck_tema_bloqueado CHECK (bloqueado IN (0, 1))
);
ALTER TABLE foro_comentarios ADD (
    bloqueado NUMBER(1) DEFAULT 0 NOT NULL,
    motivo_bloqueo VARCHAR2(500 CHAR),
    moderador_id NUMBER(19),
    CONSTRAINT ck_comentario_bloqueado CHECK (bloqueado IN (0, 1))
);
