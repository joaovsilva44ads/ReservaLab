package br.edu.reservalab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistência em SQLite via JDBC. O banco fica em data/reservalab.db. */
public class RepositorioSqlite implements RepositorioReservas {
    private static final String URL = "jdbc:sqlite:data/reservalab.db";

    public RepositorioSqlite() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection c = conectar(); Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS equipamentos (
                        id TEXT PRIMARY KEY,
                        nome TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        descricao TEXT NOT NULL,
                        simbolo TEXT NOT NULL
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS reservas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        equipamento_id TEXT NOT NULL REFERENCES equipamentos(id),
                        responsavel TEXT NOT NULL,
                        data TEXT NOT NULL,
                        turno TEXT NOT NULL CHECK (turno IN ('MANHA','TARDE','NOITE')),
                        status TEXT NOT NULL CHECK (status IN ('ATIVA','CANCELADA'))
                    )""");
                // Regra: no máximo uma reserva ATIVA por equipamento/data/turno.
                st.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_reserva_ativa
                    ON reservas(equipamento_id, data, turno) WHERE status = 'ATIVA'""");
            }
            popularCatalogo();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao inicializar o banco de dados", ex);
        }
    }

    private Connection conectar() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    /** INSERT OR IGNORE: só insere o que ainda não existe, sem duplicar em reinicializações. */
    private void popularCatalogo() throws SQLException {
        String[][] itens = {
            {"CAM", "Câmera DSLR", "Audiovisual", "Para gravação e fotografia", "CAM"},
            {"PROJ", "Projetor portátil", "Apresentação", "Para aulas e eventos", "PROJ"},
            {"MIC", "Microfone sem fio", "Áudio", "Para palestras e entrevistas", "MIC"},
            {"NOTE", "Notebook", "Informática", "Para oficinas e apresentações", "PC"},
            {"TAB", "Tablet gráfico", "Design", "Para desenho e prototipação", "TAB"}
        };
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT OR IGNORE INTO equipamentos(id,nome,categoria,descricao,simbolo) VALUES (?,?,?,?,?)")) {
            for (String[] i : itens) {
                for (int k = 0; k < 5; k++) ps.setString(k + 1, i[k]);
                ps.executeUpdate();
            }
        }
    }

    @Override public List<Equipamento> listarEquipamentos() {
        List<Equipamento> lista = new ArrayList<>();
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,nome,categoria,descricao,simbolo FROM equipamentos ORDER BY rowid")) {
            while (rs.next()) {
                lista.add(new Equipamento(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return lista;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao listar equipamentos", ex);
        }
    }

    @Override public List<Reserva> listarReservas() {
        List<Reserva> lista = new ArrayList<>();
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id,equipamento_id,responsavel,data,turno,status FROM reservas ORDER BY id DESC")) {
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao listar reservas", ex);
        }
    }

    @Override public Reserva criar(String equipamentoId, String responsavel, String data, String turno) {
        try (Connection c = conectar()) {
            try (PreparedStatement chk = c.prepareStatement("SELECT 1 FROM equipamentos WHERE id = ?")) {
                chk.setString(1, equipamentoId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) throw new RegraException(400, "Equipamento inválido.");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reservas(equipamento_id,responsavel,data,turno,status) VALUES (?,?,?,?,'ATIVA')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, equipamentoId);
                ps.setString(2, responsavel);
                ps.setString(3, data);
                ps.setString(4, turno);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return new Reserva(keys.getLong(1), equipamentoId, responsavel, data, turno, "ATIVA");
                }
            }
        } catch (SQLException ex) {
            // Violação do índice único parcial = período já ocupado.
            if (ex.getMessage() != null && ex.getMessage().contains("UNIQUE"))
                throw new RegraException(409, "Esse equipamento já está reservado nesse dia e turno.");
            throw new IllegalStateException("Erro ao criar reserva", ex);
        }
    }

    @Override public boolean cancelar(long id) {
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE reservas SET status = 'CANCELADA' WHERE id = ? AND status = 'ATIVA'")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao cancelar reserva", ex);
        }
    }

    private static Reserva mapear(ResultSet rs) throws SQLException {
        return new Reserva(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6));
    }
}
