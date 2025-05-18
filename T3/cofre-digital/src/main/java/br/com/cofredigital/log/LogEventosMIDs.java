package br.com.cofredigital.log;

public final class LogEventosMIDs {

    private LogEventosMIDs() {
        // Classe de constantes, não instanciável
    }

    // Sistema (1xxx)
    public static final int SISTEMA_INICIADO = 1001;
    public static final int SISTEMA_ENCERRADO = 1002;
    public static final int SESSAO_INICIADA = 1003; // Requer <login_name>
    public static final int SESSAO_ENCERRADA = 1004; // Requer <login_name>
    public static final int PARTIDA_SISTEMA_CADASTRO_ADMIN = 1005;
    public static final int PARTIDA_SISTEMA_OPERACAO_NORMAL = 1006;

    // Autenticação Etapa 1 - Identificação (2xxx)
    public static final int AUTH_ETAPA1_INICIADA = 2001;
    public static final int AUTH_ETAPA1_ENCERRADA = 2002;
    public static final int AUTH_LOGIN_LIBERADO = 2003; // Requer <login_name>
    public static final int AUTH_LOGIN_BLOQUEADO = 2004; // Requer <login_name>
    public static final int AUTH_LOGIN_NAO_IDENTIFICADO = 2005; // Requer <login_name>

    // Autenticação Etapa 2 - Senha (3xxx)
    public static final int AUTH_ETAPA2_INICIADA = 3001; // Requer <login_name>
    public static final int AUTH_ETAPA2_ENCERRADA = 3002; // Requer <login_name>
    public static final int AUTH_SENHA_OK = 3003; // Requer <login_name>
    public static final int AUTH_SENHA_ERRO1 = 3004; // Requer <login_name>
    public static final int AUTH_SENHA_ERRO2 = 3005; // Requer <login_name>
    public static final int AUTH_SENHA_ERRO3 = 3006; // Requer <login_name>
    public static final int AUTH_ACESSO_BLOQUEADO_ETAPA2 = 3007; // Requer <login_name>

    // Autenticação Etapa 3 - Token (4xxx)
    public static final int AUTH_ETAPA3_INICIADA = 4001; // Requer <login_name>
    public static final int AUTH_ETAPA3_ENCERRADA = 4002; // Requer <login_name>
    public static final int AUTH_TOKEN_OK = 4003; // Requer <login_name>
    public static final int AUTH_TOKEN_ERRO1 = 4004; // Requer <login_name>
    public static final int AUTH_TOKEN_ERRO2 = 4005; // Requer <login_name>
    public static final int AUTH_TOKEN_ERRO3 = 4006; // Requer <login_name>
    public static final int AUTH_ACESSO_BLOQUEADO_ETAPA3 = 4007; // Requer <login_name>
    // Novos MIDs para descriptografia da chave TOTP
    public static final int AUTH_ETAPA3_CHAVE_TOTP_DEC_INICIO = 4008; // Requer <login_name>
    public static final int AUTH_ETAPA3_CHAVE_TOTP_DEC_SUCESSO = 4009; // Requer <login_name>
    public static final int AUTH_ETAPA3_CHAVE_TOTP_DEC_FALHA = 4010; // Requer <login_name>

    // Tela Principal (5xxx)
    public static final int TELA_PRINCIPAL_APRESENTADA = 5001; // Requer <login_name>
    public static final int OPCAO1_MENU_PRINCIPAL = 5002; // Requer <login_name>
    public static final int OPCAO2_MENU_PRINCIPAL = 5003; // Requer <login_name>
    public static final int OPCAO3_MENU_PRINCIPAL = 5004; // Requer <login_name>

    // Tela de Cadastro (Admin - Roteiro original) ou Usuário (6xxx)
    public static final int TELA_CADASTRO_APRESENTADA = 6001; // Requer <login_name> (quem está operando)
    public static final int BOTAO_CADASTRAR_PRESSIONADO = 6002; // Requer <login_name>
    public static final int CAD_SENHA_INVALIDA = 6003; // Requer <login_name>
    public static final int CAD_CERTIFICADO_PATH_INVALIDO = 6004; // Requer <login_name>
    public static final int CAD_CHAVE_PRIVADA_PATH_INVALIDO = 6005; // Requer <login_name>
    public static final int CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA = 6006; // Requer <login_name>
    public static final int CAD_CHAVE_PRIVADA_ASSINATURA_INVALIDA = 6007; // Requer <login_name>
    public static final int CAD_CONFIRMACAO_DADOS_ACEITA = 6008; // Requer <login_name>
    public static final int CAD_CONFIRMACAO_DADOS_REJEITADA = 6009; // Requer <login_name>
    public static final int CAD_BOTAO_VOLTAR_MENU_PRINCIPAL = 6010; // Requer <login_name>
    // Adicionar MIDs específicos para cadastro de usuário se for diferente do admin (ex: 61xx)
    public static final int CAD_USUARIO_INICIO_FLUXO = 6101; // Requer email, adminUid
    public static final int CAD_USUARIO_DADOS_INVALIDOS = 6102; // Requer email, nome, gid
    public static final int CAD_EMAIL_JA_EXISTE_NOVO_USUARIO = 6103; // Requer email
    public static final int CAD_GRUPO_NAO_ENCONTRADO_NOVO_USUARIO = 6104; // Requer gid
    public static final int CAD_USUARIO_SUCESSO = 6105; // Requer uidNovoUsuario, emailNovoUsuario, gidNovoUsuario, kidPadrao, adminUid
    public static final int CAD_USUARIO_SEM_CERTIFICADO_INICIAL = 6106; // Requer email

