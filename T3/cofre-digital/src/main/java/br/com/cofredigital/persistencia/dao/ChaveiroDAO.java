package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.modelo.Chaveiro;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ChaveiroDAO {
    /**
     * Salva um novo par de chave/certificado no chaveiro.
     * O objeto Chaveiro retornado deve ter o kid populado.
     * @param chaveiro O objeto Chaveiro a ser salvo.
     * @return O objeto Chaveiro salvo, com o kid populado.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Chaveiro salvar(Chaveiro chaveiro) throws SQLException;

    /**
     * Busca um item do chaveiro pelo seu ID (kid).
     * @param kid O ID do item no chaveiro.
     * @return Um Optional contendo o Chaveiro se encontrado, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Chaveiro> buscarPorKid(int kid) throws SQLException;

    /**
     * Busca todos os itens do chaveiro associados a um UID de usuário.
     * @param uid O UID do usuário.
     * @return Uma lista de Chaveiro pertencentes ao usuário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    List<Chaveiro> buscarPorUid(long uid) throws SQLException;

    // Métodos para update e delete podem ser adicionados se necessário.
} 