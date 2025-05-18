package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.modelo.Mensagem;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MensagemDAO {
    /**
     * Salva uma nova mensagem no banco de dados.
     * O mid é fornecido, pois não é autoincrementável.
     * @param mensagem O objeto Mensagem a ser salvo.
     * @throws SQLException Se ocorrer um erro no banco de dados, especialmente violação de chave primária (mid).
     */
    void salvar(Mensagem mensagem) throws SQLException;

    /**
     * Salva uma lista de mensagens no banco de dados.
     * Útil para popular dados iniciais.
     * @param mensagens Mapa de MID para texto da mensagem.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    void salvarTodas(Map<Integer, String> mensagens) throws SQLException;

    /**
     * Busca uma mensagem pelo seu ID (mid).
     * @param mid O ID da mensagem.
     * @return Um Optional contendo a Mensagem se encontrada, ou Optional.empty() caso contrário.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    Optional<Mensagem> buscarPorId(int mid) throws SQLException;

    /**
     * Lista todas as mensagens cadastradas.
     * @return Uma lista de todas as Mensagens.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    List<Mensagem> listarTodas() throws SQLException;
} 