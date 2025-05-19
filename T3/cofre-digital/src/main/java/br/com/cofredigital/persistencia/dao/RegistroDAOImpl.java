package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.DatabaseManager;
import br.com.cofredigital.persistencia.modelo.Registro;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class RegistroDAOImpl implements RegistroDAO {

    @Override
    public void salvar(Registro registro) throws SQLException {
        String sql = "INSERT INTO Registros (data_hora, mid, uid, detalhes_adicionais) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (registro.getDataHora() != null) {
                pstmt.setTimestamp(1, Timestamp.valueOf(registro.getDataHora()));
            } else {
                // Definir como CURRENT_TIMESTAMP se não for fornecido, embora o BD já tenha default
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now())); 
            }
            
            pstmt.setInt(2, registro.getMid());
            
            if (registro.getUid() != null) {
                pstmt.setLong(3, registro.getUid());
            } else {
                pstmt.setNull(3, Types.BIGINT); // Ou Types.INTEGER se UID for INTEGER no BD
            }
            
            if (registro.getDetalhesAdicionais() != null) {
                pstmt.setString(4, registro.getDetalhesAdicionais());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar o registro, nenhuma linha afetada.");
            }

            // Opcional: Obter o RID gerado e definir no objeto Registro, se necessário para uso imediato.
            // Como Registros.rid é AUTOINCREMENT, o BD irá gerá-lo.
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    registro.setRid(generatedKeys.getLong(1));
                } else {
                    // Não é uma falha crítica se o ID não for retornado, a menos que seja usado imediatamente.
                    // System.err.println("Falha ao obter o RID gerado para o registro.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar registro no banco de dados: " + e.getMessage());
            // e.printStackTrace(); // Descomentar para debug
            throw e; // Relança a exceção para ser tratada pela camada de serviço
        }
    }

    @Override
    public List<Registro> listarTodos() throws SQLException {
        String sql = "SELECT * FROM Registros ORDER BY data_hora ASC";
        List<Registro> registros = new java.util.ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Registro reg = new Registro();
                reg.setRid(rs.getLong("rid"));
                reg.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                reg.setMid(rs.getInt("mid"));
                long uid = rs.getLong("uid");
                reg.setUid(rs.wasNull() ? null : uid);
                reg.setDetalhesAdicionais(rs.getString("detalhes_adicionais"));
                registros.add(reg);
            }
        }
        return registros;
    }

    // Implementações para listarTodos, buscarPorUid, etc., podem ser adicionadas aqui no futuro.
} 