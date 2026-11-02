
DECLARE
    v_autor NUMBER(19) := &AUTOR_DEMO_ID;
    v_total NUMBER;
    v_programacion NUMBER(19);
    v_videojuegos NUMBER(19);
    v_tecnologia NUMBER(19);
    v_cine NUMBER(19);
    v_musica NUMBER(19);
    v_tema1 NUMBER(19);
    v_tema2 NUMBER(19);
    v_tema3 NUMBER(19);
BEGIN
    IF v_autor IS NULL OR v_autor <= 0 OR v_autor != TRUNC(v_autor) THEN
        RAISE_APPLICATION_ERROR(-20001, 'El autor debe ser un ID entero positivo');
    END IF;
    SELECT (SELECT COUNT(*) FROM foro_categorias)
         + (SELECT COUNT(*) FROM foro_temas)
         + (SELECT COUNT(*) FROM foro_comentarios)
      INTO v_total FROM dual;
    IF v_total != 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Carga demo cancelada: las tablas deben estar vacias');
    END IF;

    INSERT INTO foro_categorias(nombre, nombre_normalizado, descripcion)
        VALUES ('Programacion', 'programacion', 'Lenguajes y desarrollo de software') RETURNING id INTO v_programacion;
    INSERT INTO foro_categorias(nombre, nombre_normalizado, descripcion)
        VALUES ('Videojuegos', 'videojuegos', 'Conversaciones sobre videojuegos') RETURNING id INTO v_videojuegos;
    INSERT INTO foro_categorias(nombre, nombre_normalizado, descripcion)
        VALUES ('Tecnologia', 'tecnologia', 'Herramientas y dispositivos') RETURNING id INTO v_tecnologia;
    INSERT INTO foro_categorias(nombre, nombre_normalizado, descripcion)
        VALUES ('Cine', 'cine', 'Peliculas y recomendaciones') RETURNING id INTO v_cine;
    INSERT INTO foro_categorias(nombre, nombre_normalizado, descripcion)
        VALUES ('Musica', 'musica', 'Artistas y generos musicales') RETURNING id INTO v_musica;

    INSERT INTO foro_temas(titulo, contenido, categoria_id, autor_id)
        VALUES ('Organizar un proyecto Java', TO_CLOB('Tema de demostracion sobre separacion de responsabilidades.'), v_programacion, v_autor)
        RETURNING id INTO v_tema1;
    INSERT INTO foro_temas(titulo, contenido, categoria_id, autor_id)
        VALUES ('Juegos cooperativos', TO_CLOB('Tema de demostracion para compartir experiencias de juego.'), v_videojuegos, v_autor)
        RETURNING id INTO v_tema2;
    INSERT INTO foro_temas(titulo, contenido, categoria_id, autor_id)
        VALUES ('Herramientas de estudio', TO_CLOB('Tema de demostracion sobre herramientas para aprender.'), v_tecnologia, v_autor)
        RETURNING id INTO v_tema3;

    INSERT INTO foro_comentarios(contenido, tema_id, autor_id)
        VALUES (TO_CLOB('Separar controlador, servicio y repositorio ayuda a entender el flujo.'), v_tema1, v_autor);
    INSERT INTO foro_comentarios(contenido, tema_id, autor_id)
        VALUES (TO_CLOB('Podemos compartir opciones para jugar en equipo.'), v_tema2, v_autor);
    INSERT INTO foro_comentarios(contenido, tema_id, autor_id)
        VALUES (TO_CLOB('Practicar con ejemplos pequenos facilita el aprendizaje.'), v_tema3, v_autor);
END;
/
COMMIT;

SELECT id, nombre FROM foro_categorias ORDER BY id;
SELECT id, titulo, categoria_id, autor_id FROM foro_temas ORDER BY id;
SELECT id, tema_id, autor_id FROM foro_comentarios ORDER BY id;
