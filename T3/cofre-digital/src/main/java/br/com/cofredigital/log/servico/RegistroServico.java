// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.log.servico;

import br.com.cofredigital.persistencia.dao.RegistroDAO;
import br.com.cofredigital.persistencia.dao.RegistroDAOImpl;
import br.com.cofredigital.persistencia.modelo.Registro;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.StringJoiner;

public class RegistroServico {

    private final RegistroDAO registroDAO;

    public RegistroServico() {
        this.registroDAO = new RegistroDAOImpl();
    }

    // Construtor para injeção de dependência (melhor para testes)
    public RegistroServico(RegistroDAO registroDAO) {
        this.registroDAO = registroDAO;
    }

    /**
     * Registra um evento do sistema (sem usuário associado diretamente).
     *
     * @param mid O código da mensagem do evento.
     * @param detalhes Mapa de detalhes (chave-valor) para o evento. Pode ser nulo ou vazio.
     */
    public void registrarEventoDoSistema(int mid, Map<String, String> detalhes) {
        registrarEvento(mid, null, detalhes);
    }

    /**
     * Registra um evento associado a um usuário.
     *
     * @param mid O código da mensagem do evento.
     * @param uid O ID do usuário associado.
     * @param detalhes Mapa de detalhes (chave-valor) para o evento. Pode ser nulo ou vazio.
     */
    public void registrarEventoDoUsuario(int mid, Long uid, Map<String, String> detalhes) {
        if (uid == null) {
            // Segurança: se é um evento de usuário, UID não deveria ser nulo.
            // Considerar logar um aviso ou lançar exceção dependendo da criticidade.
            System.err.println("Aviso: Tentativa de registrar evento de usuário com UID nulo. MID: " + mid);
        }
        registrarEvento(mid, uid, detalhes);
    }

    /**
     * Método central para registrar um evento.
     *
     * @param mid O código da mensagem do evento.
     * @param uid O ID do usuário associado (pode ser nulo).
     * @param detalhes Mapa de detalhes (chave-valor) para o evento. Pode ser nulo ou vazio.
     */
    private void registrarEvento(int mid, Long uid, Map<String, String> detalhes) {
        LocalDateTime dataHoraAtual = LocalDateTime.now();
        String detalhesFormatados = formatarDetalhes(detalhes);

        Registro novoRegistro = new Registro(dataHoraAtual, mid, uid, detalhesFormatados);

        try {
            registroDAO.salvar(novoRegistro);
        } catch (SQLException e) {
            // O que fazer em caso de falha ao salvar o log?
            // Por enquanto, apenas imprimir no console. Em um sistema de produção, poderia tentar
            // um mecanismo de fallback ou logar em arquivo local de emergência.
            System.err.println("Falha crítica ao salvar registro de log no banco de dados: " + e.getMessage());
            System.err.println("Detalhes do Log não salvo: MID=" + mid + ", UID=" + uid + ", Detalhes=" + detalhesFormatados);
            // e.printStackTrace(); // Para debug mais profundo
        }
    }

    /**
     * Formata o mapa de detalhes em uma String para armazenamento.
     * Exemplo simples: "chave1=valor1, chave2=valor2"
     * Para algo mais robusto, JSON seria uma boa opção.
     * @param detalhes Mapa de detalhes.
     * @return String formatada ou null se os detalhes forem nulos ou vazios.
     */
    private String formatarDetalhes(Map<String, String> detalhes) {
        if (detalhes == null || detalhes.isEmpty()) {
            return null;
        }
        // Formato simples: chave1='valor1', chave2='valor2'
        // Para JSON, poderia usar uma biblioteca como Jackson ou Gson, mas para manter simples:
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, String> entry : detalhes.entrySet()) {
            // Simples escape de aspas simples no valor, se necessário para o formato escolhido.
            String valorFormatado = entry.getValue().replace("'", "''"); 
            joiner.add(entry.getKey() + "='" + valorFormatado + "'");
        }
        return joiner.toString();
    }

    // --- Métodos de conveniência com varargs para detalhes mais simples ---

    public void registrarEventoDoSistema(int mid, String... detalhesSimples) {
        registrarEvento(mid, null, converterDetalhesSimplesParaMapa(detalhesSimples));
    }

    public void registrarEventoDoUsuario(int mid, Long uid, String... detalhesSimples) {
        if (uid == null) {
            System.err.println("Aviso: Tentativa de registrar evento de usuário com UID nulo usando varargs. MID: " + mid);
        }
        registrarEvento(mid, uid, converterDetalhesSimplesParaMapa(detalhesSimples));
    }

    /**
     * Converte um array de strings (formato chave1, valor1, chave2, valor2...) 
     * em um Map<String, String>.
     * Se o número de strings for ímpar, o último valor é ignorado.
     */
    private Map<String, String> converterDetalhesSimplesParaMapa(String... detalhesSimples) {
        if (detalhesSimples == null || detalhesSimples.length == 0) {
            return null;
        }
        Map<String, String> mapaDetalhes = new java.util.LinkedHashMap<>(); // Mantém a ordem de inserção
        for (int i = 0; i < detalhesSimples.length - 1; i += 2) {
            mapaDetalhes.put(detalhesSimples[i], detalhesSimples[i+1]);
        }
        return mapaDetalhes;
    }
} 