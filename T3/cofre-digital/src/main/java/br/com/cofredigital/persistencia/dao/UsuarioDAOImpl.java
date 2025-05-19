// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.persistencia.DatabaseManager;
import br.com.cofredigital.persistencia.modelo.Grupo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Usuario salvar(Usuario usuario, int gid) throws SQLException {
        String sql = "INSERT INTO Usuarios (nome, email, senha_hash, chave_totp_secreta, gid, kid_padrao, " +
                     "tentativas_falha_senha, tentativas_falha_token, bloqueado_ate, total_acessos, data_criacao, ultimo_acesso) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, usuario.getNome());
            pstmt.setString(2, usuario.getEmail());
            pstmt.setString(3, usuario.getSenha());
            pstmt.setString(4, usuario.getChaveSecretaTotp());
            pstmt.setInt(5, gid);
            if (usuario.getKid() != null) {
                pstmt.setInt(6, usuario.getKid());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            pstmt.setInt(7, usuario.getTentativasFalhasSenha());
            pstmt.setInt(8, usuario.getTentativasFalhasToken());
            pstmt.setTimestamp(9, usuario.getBloqueadoAte() != null ? Timestamp.valueOf(usuario.getBloqueadoAte()) : null);
            pstmt.setInt(10, usuario.getTotalAcessos());
            pstmt.setTimestamp(11, Timestamp.valueOf(usuario.getDataCriacao()));
            pstmt.setTimestamp(12, usuario.getUltimoAcesso() != null ? Timestamp.valueOf(usuario.getUltimoAcesso()) : null);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar o usuário, nenhuma linha afetada.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Falha ao salvar o usuário, nenhum UID obtido.");
                }
            }
            return usuario;
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(long uid) throws SQLException {
        String sql = "SELECT * FROM Usuarios WHERE uid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, uid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUsuario(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM Usuarios WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUsuario(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM Usuarios";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(mapRowToUsuario(rs));
            }
        }
        return usuarios;
    }

    @Override
    public void atualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE Usuarios SET nome = ?, email = ?, senha_hash = ?, chave_totp_secreta = ?, " +
                     "kid_padrao = ?, tentativas_falha_senha = ?, tentativas_falha_token = ?, bloqueado_ate = ?, " +
                     "total_acessos = ?, ultimo_acesso = ?, grupo = ? WHERE uid = ?"; 
                     // Nota: grupo (gid) também precisa ser atualizado se mudar, e o schema Usuarios não tem "grupo" mas "gid".
                     // Corrigindo a query para usar gid.
        sql = "UPDATE Usuarios SET nome = ?, email = ?, senha_hash = ?, chave_totp_secreta = ?, " +
              "gid = (SELECT gid FROM Grupos WHERE nome_grupo = ?), kid_padrao = ?, " +
              "tentativas_falha_senha = ?, tentativas_falha_token = ?, bloqueado_ate = ?, " +
              "total_acessos = ?, ultimo_acesso = ? WHERE uid = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getNome());
            pstmt.setString(2, usuario.getEmail());
            pstmt.setString(3, usuario.getSenha());
            pstmt.setString(4, usuario.getChaveSecretaTotp());
            pstmt.setString(5, usuario.getGrupo()); // nome_grupo para subselect
            if (usuario.getKid() != null) {
                pstmt.setInt(6, usuario.getKid());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            pstmt.setInt(7, usuario.getTentativasFalhasSenha());
            pstmt.setInt(8, usuario.getTentativasFalhasToken());
            pstmt.setTimestamp(9, usuario.getBloqueadoAte() != null ? Timestamp.valueOf(usuario.getBloqueadoAte()) : null);
            pstmt.setInt(10, usuario.getTotalAcessos());
            pstmt.setTimestamp(11, usuario.getUltimoAcesso() != null ? Timestamp.valueOf(usuario.getUltimoAcesso()) : null);
            pstmt.setLong(12, usuario.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Falha ao atualizar o usuário, UID não encontrado: " + usuario.getId());
            }
        }
    }

    @Override
    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT 1 FROM Usuarios WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    @Override
    public void atualizarKidPadrao(long uid, Integer kid) throws SQLException {
        String sql = "UPDATE Usuarios SET kid_padrao = ? WHERE uid = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (kid != null) {
                pstmt.setInt(1, kid);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setLong(2, uid);
            pstmt.executeUpdate();
        }
    }

    private Usuario mapRowToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("uid"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setSenha(rs.getString("senha_hash"));
        usuario.setChaveSecretaTotp(rs.getString("chave_totp_secreta"));
        
        // Para o grupo, precisamos buscar o nome do grupo usando o GID
        int gid = rs.getInt("gid");
        GrupoDAO grupoDAO = new GrupoDAOImpl(); // Poderia ser injetado para melhor testabilidade
        Optional<Grupo> grupoOpt = grupoDAO.buscarPorId(gid);
        grupoOpt.ifPresent(g -> usuario.setGrupo(g.getNomeGrupo()));
        // Se o grupo não for encontrado, usuario.getGrupo() permanecerá null, tratar conforme necessário.

        usuario.setKid(rs.getObject("kid_padrao") != null ? rs.getInt("kid_padrao") : null);
        usuario.setTentativasFalhasSenha(rs.getInt("tentativas_falha_senha"));
        usuario.setTentativasFalhasToken(rs.getInt("tentativas_falha_token"));
        Timestamp bloqueadoAteTs = rs.getTimestamp("bloqueado_ate");
        if (bloqueadoAteTs != null) {
            usuario.setBloqueadoAte(bloqueadoAteTs.toLocalDateTime());
        }
        usuario.setTotalAcessos(rs.getInt("total_acessos"));
        usuario.setDataCriacao(rs.getTimestamp("data_criacao").toLocalDateTime());
        Timestamp ultimoAcessoTs = rs.getTimestamp("ultimo_acesso");
        if (ultimoAcessoTs != null) {
            usuario.setUltimoAcesso(ultimoAcessoTs.toLocalDateTime());
        }
        return usuario;
    }
} 