    // MIDs para gerenciamento de Chaveiro (62xx)
    public static final int CHAVEIRO_SALVAR_INICIO = 6201; // Requer uid
    public static final int CHAVEIRO_SALVAR_SUCESSO = 6202; // Requer uid, kid
    public static final int CHAVEIRO_SALVAR_FALHA_BD = 6203; // Requer uid
    public static final int CHAVEIRO_DEFINIDO_COMO_PADRAO = 6204; // Requer uid, kid

    // MIDs para associação de KID ao Usuário (ainda na faixa 62xx ou nova 63xx)
    // Vou manter na 62xx por enquanto
    public static final int USUARIO_ASSOCIA_KID_INICIO = 6205; // Requer uid, kid
    public static final int USUARIO_ASSOCIA_KID_SUCESSO = 6206; // Requer uid, kid
    public static final int USUARIO_ASSOCIA_KID_FALHA_USUARIO_NAO_ENCONTRADO = 6207; // Requer uid tentando associar
    public static final int USUARIO_ASSOCIA_KID_FALHA_CHAVEIRO_NAO_ENCONTRADO = 6208; // Requer uid, kid que não foi encontrado
    public static final int USUARIO_ASSOCIA_KID_FALHA_BD = 6209; // Requer uid, kid, erro SQL

    // MIDs para atualização de dados do Usuário (63xx)
    public static final int USUARIO_ATUALIZAR_DADOS_INICIO = 6301; // Requer uid, email (novo)
    public static final int USUARIO_ATUALIZAR_DADOS_SUCESSO = 6302; // Requer uid, email
    public static final int USUARIO_ATUALIZAR_DADOS_FALHA_EMAIL_JA_EXISTE = 6303; // Requer uid, email_tentativa
    public static final int USUARIO_ATUALIZAR_DADOS_FALHA_USUARIO_NAO_ENCONTRADO = 6304; // Requer uid_tentativa
    public static final int USUARIO_ATUALIZAR_DADOS_FALHA_BD = 6305; // Requer uid, email, erro SQL

    // MIDs para bloqueio/desbloqueio manual de Usuário (ainda 63xx)
    public static final int USUARIO_BLOQUEIO_MANUAL_ACIONADO = 6306; // Requer uid, admin_uid (se aplicável), minutos
    public static final int USUARIO_DESBLOQUEIO_MANUAL_ACIONADO = 6307; // Requer uid, admin_uid (se aplicável)
    public static final int USUARIO_BLOQUEIO_FALHA_USUARIO_NAO_ENCONTRADO = 6308; // Requer uid_tentativa
    public static final int USUARIO_DESBLOQUEIO_FALHA_USUARIO_NAO_ENCONTRADO = 6309; // Requer uid_tentativa

    // Tela de Consulta de Arquivos Secretos (7xxx)
    public static final int TELA_CONSULTA_ARQUIVOS_APRESENTADA = 7001; // Requer <login_name>
    public static final int CONSULTA_BOTAO_VOLTAR_MENU_PRINCIPAL = 7002; // Requer <login_name>
    public static final int CONSULTA_BOTAO_LISTAR = 7003; // Requer <login_name>
    public static final int CONSULTA_CAMINHO_PASTA_INVALIDO = 7004; // Requer <login_name>
    public static final int CONSULTA_INDICE_DECRIPTADO_OK = 7005; // Requer <login_name>
    public static final int CONSULTA_INDICE_VERIFICADO_OK = 7006; // Requer <login_name>
    public static final int CONSULTA_INDICE_DECRIPTACAO_FALHA = 7007; // Requer <login_name>
    public static final int CONSULTA_INDICE_VERIFICACAO_FALHA = 7008; // Requer <login_name>
    public static final int CONSULTA_LISTA_ARQUIVOS_INDICE_APRESENTADA = 7009; // Requer <login_name>
    public static final int CONSULTA_ARQUIVO_SELECIONADO_DECRIPTAR = 7010; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ACESSO_ARQUIVO_PERMITIDO = 7011; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ACESSO_ARQUIVO_NEGADO = 7012; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ARQUIVO_DECRIPTADO_OK = 7013; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ARQUIVO_VERIFICADO_OK = 7014; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ARQUIVO_DECRIPTACAO_FALHA = 7015; // Requer <login_name>, <arq_name>
    public static final int CONSULTA_ARQUIVO_VERIFICACAO_FALHA = 7016; // Requer <login_name>, <arq_name>

    // Tela de Saída (8xxx)
    public static final int TELA_SAIDA_APRESENTADA = 8001; // Requer <login_name>
    public static final int BOTAO_ENCERRAR_SESSAO = 8002; // Requer <login_name>
    public static final int BOTAO_ENCERRAR_SISTEMA = 8003; // Requer <login_name> (usuário que clicou)
    public static final int SAIR_BOTAO_VOLTAR_MENU_PRINCIPAL = 8004; // Requer <login_name>
    
    // Outros eventos (9xxx) - Ex: Manutenção, erros inesperados não cobertos
    // public static final int ERRO_INESPERADO_SISTEMA = 9001; // Requer detalhes

} 