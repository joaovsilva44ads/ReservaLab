package br.edu.reservalab;

import java.util.ArrayList;
import java.util.List;

/** Versão temporária: os dados desaparecem quando o servidor é reiniciado. */
public class RepositorioMemoria implements RepositorioReservas {
    private static final List<Equipamento> EQUIPAMENTOS = List.of(
        new Equipamento("CAM", "Câmera DSLR", "Audiovisual", "Para gravação e fotografia", "CAM"),
        new Equipamento("PROJ", "Projetor portátil", "Apresentação", "Para aulas e eventos", "PROJ"),
        new Equipamento("MIC", "Microfone sem fio", "Áudio", "Para palestras e entrevistas", "MIC"),
        new Equipamento("NOTE", "Notebook", "Informática", "Para oficinas e apresentações", "PC"),
        new Equipamento("TAB", "Tablet gráfico", "Design", "Para desenho e prototipação", "TAB")
    );
    private final List<Reserva> reservas = new ArrayList<>();
    private long proximoId = 1;
    @Override public List<Equipamento> listarEquipamentos() { return EQUIPAMENTOS; }
    @Override public synchronized List<Reserva> listarReservas() { return reservas.stream().sorted((a,b) -> Long.compare(b.id(), a.id())).toList(); }
    @Override public synchronized Reserva criar(String equipamentoId, String responsavel, String data, String turno) {
        boolean ocupado = reservas.stream().anyMatch(r -> r.status().equals("ATIVA") &&
            r.equipamentoId().equals(equipamentoId) && r.data().equals(data) && r.turno().equals(turno));
        if (ocupado) throw new RegraException(409, "Esse equipamento já está reservado nesse dia e turno.");
        Reserva nova = new Reserva(proximoId++, equipamentoId, responsavel, data, turno, "ATIVA");
        reservas.add(nova);
        return nova;
    }
    @Override public synchronized boolean cancelar(long id) {
        for (int i=0; i<reservas.size(); i++) {
            Reserva r = reservas.get(i);
            if (r.id()==id && r.status().equals("ATIVA")) {
                reservas.set(i, new Reserva(r.id(), r.equipamentoId(), r.responsavel(), r.data(), r.turno(), "CANCELADA"));
                return true;
            }
        }
        return false;
    }
}
