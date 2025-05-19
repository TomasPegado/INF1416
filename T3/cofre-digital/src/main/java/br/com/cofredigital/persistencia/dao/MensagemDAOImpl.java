// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.DatabaseManager;
import br.com.cofredigital.persistencia.modelo.Mensagem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MensagemDAOImpl implements MensagemDAO {

    @Override
    public void salvar(Mensagem mensagem) throws SQLException {
        String sql = "INSERT INTO Mensagens (mid, texto_mensagem) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, mensagem.getMid());
            pstmt.setString(2, mensagem.getTextoMensagem());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void salvarTodas(Map<Integer, String> mensagens) throws SQLException {
        String sql = "INSERT INTO Mensagens (mid, texto_mensagem) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // Inicia transação
            for (Map.Entry<Integer, String> entry : mensagens.entrySet()) {
                // Verifica se a mensagem já existe para evitar erro de chave primária
                if (buscarPorId(entry.getKey()).isEmpty()) {
                    pstmt.setInt(1, entry.getKey());
                    pstmt.setString(2, entry.getValue());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit(); // Finaliza transação
        } catch (SQLException e) {
            // Em caso de erro, tentar rollback se a conexão ainda estiver ativa
            // A complexidade do rollback depende se você quer continuar após um erro parcial
            // ou falhar tudo. Para popular dados, falhar tudo pode ser mais simples.
            throw e;
        } finally {
            // Idealmente, a conexão seria resetada para autocommit=true aqui,
            // mas o try-with-resources fecha a conexão, então não é estritamente necessário
            // para este escopo limitado.
        }
    }

    @Override
    public Optional<Mensagem> buscarPorId(int mid) throws SQLException {
        String sql = "SELECT mid, texto_mensagem FROM Mensagens WHERE mid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, mid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Mensagem(rs.getInt("mid"), rs.getString("texto_mensagem")));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Mensagem> listarTodas() throws SQLException {
        List<Mensagem> mensagens = new ArrayList<>();
        String sql = "SELECT mid, texto_mensagem FROM Mensagens";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                mensagens.add(new Mensagem(rs.getInt("mid"), rs.getString("texto_mensagem")));
            }
        }
        return mensagens;
    }
} 