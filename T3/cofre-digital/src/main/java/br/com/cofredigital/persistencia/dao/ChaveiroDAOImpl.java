// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.DatabaseManager;
import br.com.cofredigital.persistencia.modelo.Chaveiro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChaveiroDAOImpl implements ChaveiroDAO {

    @Override
    public Chaveiro salvar(Chaveiro chaveiro) throws SQLException {
        String sql = "INSERT INTO Chaveiro (uid, certificado_pem, chave_privada_criptografada) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, chaveiro.getUid());
            pstmt.setString(2, chaveiro.getCertificadoPem());
            pstmt.setBytes(3, chaveiro.getChavePrivadaCriptografada());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar o item no chaveiro, nenhuma linha afetada.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    chaveiro.setKid(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Falha ao salvar o item no chaveiro, nenhum KID obtido.");
                }
            }
            return chaveiro;
        }
    }

    @Override
    public Optional<Chaveiro> buscarPorKid(int kid) throws SQLException {
        String sql = "SELECT kid, uid, certificado_pem, chave_privada_criptografada FROM Chaveiro WHERE kid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, kid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Chaveiro(
                            rs.getInt("kid"),
                            rs.getLong("uid"),
                            rs.getString("certificado_pem"),
                            rs.getBytes("chave_privada_criptografada")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Chaveiro> buscarPorUid(long uid) throws SQLException {
        List<Chaveiro> chaves = new ArrayList<>();
        String sql = "SELECT kid, uid, certificado_pem, chave_privada_criptografada FROM Chaveiro WHERE uid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, uid);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    chaves.add(new Chaveiro(
                            rs.getInt("kid"),
                            rs.getLong("uid"),
                            rs.getString("certificado_pem"),
                            rs.getBytes("chave_privada_criptografada")
                    ));
                }
            }
        }
        return chaves;
    }
} 