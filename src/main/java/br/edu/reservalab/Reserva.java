package br.edu.reservalab;

public record Reserva(long id, String equipamentoId, String responsavel, String data, String turno, String status) {}
