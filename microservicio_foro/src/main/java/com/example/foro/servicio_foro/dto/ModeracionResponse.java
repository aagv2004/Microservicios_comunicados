package com.example.foro.servicio_foro.dto;

public record ModeracionResponse(String tipo, Long id, boolean bloqueado, String motivo, Long moderadorId) {}
