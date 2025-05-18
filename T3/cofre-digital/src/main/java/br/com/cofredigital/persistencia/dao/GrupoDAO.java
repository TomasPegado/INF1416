package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.modelo.Grupo;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface GrupoDAO {
    /**
     * Salva um novo grupo no banco de dados.
     * Se o gid do objeto Grupo for 0 ou não definido, espera-se que o banco gere o ID.
     * O objeto Grupo retornado (ou o objeto passado, se modificado por referência)
     * deve ter o gid populado após a inserção.
     * @param grupo O objeto Grupo a ser salvo (sem gid se for novo, ou com gid para atualização - embora este método seja para salvar um novo).
     * @return O objeto Grupo salvo, com o gid populado.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Grupo salvar(Grupo grupo) throws SQLException;

    /**
     * Busca um grupo pelo seu ID (gid).
     * @param gid O ID do grupo.
     * @return Um Optional contendo o Grupo se encontrado, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Grupo> buscarPorId(int gid) throws SQLException;

    /**
     * Busca um grupo pelo seu nome.
     * @param nomeGrupo O nome do grupo.
     * @return Um Optional contendo o Grupo se encontrado, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Grupo> buscarPorNome(String nomeGrupo) throws SQLException;

    /**
     * Lista todos os grupos cadastrados.
     * @return Uma lista de todos os Grupos.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    List<Grupo> listarTodos() throws SQLException;
    
    // Poderíamos adicionar update(Grupo grupo) e delete(int gid) se necessário,
    // mas para Grupos, o cadastro inicial e consulta parecem ser o foco primário.
} 