package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UsuarioDAO {
    /**
     * Salva um novo usuário no banco de dados.
     * O objeto Usuario retornado deve ter o uid populado.
     * @param usuario O objeto Usuario a ser salvo.
     * @param gid O ID do grupo ao qual o usuário pertence.
     * @return O objeto Usuario salvo, com o uid populado.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Usuario salvar(Usuario usuario, int gid) throws SQLException;

    /**
     * Busca um usuário pelo seu ID (uid).
     * @param uid O ID do usuário.
     * @return Um Optional contendo o Usuario se encontrado, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Usuario> buscarPorId(long uid) throws SQLException;

    /**
     * Busca um usuário pelo seu email.
     * @param email O email do usuário.
     * @return Um Optional contendo o Usuario se encontrado, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Usuario> buscarPorEmail(String email) throws SQLException;

    /**
     * Lista todos os usuários cadastrados.
     * @return Uma lista de todos os Usuarios.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    List<Usuario> listarTodos() throws SQLException;

    /**
     * Atualiza os dados de um usuário existente no banco de dados.
     * @param usuario O objeto Usuario com os dados atualizados.
     * @throws SQLException Se ocorrer um erro no banco de dados ou se o usuário não for encontrado.
     */
    void atualizar(Usuario usuario) throws SQLException;

    /**
     * Verifica se um email já existe no banco de dados.
     * @param email O email a ser verificado.
     * @return true se o email existir, false caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    boolean emailExiste(String email) throws SQLException;
    
    /**
     * Atualiza o KID padrão de um usuário.
     * @param uid O ID do usuário.
     * @param kid O novo KID padrão.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    void atualizarKidPadrao(long uid, Integer kid) throws SQLException;
} 