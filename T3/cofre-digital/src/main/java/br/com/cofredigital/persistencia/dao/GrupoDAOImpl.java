package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.DatabaseManager;
import br.com.cofredigital.persistencia.modelo.Grupo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GrupoDAOImpl implements GrupoDAO {

    @Override
    public Grupo salvar(Grupo grupo) throws SQLException {
        String sql = "INSERT INTO Grupos (nome_grupo) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, grupo.getNomeGrupo());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar o grupo, nenhuma linha afetada.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    grupo.setGid(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Falha ao salvar o grupo, nenhum ID obtido.");
                }
            }
            return grupo;
        } catch (SQLException e) {
            // Adicionar um log mais específico para constraint violation (nome_grupo UNIQUE)
            if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: Grupos.nome_grupo")) {
                 System.err.println("Tentativa de inserir nome de grupo duplicado: " + grupo.getNomeGrupo());
                 // Lançar uma exceção mais específica ou tratar de acordo
            }
            throw e; // Relança a exceção original ou uma nova mais específica
        }
    }

    @Override
    public Optional<Grupo> buscarPorId(int gid) throws SQLException {
        String sql = "SELECT gid, nome_grupo FROM Grupos WHERE gid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Grupo(rs.getInt("gid"), rs.getString("nome_grupo")));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Grupo> buscarPorNome(String nomeGrupo) throws SQLException {
        String sql = "SELECT gid, nome_grupo FROM Grupos WHERE nome_grupo = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeGrupo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Grupo(rs.getInt("gid"), rs.getString("nome_grupo")));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Grupo> listarTodos() throws SQLException {
        List<Grupo> grupos = new ArrayList<>();
        String sql = "SELECT gid, nome_grupo FROM Grupos";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                grupos.add(new Grupo(rs.getInt("gid"), rs.getString("nome_grupo")));
            }
        }
        return grupos;
    }
} 