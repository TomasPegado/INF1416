package br.com.cofredigital.persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import br.com.cofredigital.persistencia.dao.GrupoDAO;
import br.com.cofredigital.persistencia.dao.GrupoDAOImpl;
import br.com.cofredigital.persistencia.modelo.Grupo;
import br.com.cofredigital.persistencia.dao.MensagemDAO;
import br.com.cofredigital.persistencia.dao.MensagemDAOImpl;
import java.util.Map;
import java.util.LinkedHashMap; // Para manter a ordem de inserção, se relevante

public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:cofre_digital.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static void inicializarBanco() {
        String sqlGrupo = "CREATE TABLE IF NOT EXISTS Grupos (" +
                        "gid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nome_grupo TEXT NOT NULL UNIQUE" +
                        ");";

        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS Usuarios (" +
                           "uid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                           "nome TEXT NOT NULL, " +
                           "email TEXT NOT NULL UNIQUE, " +
                           "senha_hash TEXT NOT NULL, " +
                           "chave_totp_secreta TEXT NOT NULL, " +
                           "gid INTEGER NOT NULL, " +
                           "kid_padrao INTEGER UNIQUE, " +
                           "tentativas_falha_senha INTEGER NOT NULL DEFAULT 0, " +
                           "tentativas_falha_token INTEGER NOT NULL DEFAULT 0, " +
                           "bloqueado_ate TIMESTAMP, " +
                           "total_acessos INTEGER NOT NULL DEFAULT 0, " +
                           "data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                           "ultimo_acesso TIMESTAMP, " +
                           "FOREIGN KEY (gid) REFERENCES Grupos(gid), " +
                           "FOREIGN KEY (kid_padrao) REFERENCES Chaveiro(kid)" +
                           ");";

        String sqlChaveiro = "CREATE TABLE IF NOT EXISTS Chaveiro (" +
                             "kid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "uid INTEGER NOT NULL, " +
                             "certificado_pem TEXT NOT NULL, " +
                             "chave_privada_criptografada BLOB NOT NULL, " +
                             "FOREIGN KEY (uid) REFERENCES Usuarios(uid)" +
                             ");";

        String sqlMensagens = "CREATE TABLE IF NOT EXISTS Mensagens (" +
                              "mid INTEGER PRIMARY KEY, " +
                              "texto_mensagem TEXT NOT NULL" +
                              ");";

        String sqlRegistros = "CREATE TABLE IF NOT EXISTS Registros (" +
                              "rid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                              "data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                              "mid INTEGER NOT NULL, " +
                              "uid INTEGER, " +
                              "detalhes_adicionais TEXT, " +
                              "FOREIGN KEY (mid) REFERENCES Mensagens(mid), " +
                              "FOREIGN KEY (uid) REFERENCES Usuarios(uid)" +
                              ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlGrupo);
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlChaveiro);
            stmt.execute(sqlMensagens);
            stmt.execute(sqlRegistros);

            System.out.println("Banco de dados inicializado e tabelas verificadas/criadas.");

            // Popular dados iniciais
            popularDadosIniciais(conn); // Passar a conexão para reutilização

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace(); // Importante para debug
            // Considerar lançar uma RuntimeException aqui para parar a aplicação se o DB não puder ser inicializado.
            throw new RuntimeException("Não foi possível inicializar o banco de dados.", e);
        }
    }

    private static void popularDadosIniciais(Connection conn) throws SQLException {
        GrupoDAO grupoDAO = new GrupoDAOImpl(); // Instancia aqui para uso interno

        // Popula Grupos
        String[] nomesGrupos = {"Administrador", "Usuário"};
        for (String nomeGrupo : nomesGrupos) {
            if (grupoDAO.buscarPorNome(nomeGrupo).isEmpty()) {
                grupoDAO.salvar(new Grupo(0, nomeGrupo));
                System.out.println("Grupo '" + nomeGrupo + "' inserido.");
            }
        }

        // TODO: Popular Mensagens aqui posteriormente
        // Popula Mensagens
        MensagemDAO mensagemDAO = new MensagemDAOImpl();
        Map<Integer, String> mensagensParaSalvar = new LinkedHashMap<>();
        // Adicionar todas as mensagens do roteiro aqui
        mensagensParaSalvar.put(1001, "Sistema iniciado.");
        mensagensParaSalvar.put(1002, "Sistema encerrado.");
        mensagensParaSalvar.put(1003, "Sessão iniciada para <login_name>.");
        mensagensParaSalvar.put(1004, "Sessão encerrada para <login_name>.");
        mensagensParaSalvar.put(1005, "Partida do sistema iniciada para cadastro do administrador.");
        mensagensParaSalvar.put(1006, "Partida do sistema iniciada para operação normal pelos usuários.");
        mensagensParaSalvar.put(2001, "Autenticação etapa 1 iniciada.");
        mensagensParaSalvar.put(2002, "Autenticação etapa 1 encerrada.");
        mensagensParaSalvar.put(2003, "Login name <login_name> identificado com acesso liberado.");
        mensagensParaSalvar.put(2004, "Login name <login_name> identificado com acesso bloqueado.");
        mensagensParaSalvar.put(2005, "Login name <login_name> não identificado.");
        mensagensParaSalvar.put(3001, "Autenticação etapa 2 iniciada para <login_name>.");
        mensagensParaSalvar.put(3002, "Autenticação etapa 2 encerrada para <login_name>.");
        mensagensParaSalvar.put(3003, "Senha pessoal verificada positivamente para <login_name>.");
        mensagensParaSalvar.put(3004, "Primeiro erro da senha pessoal contabilizado para <login_name>.");
        mensagensParaSalvar.put(3005, "Segundo erro da senha pessoal contabilizado para <login_name>.");
        mensagensParaSalvar.put(3006, "Terceiro erro da senha pessoal contabilizado para <login_name>.");
        mensagensParaSalvar.put(3007, "Acesso do usuario <login_name> bloqueado pela autenticação etapa 2.");
        mensagensParaSalvar.put(4001, "Autenticação etapa 3 iniciada para <login_name>.");
        mensagensParaSalvar.put(4002, "Autenticação etapa 3 encerrada para <login_name>.");
        mensagensParaSalvar.put(4003, "Token verificado positivamente para <login_name>.");
        mensagensParaSalvar.put(4004, "Primeiro erro de token contabilizado para <login_name>.");
        mensagensParaSalvar.put(4005, "Segundo erro de token contabilizado para <login_name>.");
        mensagensParaSalvar.put(4006, "Terceiro erro de token contabilizado para <login_name>.");
        mensagensParaSalvar.put(4007, "Acesso do usuario <login_name> bloqueado pela autenticação etapa 3.");
        mensagensParaSalvar.put(5001, "Tela principal apresentada para <login_name>.");
        mensagensParaSalvar.put(5002, "Opção 1 do menu principal selecionada por <login_name>.");
        mensagensParaSalvar.put(5003, "Opção 2 do menu principal selecionada por <login_name>.");
        mensagensParaSalvar.put(5004, "Opção 3 do menu principal selecionada por <login_name>.");
        mensagensParaSalvar.put(6001, "Tela de cadastro apresentada para <login_name>.");
        mensagensParaSalvar.put(6002, "Botão cadastrar pressionado por <login_name>.");
        mensagensParaSalvar.put(6003, "Senha pessoal inválida fornecida por <login_name>.");
        mensagensParaSalvar.put(6004, "Caminho do certificado digital inválido fornecido por <login_name>.");
        mensagensParaSalvar.put(6005, "Chave privada verificada negativamente para <login_name> (caminho inválido).");
        mensagensParaSalvar.put(6006, "Chave privada verificada negativamente para <login_name> (frase secreta inválida).");
        mensagensParaSalvar.put(6007, "Chave privada verificada negativamente para <login_name> (assinatura digital inválida).");
        mensagensParaSalvar.put(6008, "Confirmação de dados aceita por <login_name>.");
        mensagensParaSalvar.put(6009, "Confirmação de dados rejeitada por <login_name>.");
        mensagensParaSalvar.put(6010, "Botão voltar de cadastro para o menu principal pressionado por <login_name>.");
        mensagensParaSalvar.put(7001, "Tela de consulta de arquivos secretos apresentada para <login_name>.");
        mensagensParaSalvar.put(7002, "Botão voltar de consulta para o menu principal pressionado por <login_name>.");
        mensagensParaSalvar.put(7003, "Botão Listar de consulta pressionado por <login_name>.");
        mensagensParaSalvar.put(7004, "Caminho de pasta inválido fornecido por <login_name>.");
        mensagensParaSalvar.put(7005, "Arquivo de índice decriptado com sucesso para <login_name>.");
        mensagensParaSalvar.put(7006, "Arquivo de índice verificado (integridade e autenticidade) com sucesso para <login_name>.");
        mensagensParaSalvar.put(7007, "Falha na decriptação do arquivo de índice para <login_name>.");
        mensagensParaSalvar.put(7008, "Falha na verificação (integridade e autenticidade) do arquivo de índice para <login_name>.");
        mensagensParaSalvar.put(7009, "Lista de arquivos presentes no índice apresentada para <login_name>.");
        mensagensParaSalvar.put(7010, "Arquivo <arq_name> selecionado por <login_name> para decriptação.");
        mensagensParaSalvar.put(7011, "Acesso permitido ao arquivo <arq_name> para <login_name>.");
        mensagensParaSalvar.put(7012, "Acesso negado ao arquivo <arq_name> para <login_name>.");
        mensagensParaSalvar.put(7013, "Arquivo <arq_name> decriptado com sucesso para <login_name>.");
        mensagensParaSalvar.put(7014, "Arquivo <arq_name> verificado (integridade e autenticidade) com sucesso para <login_name>.");
        mensagensParaSalvar.put(7015, "Falha na decriptação do arquivo <arq_name> para <login_name>.");
        mensagensParaSalvar.put(7016, "Falha na verificação (integridade e autenticidade) do arquivo <arq_name> para <login_name>.");
        mensagensParaSalvar.put(8001, "Tela de saída apresentada para <login_name>.");
        mensagensParaSalvar.put(8002, "Botão encerrar sessão pressionado por <login_name>.");
        mensagensParaSalvar.put(8003, "Botão encerrar sistema pressionado por <login_name>.");
        mensagensParaSalvar.put(8004, "Botão voltar de sair para o menu principal pressionado por <login_name>.");
        
        if (!mensagensParaSalvar.isEmpty()) {
            mensagemDAO.salvarTodas(mensagensParaSalvar);
            System.out.println(mensagensParaSalvar.size() + " mensagens padrão verificadas/inseridas.");
        }
    }

    // Método principal para teste rápido (opcional, pode ser removido depois)
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC"); // Garante que o driver está carregado
            System.out.println("Driver SQLite carregado com sucesso.");
            inicializarBanco();
            try (Connection conn = getConnection()) {
                if (conn != null) {
                    System.out.println("Conexão com o banco de dados SQLite estabelecida com sucesso.");
                } else {
                    System.err.println("Falha ao conectar com o banco de dados SQLite.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao testar conexão: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite JDBC não encontrado. Verifique as dependências do Maven.");
            e.printStackTrace();
        }
    }
} 