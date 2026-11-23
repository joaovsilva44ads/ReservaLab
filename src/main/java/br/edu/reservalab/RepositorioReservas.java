package br.edu.reservalab;

import java.util.List;

public interface RepositorioReservas {
    List<Equipamento> listarEquipamentos();
    List<Reserva> listarReservas();
    Reserva criar(String equipamentoId, String responsavel, String data, String turno);
    boolean cancelar(long id);
}
