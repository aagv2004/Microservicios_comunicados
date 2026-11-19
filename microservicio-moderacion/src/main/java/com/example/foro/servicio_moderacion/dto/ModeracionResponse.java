package com.example.foro.servicio_moderacion.dto;

public record ModeracionResponse(String tipo, Long id, boolean bloqueado, String motivo, Long moderadorId) {}
