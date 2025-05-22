// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import br.com.cofredigital.crypto.PasswordUtil;
import br.com.cofredigital.crypto.AESUtil;
import br.com.cofredigital.crypto.CertificateUtil;
import br.com.cofredigital.crypto.PrivateKeyUtil;
import br.com.cofredigital.persistencia.dao.UsuarioDAO;
import br.com.cofredigital.persistencia.dao.UsuarioDAOImpl;
import br.com.cofredigital.persistencia.dao.GrupoDAO;
import br.com.cofredigital.persistencia.dao.GrupoDAOImpl;
import br.com.cofredigital.persistencia.dao.ChaveiroDAO;
import br.com.cofredigital.persistencia.dao.ChaveiroDAOImpl;
import br.com.cofredigital.persistencia.modelo.Chaveiro;
import br.com.cofredigital.persistencia.modelo.Grupo;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.util.StringUtil;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import java.sql.SQLException;
import java.util.Map;
import java.time.LocalDateTime;
import java.security.GeneralSecurityException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS8EncryptedPrivateKeyInfoBuilder;

public class UsuarioServico {

    private final UsuarioDAO usuarioDAO;
    private final GrupoDAO grupoDAO;
    private final ChaveiroDAO chaveiroDAO;
    private final TotpServico totpServico;
    private final RegistroServico registroServico;
    private static final int MAX_TENTATIVAS_SENHA = 3; // Exemplo, ajuste conforme necessário
    private static final int MINUTOS_BLOQUEIO_SENHA = 2; // Exemplo

    private String adminPassphraseSession = null;

    public UsuarioServico(TotpServico totpServico, RegistroServico registroServico) {
        this.totpServico = totpServico;
        this.usuarioDAO = new UsuarioDAOImpl();
        this.grupoDAO = new GrupoDAOImpl();
        this.chaveiroDAO = new ChaveiroDAOImpl();
        this.registroServico = registroServico;
    }

    public Usuario cadastrarUsuario(Usuario usuario, String senhaOriginal, String nomeGrupo) throws Exception {
        if (usuarioDAO.emailExiste(usuario.getEmail())) {
            throw new EmailJaExisteException(usuario.getEmail());
        }
        
        usuario.setSenha(PasswordUtil.hashPassword(senhaOriginal));
        
        String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
        SecretKey chaveAES = AESUtil.generateKeyFromSecret(senhaOriginal, 256);
        byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), chaveAES);
        usuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
        
        Optional<Grupo> grupoOpt = grupoDAO.buscarPorNome(nomeGrupo);
        if (grupoOpt.isEmpty()) {
            throw new Exception("Grupo '" + nomeGrupo + "' não encontrado no banco de dados.");
        }
        int gid = grupoOpt.get().getGid();

        Usuario usuarioSalvo = usuarioDAO.salvar(usuario, gid);
        // O UID do usuário é definido pelo banco. O KID será associado ao salvar o Chaveiro, se aplicável.

        System.out.println("[UsuarioServico] Usuário pré-cadastrado no BD: " + usuarioSalvo.getEmail() + " com UID: " + usuarioSalvo.getId() + " e chave TOTP (plana): " + chaveSecretaTotpBase32);
        return usuarioSalvo;
    }

    public Usuario buscarPorId(Long id) {
        try {
            return usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao buscar usuário por ID: " + id, e);
        }
    }

    public Usuario buscarPorEmail(String email) {
        try {
            return usuarioDAO.buscarPorEmail(email).orElse(null);
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao buscar usuário por email: " + email, e);
        }
    }
    
    public boolean existsByEmail(String email) {
        try {
            return usuarioDAO.emailExiste(email);
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao verificar existência de email: " + email, e);
        }
    }

    public List<Usuario> listarTodos() {
        try {
            return usuarioDAO.listarTodos();
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao listar todos os usuários.", e);
        }
    }

    public Usuario atualizar(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            // Log de erro interno, dados inválidos para atualização
            // // registroServico.registrarEventoDoSistema(MID_ATUALIZAR_USUARIO_DADOS_ENTRADA_INVALIDOS);
            throw new IllegalArgumentException("Dados do usuário inválidos para atualização.");
        }
        Long uid = usuario.getId();
        String emailNovo = usuario.getEmail();
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ATUALIZAR_DADOS_INICIO, uid, "uid", String.valueOf(uid), "email_novo", emailNovo);

        try {
            Usuario usuarioAntes = buscarPorId(uid); // Garante que o usuário existe e para pegar email antigo
            if (!usuarioAntes.getEmail().equals(emailNovo) && existsByEmail(emailNovo)) {
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ATUALIZAR_DADOS_FALHA_EMAIL_JA_EXISTE, uid, "uid", String.valueOf(uid), "email_tentativa", emailNovo);
                throw new EmailJaExisteException("Email já cadastrado para outro usuário: " + emailNovo);
            }
            // Aqui podem ser logadas as alterações específicas, se necessário, comparando usuarioAntes e usuario.
            // Ex: if (!usuarioAntes.getNome().equals(usuario.getNome())) { // registroServico.logAlteracao(...); }
            
            usuarioDAO.atualizar(usuario);
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ATUALIZAR_DADOS_SUCESSO, uid, "uid", String.valueOf(uid), "email_atualizado", emailNovo);
            return usuario; 
        } catch (SQLException e) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ATUALIZAR_DADOS_FALHA_BD, uid, "uid", String.valueOf(uid), "email_tentativa", emailNovo, "erro_sql", e.getMessage());
            throw new RuntimeException("Erro de banco de dados ao atualizar usuário: " + emailNovo, e);
        } catch (UsuarioNaoEncontradoException e) { 
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.USUARIO_ATUALIZAR_DADOS_FALHA_USUARIO_NAO_ENCONTRADO, "uid_tentativa", String.valueOf(uid));
            throw e; // Relança a exceção original
        }
    }
    
    public void bloquearUsuario(Long id, int minutos) {
        // Nota: adminUid pode ser um parâmetro adicional se quisermos logar quem realizou o bloqueio manual.
        // Por agora, o log indicará o id do usuário afetado.
        try {
            Usuario usuario = buscarPorId(id); // Lança exceção se não encontrado, que será capturada abaixo
            usuario.bloquearAcessoPorMinutos(minutos);
            atualizar(usuario); // Persiste a alteração. O método atualizar() já tem seus próprios logs.
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_BLOQUEIO_MANUAL_ACIONADO, id, "uid_bloqueado", String.valueOf(id), "minutos", String.valueOf(minutos));
        } catch (UsuarioNaoEncontradoException e) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.USUARIO_BLOQUEIO_FALHA_USUARIO_NAO_ENCONTRADO, "uid_tentativa_bloqueio", String.valueOf(id));
            throw e; // Relança para a camada chamadora
        }
        // Outras exceções de 'atualizar()' serão propagadas e logadas por 'atualizar()'.
    }

    public void desbloquearUsuario(Long id) {
        // Similar a bloquearUsuario, adminUid poderia ser um parâmetro.
        try {
            Usuario usuario = buscarPorId(id); // Lança exceção se não encontrado
            usuario.desbloquearAcesso();
            atualizar(usuario); // Persiste a alteração. O método atualizar() já tem seus próprios logs.
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_DESBLOQUEIO_MANUAL_ACIONADO, id, "uid_desbloqueado", String.valueOf(id));
        } catch (UsuarioNaoEncontradoException e) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.USUARIO_DESBLOQUEIO_FALHA_USUARIO_NAO_ENCONTRADO, "uid_tentativa_desbloqueio", String.valueOf(id));
            throw e; // Relança para a camada chamadora
        }
        // Outras exceções de 'atualizar()' serão propagadas e logadas por 'atualizar()'.
    }

    public String obterChaveTotpDescriptografada(Usuario usuario, String senhaCandidata) throws Exception {
        if (usuario == null) {
            // Não deveria acontecer se chamado corretamente, mas é uma guarda.
            // // // registroServico.registrarEventoDoSistema(MID_INTERNO_USUARIO_NULO_OBTER_TOTP);
            throw new IllegalArgumentException("Usuário não pode ser nulo para obter chave TOTP.");
        }
        Long uid = usuario.getId();
        String email = usuario.getEmail();

        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CHAVE_TOTP_DEC_INICIO, uid, "email", email);

        try {
            SecretKey chaveAES = AESUtil.generateKeyFromSecret(senhaCandidata, 256);
            byte[] chaveTotpCriptografada = Base64.getDecoder().decode(usuario.getChaveSecretaTotp());
            byte[] chaveTotpBytes = AESUtil.decrypt(chaveTotpCriptografada, chaveAES);
            String chavePlana = new String(chaveTotpBytes, StandardCharsets.UTF_8);
            System.out.println("[UsuarioServico.obterChaveTotpDescriptografada] Chave TOTP descriptografada para " + email + ": " + chavePlana); // DEBUG
            
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CHAVE_TOTP_DEC_SUCESSO, uid, "email", email);
            return chavePlana;
        } catch (Exception e) {
            // A exceção aqui é provavelmente javax.crypto.BadPaddingException se a senha estiver errada
            // ou qualquer outra exceção de AESUtil.decrypt.
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CHAVE_TOTP_DEC_FALHA, uid, "email", email, "erro", e.getClass().getSimpleName());
            // Relança a exceção para que a camada chamadora saiba que a descriptografia falhou.
            // A camada chamadora (provavelmente um serviço de autenticação de mais alto nível ou UI)
            // decidirá se isso conta como uma tentativa de token falha.
            throw new Exception("Falha ao descriptografar a chave TOTP para o usuário " + email + ". A senha fornecida pode estar incorreta.", e);
        }
    }

    public Usuario validarIdentificacaoUsuario(String email) throws UsuarioNaoEncontradoException, IllegalStateException {
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_INICIADA, null, "email_tentativa", email); // UID será adicionado depois se o usuário for encontrado

        if (email == null || email.trim().isEmpty()) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, null, "email_tentativa", email, "resultado", "entrada_invalida");
            throw new IllegalArgumentException("Email não pode ser vazio.");
        }

        Usuario usuario;
        try {
            usuario = usuarioDAO.buscarPorEmail(email)
                .orElseThrow(() -> {
                    // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_LOGIN_NAO_IDENTIFICADO, null, "email_tentativa", email);
                    // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, null, "email_tentativa", email, "resultado", "usuario_nao_encontrado");
                    return new UsuarioNaoEncontradoException("Usuário não encontrado para o email: " + email);
                });
        } catch (SQLException e) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, null, "email_tentativa", email, "resultado", "erro_bd", "detalhe", e.getMessage());
            throw new RuntimeException("Erro de banco de dados ao buscar usuário por email: " + email, e);
        }
        
        Long uidUsuario = usuario.getId(); // UID disponível para logs subsequentes

        if (usuario.isAcessoBloqueado()) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_LOGIN_BLOQUEADO, uidUsuario, "email", email, "bloqueado_ate", usuario.getBloqueadoAte().toString());
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, uidUsuario, "email", email, "resultado", "usuario_bloqueado");
            System.err.println("Tentativa de login para usuário bloqueado: " + email + ". Bloqueado até: " + usuario.getBloqueadoAte());
            throw new IllegalStateException("Acesso bloqueado para o usuário: " + email + ". Tente novamente após " + usuario.getBloqueadoAte());
        }

        if (usuario.getSenha() == null || usuario.getSenha().isEmpty()) {
            // Este é um estado inesperado, um usuário ativo deveria ter hash de senha.
            System.err.println("Usuário não possui hash de senha configurado: " + email);
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, uidUsuario, "email", email, "resultado", "erro_interno_senha_ausente");
            throw new IllegalStateException("Configuração interna inválida para o usuário: " + email + ". Contate o administrador.");
        }
        
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA1_ENCERRADA, uidUsuario, "email", email, "resultado", "sucesso_identificacao");
        return usuario; // Retorna o usuário se todas as verificações passarem
    }

    public Optional<String> autenticarComTecladoVirtual(Usuario usuario, List<Character[]> sequenciaPares) throws Exception {
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_INICIADA, usuario.getId(), "email", usuario.getEmail());

        if (usuario == null || sequenciaPares == null || sequenciaPares.isEmpty()) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, usuario != null ? usuario.getId() : null, "email", usuario != null ? usuario.getEmail() : "N/A", "resultado", "entrada_invalida_etapa2");
            return Optional.empty(); // Entrada inválida para a etapa de senha
        }
        
        Long uidUsuario = usuario.getId();
        String email = usuario.getEmail(); // Email já validado, usado para logs e mensagens

        // A verificação de bloqueio e hash de senha já foi feita em validarIdentificacaoUsuario,
        // mas é bom manter a checagem de bloqueio caso o estado mude entre as etapas,
        // embora improvável no fluxo síncrono típico. Se o design garantir que não muda, pode ser omitido.
        if (usuario.isAcessoBloqueado()) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_LOGIN_BLOQUEADO, uidUsuario, "email", email, "bloqueado_ate", usuario.getBloqueadoAte().toString());
            System.err.println("Usuário bloqueado detectado na Etapa 2: " + email + ". Bloqueado até: " + usuario.getBloqueadoAte());
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, uidUsuario, "email", email, "resultado", "usuario_bloqueado_etapa2");
            return Optional.empty(); 
        }

        String senhaHash = usuario.getSenha(); 
        // Checagem de senhaHash null/empty já feita em validarIdentificacaoUsuario, mas por segurança:
        if (senhaHash == null || senhaHash.isEmpty()) {
            System.err.println("Usuário sem hash de senha na Etapa 2: " + email);
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, uidUsuario, "email", email, "resultado", "erro_interno_senha_ausente_etapa2");
            return Optional.empty(); 
        }

        if (sequenciaPares.size() < 8 || sequenciaPares.size() > 10) { 
             System.err.println("Tentativa de login com comprimento de senha inválido: " + sequenciaPares.size() + " para usuário " + email);
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, uidUsuario, "email", email, "resultado", "senha_comprimento_invalido");
             return Optional.empty();
        }

        String senhaAutenticada = verificarCombinacoesDePares(new StringBuilder(), sequenciaPares, 0, senhaHash);

        if (senhaAutenticada != null) {
            usuario.resetarContadoresDeFalha();
            usuario.incrementarTotalAcessos(); 
            if(usuario.isAcessoBloqueado()) usuario.desbloquearAcesso(); 
            atualizar(usuario); 
            // Log de sucesso de login
            registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_SENHA_OK, uidUsuario, "email", email);
            registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, uidUsuario, "email", email, "resultado", "sucesso");
            return Optional.of(senhaAutenticada);
        } else {
            usuario.registrarFalhaSenha();
            int tentativas = usuario.getTentativasFalhasSenha();
            if (tentativas == 1) {
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_SENHA_ERRO1, uidUsuario, "email", email, "tentativas", String.valueOf(tentativas));
            } else if (tentativas == 2) {
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_SENHA_ERRO2, uidUsuario, "email", email, "tentativas", String.valueOf(tentativas));
            } else if (tentativas >= MAX_TENTATIVAS_SENHA) { // Maior ou igual para segurança
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_SENHA_ERRO3, uidUsuario, "email", email, "tentativas", String.valueOf(tentativas));
                usuario.bloquearAcessoPorMinutos(MINUTOS_BLOQUEIO_SENHA);
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_LOGIN_BLOQUEADO, uidUsuario, "email", email, "minutos_bloqueio", String.valueOf(MINUTOS_BLOQUEIO_SENHA));
                System.err.println("Usuário bloqueado por " + MINUTOS_BLOQUEIO_SENHA + " minutos devido a " + tentativas + " tentativas de senha: " + email);
            }
            atualizar(usuario); 
            registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, uidUsuario, "email", email, "resultado", "falha_senha");
            return Optional.empty();
        }
    }

    private String verificarCombinacoesDePares(StringBuilder senhaCandidataAtual, 
                                                List<Character[]> sequenciaPares, 
                                                int indiceParAtual, 
                                                String senhaHash) {
        if (indiceParAtual == sequenciaPares.size()) {
            String candidataFinal = senhaCandidataAtual.toString();
            if (candidataFinal.length() < 8 || candidataFinal.length() > 10) {
                return null; 
            }
            if (PasswordUtil.checkPassword(candidataFinal, senhaHash)) {
                return candidataFinal;
            }
            return null;
        }

        Character[] par = sequenciaPares.get(indiceParAtual);
        String resultado;

        senhaCandidataAtual.append(par[0]);
        resultado = verificarCombinacoesDePares(senhaCandidataAtual, sequenciaPares, indiceParAtual + 1, senhaHash);
        if (resultado != null) {
            return resultado;
        }
        senhaCandidataAtual.deleteCharAt(senhaCandidataAtual.length() - 1);

        senhaCandidataAtual.append(par[1]);
        resultado = verificarCombinacoesDePares(senhaCandidataAtual, sequenciaPares, indiceParAtual + 1, senhaHash);
        if (resultado != null) {
            return resultado;
        }
        senhaCandidataAtual.deleteCharAt(senhaCandidataAtual.length() - 1);

        return null;
    }

    public boolean isFirstExecution() {
        try {
            Optional<Grupo> adminGroupOpt = grupoDAO.buscarPorNome("Administrador");
            if (adminGroupOpt.isEmpty()) {
                System.err.println("CRÍTICO: Grupo Administrador não encontrado no banco de dados!");
                return true; 
            }
            List<Usuario> todosUsuarios = usuarioDAO.listarTodos(); 
            return todosUsuarios.stream()
                .filter(u -> u.getGrupo() != null)
                .noneMatch(u -> "Administrador".equalsIgnoreCase(u.getGrupo()));
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao verificar primeira execução.", e);
        }
    }

    public void completeAdminFirstSetup(Usuario admin, String passphrase) {
        this.adminPassphraseSession = passphrase;
        if (admin != null && "Administrador".equalsIgnoreCase(admin.getGrupo())) {
            System.out.println("[UsuarioServico] Frase secreta do administrador '" + admin.getEmail() + "' armazenada para a sessão.");
        } else if (admin == null && passphrase != null) {
             System.out.println("[UsuarioServico] Frase secreta do administrador (validada em execução subsequente) armazenada para a sessão.");
        } else {
            System.err.println("[UsuarioServico] Tentativa de chamar completeAdminFirstSetup sem dados suficientes.");
        }
    }
    
    public void storeAdminPassphraseForSession(String passphrase) {
        this.adminPassphraseSession = passphrase;
         System.out.println("[UsuarioServico] Frase secreta do administrador armazenada para a sessão (via storeAdminPassphraseForSession).");
    }

    public String getAdminPassphraseForSession() {
        return this.adminPassphraseSession;
    }

    public boolean isAdminPassphraseValidatedForSession() {
        return this.adminPassphraseSession != null && !this.adminPassphraseSession.isEmpty();
    }
    
    public boolean validarChavePrivadaComFrase(String caminhoChavePrivada, String fraseSecreta, X509Certificate certificate) throws Exception {
        if (StringUtil.isAnyEmpty(caminhoChavePrivada, fraseSecreta) || certificate == null) {
            // Log de parâmetros inválidos, se necessário
            // // // registroServico.registrarEventoDoSistema(MID_VALIDACAO_CHAVE_PARAM_INVALIDOS);
            return false;
        }
        try {
            // Alterado: Chamar loadPrivateKeyFromPEMFile diretamente
            PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(caminhoChavePrivada, fraseSecreta);
            
            if (privateKey == null) {
                // Frase secreta provavelmente incorreta, não conseguiu decriptografar.
                // Log apropriado (MID 6006) já é feito pelo SetupAdminPanel se este método retornar false ou lançar exceção.
                // Poderíamos logar aqui também para consistência interna do serviço, mas evitar duplicação.
                System.err.println("[UsuarioServico.validarChavePrivadaComFrase] Falha ao carregar/decriptografar chave privada PEM com a frase fornecida. Verifique o caminho e a frase.");
                return false; 
            }
            
            PublicKey publicKey = certificate.getPublicKey();

            // Validação da assinatura digital de um array aleatório de 8192 bytes
            SecureRandom random = new SecureRandom();
            byte[] randomBytes = new byte[8192];
            random.nextBytes(randomBytes);

            Signature sig = Signature.getInstance("SHA1withRSA"); 
            sig.initSign(privateKey);
            sig.update(randomBytes);
            byte[] digitalSignature = sig.sign();

            sig.initVerify(publicKey);
            sig.update(randomBytes);
            
            boolean validSignature = sig.verify(digitalSignature);
            if (!validSignature) {
                 System.err.println("[UsuarioServico.validarChavePrivadaComFrase] Verificação da assinatura da chave falhou.");
                // Log apropriado (MID 6007) já é feito pelo SetupAdminPanel.
            }
            return validSignature;

        } catch (Exception e) {
            // Outras exceções durante o carregamento ou validação (ex: GeneralSecurityException de Signature).
            // // // registroServico.registrarEventoDoSistema(MID_VALIDACAO_CHAVE_ERRO_GERAL, "path_chave", caminhoChavePrivada, "erro", e.getMessage());
            System.err.println("[UsuarioServico.validarChavePrivadaComFrase] Erro geral na validação: " + e.getMessage());
            throw e; // Relança
        }
    }
    
    public boolean validateAdminPassphrase(String candidatePassphrase) {
        // Log de início da tentativa de validação da frase secreta do admin.
        // MID para "VALIDACAO_ADMIN_PASSPHRASE_INICIADA" pode ser adicionado se desejado.

        if (isFirstExecution()) {
            // Este caso não deveria ser alcançado se MainFrame direciona corretamente para SetupAdminPanel.
            // No entanto, como uma salvaguarda:
            // Na primeira execução, qualquer frase não vazia é considerada "válida" para prosseguir o setup.
            // Não há chaveiro ainda para comparar.
            boolean isValid = !StringUtil.isAnyEmpty(candidatePassphrase);
            if (isValid) {
                 // Log: Frase para setup inicial aceita (não validada contra chaveiro, pois não existe)
                 // Idealmente, registrar um MID específico para esta situação, ex: SETUP_PASSPHRASE_PRIMEIRA_EXEC_ACEITA
            } else {
                 // Log: Frase para setup inicial REJEITADA (vazia)
                 // Idealmente, registrar um MID específico, ex: SETUP_PASSPHRASE_PRIMEIRA_EXEC_REJEITADA
                 // // registroServico.registrarEventoDoSistema(LogEventosMIDs.VALIDATE_ADMIN_PASSPHRASE_FALHA_GUI, "motivo", "Primeira execução, frase vazia para setup.");
            }
            return isValid;
        }
        
        Long adminUid = null; 
        try {
            List<Usuario> admins = usuarioDAO.listarTodos().stream()
                .filter(u -> u.getGrupo() != null && "Administrador".equalsIgnoreCase(u.getGrupo()))
                .toList();

            if (admins.isEmpty()) {
                System.err.println("[UsuarioServico] Erro crítico: Não é primeira execução, mas nenhum administrador encontrado para validar frase.");
                 // // // registroServico.registrarEventoDoSistema(MID_ERRO_VALIDACAO_PASSPHRASE_ADMIN_NAO_ENCONTRADO);
                 //  LogEventosMIDs.VALIDATE_ADMIN_PASSPHRASE_FALHA_GUI (com motivo específico) já será logado pela GUI.
                 return false; 
            }
            Usuario admin = admins.get(0); 
            adminUid = admin.getId();

            if (admin.getKid() == null) {
                System.err.println("[UsuarioServico] Administrador não possui KID associado. Não é possível validar frase secreta.");
                // // // registroServico.registrarEventoDoUsuario(MID_ERRO_VALIDACAO_PASSPHRASE_ADMIN_SEM_KID, adminUid);
                return false;
            }

            Optional<Chaveiro> chaveiroAdminOpt = chaveiroDAO.buscarPorKid(admin.getKid());
            if (chaveiroAdminOpt.isEmpty()) {
                System.err.println("[UsuarioServico] Dados do chaveiro não encontrados para o KID do administrador: " + admin.getKid());
                // // // registroServico.registrarEventoDoUsuario(MID_ERRO_VALIDACAO_PASSPHRASE_CHAVEIRO_NAO_ENCONTRADO, adminUid, "kid", String.valueOf(admin.getKid()));
                return false; 
            }
            Chaveiro chaveiroAdmin = chaveiroAdminOpt.get();
            
            if (StringUtil.isAnyEmpty(candidatePassphrase)) {
                // Log: Tentativa de validar frase secreta vazia (não primeira execução)
                // O painel ValidateAdminPassphrasePanel já faz essa validação e log.
                // No entanto, podemos logar aqui também para o serviço.
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, adminUid, "motivo", "frase_vazia_ou_nula_no_servico");
                return false;
            }

            try {
                X509Certificate certificate = CertificateUtil.loadCertificateFromPEMString(chaveiroAdmin.getCertificadoPem());
                PublicKey publicKey = CertificateUtil.getPublicKeyFromCertificate(certificate);
                // Usar o método correto para PKCS#8 DER binário criptografado
                PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromDERBytes(chaveiroAdmin.getChavePrivadaCriptografada(), candidatePassphrase);
                
                if (privateKey == null) { 
                     // Frase secreta incorreta, falha na decriptografia.
                     // O LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA será usado pela GUI / painel chamador.
                     // Este log aqui é mais para o serviço, se desejado.
                     System.err.println("[UsuarioServico.validateAdminPassphrase] Falha ao decriptografar chave privada do admin com a frase fornecida.");
                     // A GUI logará VALIDATE_ADMIN_PASSPHRASE_FALHA_GUI.
                     return false;
                }

                // Validação da assinatura digital de um array aleatório de 8192 bytes
                SecureRandom random = new SecureRandom();
                byte[] randomBytes = new byte[8192];
                random.nextBytes(randomBytes);

                Signature sig = Signature.getInstance("SHA1withRSA");
                sig.initSign(privateKey);
                sig.update(randomBytes);
                byte[] digitalSignature = sig.sign();

                sig.initVerify(publicKey);
                sig.update(randomBytes);
                
                if (sig.verify(digitalSignature)) {
                    storeAdminPassphraseForSession(candidatePassphrase); 
                    // // // registroServico.registrarEventoDoUsuario(MID_VALIDACAO_PASSPHRASE_ADMIN_SUCESSO, adminUid);
                    // Sucesso é logado pela GUI como VALIDATE_ADMIN_PASSPHRASE_SUCESSO_GUI.
                    return true;
                }
                
                // Se chegou aqui, a verificação da assinatura falhou.
                // A GUI logará VALIDATE_ADMIN_PASSPHRASE_FALHA_GUI.
                // Poderíamos logar um MID_VALIDACAO_PASSPHRASE_ASSINATURA_FALHA aqui se quiséssemos ser mais granulares no log do serviço.
                System.err.println("[UsuarioServico.validateAdminPassphrase] Verificação da assinatura da chave do admin falhou.");
                return false; 

            } catch (Exception e) {
                // Erro durante o processo de decriptografia/validação (ex: PEM inválido, etc.)
                // A GUI logará VALIDATE_ADMIN_PASSPHRASE_FALHA_GUI.
                // Log mais granular no serviço:
                // // // registroServico.registrarEventoDoUsuario(MID_ERRO_INTERNO_VALIDACAO_PASSPHRASE, adminUid, "kid", String.valueOf(admin.getKid()), "erro", e.getMessage());
                System.err.println("[UsuarioServico.validateAdminPassphrase] Exceção durante validação da frase secreta do admin: " + e.getMessage());
                return false; 
            }
        } catch (SQLException e) {
            // // // registroServico.registrarEventoDoSistema(MID_ERRO_BD_VALIDACAO_PASSPHRASE, "erro", e.getMessage());
             System.err.println("[UsuarioServico.validateAdminPassphrase] Erro de SQL: " + e.getMessage());
             throw new RuntimeException("Erro de banco de dados ao validar frase do admin.", e); // Relançar para que a aplicação saiba do erro crítico.
        }
    }

    public Usuario setupInitialAdmin(String nomeInputIgnorado, String emailInputIgnorado, 
                                     String caminhoCertificado, String caminhoChavePrivada, 
                                     String fraseSecretaChave, String senhaPessoal, String grupoNome) throws Exception {
        
        // // registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN);
        System.out.println("[UsuarioServico] Iniciando setup do administrador inicial... (Log MID: " + LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN + ")");

        if (!isFirstExecution()) {
            System.err.println("[UsuarioServico] Tentativa de setup do admin inicial, mas não é a primeira execução.");
            throw new IllegalStateException("Setup do administrador inicial só pode ocorrer na primeira execução.");
        }

        if (StringUtil.isAnyEmpty(caminhoCertificado, caminhoChavePrivada, fraseSecretaChave, senhaPessoal) ||
            !"Administrador".equalsIgnoreCase(grupoNome)) {
            System.err.println("[UsuarioServico] Dados inválidos fornecidos para setup do admin.");
            throw new IllegalArgumentException("Dados insuficientes ou inválidos para o cadastro do administrador.");
        }

        X509Certificate certificate;
        PublicKey publicKey;
        PrivateKey privateKey;
        String nomeDoCertificado;
        String emailDoCertificado;
        String certificadoPemString;

        try {
            System.out.println("[UsuarioServico] Carregando certificado de: " + caminhoCertificado);
            certificate = CertificateUtil.loadCertificateFromFile(caminhoCertificado);
            if (certificate == null) {
                // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "caminho", caminhoCertificado);
                throw new RuntimeException("Falha ao carregar o certificado do administrador.");
            }
            publicKey = CertificateUtil.getPublicKeyFromCertificate(certificate);
            privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(caminhoChavePrivada, fraseSecretaChave);
            if (privateKey == null) {
                // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, "path_chave", caminhoChavePrivada);
                System.err.println("[UsuarioServico.setupInitialAdmin] Falha ao carregar/decriptografar chave privada PEM. Verifique o caminho e a frase secreta.");
                throw new SecurityException("Falha ao carregar ou decriptografar a chave privada PEM. Frase secreta pode estar incorreta ou arquivo corrompido/não encontrado.");
            }
            nomeDoCertificado = CertificateUtil.extractCNFromCertificate(certificate);
            emailDoCertificado = CertificateUtil.extractEmailFromCertificate(certificate);
            if (emailDoCertificado == null || emailDoCertificado.trim().isEmpty()) {
                throw new RuntimeException("Não foi possível extrair o e-mail do certificado do administrador.");
            }
            if (nomeDoCertificado == null || nomeDoCertificado.trim().isEmpty()) nomeDoCertificado = emailDoCertificado; 
            if (usuarioDAO.emailExiste(emailDoCertificado)) { 
                throw new EmailJaExisteException("Email do certificado ('" + emailDoCertificado + "') já cadastrado.");
            }
            certificadoPemString = CertificateUtil.convertToPem(certificate);
        } catch (SQLException sqle) { 
             throw new Exception("Erro de banco de dados durante o setup do admin: " + sqle.getMessage(), sqle);
        } catch (CertificateException ce) { 
            throw new Exception("Erro relacionado ao certificado: " + ce.getMessage(), ce);
        } catch (SecurityException se) { 
             throw new Exception("Erro de segurança durante o setup do admin: " + se.getMessage(), se); 
        } catch (EmailJaExisteException ejee) { 
             throw new Exception("Erro de cadastro: " + ejee.getMessage(), ejee);
        } catch (RuntimeException re) { 
             throw new Exception("Erro inesperado durante o setup do admin (runtime): " + re.getMessage(), re);
        } catch (Exception e) { 
            throw new Exception("Erro geral inesperado no setup criptográfico do admin: " + e.getMessage(), e);
        }

        Usuario adminUsuario = new Usuario();
        adminUsuario.setNome(nomeDoCertificado);
        adminUsuario.setEmail(emailDoCertificado);
        adminUsuario.setGrupo("Administrador"); 
        adminUsuario.setSenha(PasswordUtil.hashPassword(senhaPessoal));
        
        String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
        SecretKey aesKeyForTotp = AESUtil.generateKeyFromSecret(senhaPessoal, 256);
        byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), aesKeyForTotp);
        adminUsuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
        adminUsuario.setContaAtiva(true); // Assumindo que o modelo Usuario tem setContaAtiva ou é gerenciado por padrão
        
        Optional<Grupo> adminGrupoOpt = grupoDAO.buscarPorNome("Administrador");
        if (adminGrupoOpt.isEmpty()) {
            throw new Exception("Grupo Administrador não encontrado no banco de dados durante o setup.");
        }
        Usuario adminSalvo = usuarioDAO.salvar(adminUsuario, adminGrupoOpt.get().getGid());

        // Exportar a chave privada para PKCS#8 DER criptografado usando a frase secreta
        byte[] chavePrivadaDerCriptografada;
        try {
            JcaPKCS8EncryptedPrivateKeyInfoBuilder pkcs8Builder =
                new JcaPKCS8EncryptedPrivateKeyInfoBuilder(privateKey);
            OutputEncryptor encryptor =
                new JcePKCSPBEOutputEncryptorBuilder(
                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC)
                        .setProvider("BC")
                        .build(fraseSecretaChave.toCharArray());
            PKCS8EncryptedPrivateKeyInfo encryptedInfo = pkcs8Builder.build(encryptor);
            chavePrivadaDerCriptografada = encryptedInfo.getEncoded();
            // Armazenar no Chaveiro (BD) - usar chavePrivadaDerCriptografada
            Chaveiro chaveiroAdmin = new Chaveiro(0, adminSalvo.getId(), certificadoPemString, chavePrivadaDerCriptografada); // KID será gerado pelo BD
            Chaveiro chaveiroSalvo = chaveiroDAO.salvar(chaveiroAdmin); // Salva e obtém KID
            // Atualizar o usuário admin com o KID do seu chaveiro principal
            adminSalvo.setKid(chaveiroSalvo.getKid()); // setKid espera Integer
            usuarioDAO.atualizarKidPadrao(adminSalvo.getId(), adminSalvo.getKid());
            System.out.println("[UsuarioServico] Certificado PEM e chave privada criptografada do admin armazenados no Chaveiro (BD) com KID: " + chaveiroSalvo.getKid() + " para UID: " + adminSalvo.getId());
            this.storeAdminPassphraseForSession(fraseSecretaChave); // Armazena frase validada
            Map<String, String> detalhesSucesso = Map.of("emailAdmin", adminSalvo.getEmail(), "uidAdmin", String.valueOf(adminSalvo.getId()), "kidChaveiro", String.valueOf(chaveiroSalvo.getKid()));
            System.out.println("[UsuarioServico] Setup do administrador inicial concluído com sucesso para: " + adminSalvo.getEmail() + ". Chave TOTP (plana): " + chaveSecretaTotpBase32);
            return adminSalvo;
        } catch (Exception e) {
            System.err.println("[UsuarioServico] Erro ao exportar chave privada para PKCS#8 DER criptografado: " + e.getMessage());
            throw new RuntimeException("Erro ao exportar chave privada para PKCS#8 DER criptografado.", e);
        }
    }

    // --- Métodos adicionais para gerenciamento de Chaveiro ---
    public void associarKidAoUsuario(long uid, int kid) throws SQLException {
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ASSOCIA_KID_INICIO, uid, "uid", String.valueOf(uid), "kid_a_associar", String.valueOf(kid));
        
        Usuario usuario;
        try {
            usuario = buscarPorId(uid); // Valida se usuário existe
        } catch (UsuarioNaoEncontradoException e) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.USUARIO_ASSOCIA_KID_FALHA_USUARIO_NAO_ENCONTRADO, "uid_nao_encontrado", String.valueOf(uid));
            throw e; // Relança a exceção original
        }
        
        Optional<Chaveiro> chaveiroOpt = chaveiroDAO.buscarPorKid(kid);
        if(chaveiroOpt.isEmpty()){
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ASSOCIA_KID_FALHA_CHAVEIRO_NAO_ENCONTRADO, uid, "uid", String.valueOf(uid), "kid_nao_encontrado", String.valueOf(kid));
            throw new SQLException("Chaveiro com KID " + kid + " não encontrado.");
        }
        // Opcional: verificar se chaveiroOpt.get().getUid() == uid se necessário
        // if (chaveiroOpt.get().getUid() != uid) { ... log e exceção ... }

        try {
            usuarioDAO.atualizarKidPadrao(uid, kid);
        } catch (SQLException e) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ASSOCIA_KID_FALHA_BD, uid, "uid", String.valueOf(uid), "kid", String.valueOf(kid), "erro_sql", e.getMessage());
            throw e; // Relança para a camada superior
        }
        
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.USUARIO_ASSOCIA_KID_SUCESSO, uid, "uid", String.valueOf(uid), "kid_associado", String.valueOf(kid));
        System.out.println("[UsuarioServico] KID " + kid + " associado ao usuário UID " + uid + " como KID padrão.");
    }

    public Chaveiro salvarChaveiro(long uid, String certificadoPem, byte[] chavePrivadaCriptografada, String senhaMestreDoCertificado) throws Exception {
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CHAVEIRO_SALVAR_INICIO, uid, "uid", String.valueOf(uid));
        Usuario usuario = buscarPorId(uid); // Valida se usuário existe e para obter email para logs

        try {
            PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromBytes(chavePrivadaCriptografada, senhaMestreDoCertificado);
            if (privateKey == null) { // Checagem explícita de falha na decriptografia
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, uid, "uid", String.valueOf(uid), "contexto", "salvarChaveiro");
                throw new IllegalArgumentException("Senha mestre inválida. Não foi possível decriptografar a chave privada fornecida.");
            }
            X509Certificate certificate = CertificateUtil.loadCertificateFromPEMString(certificadoPem);
            PublicKey publicKey = certificate.getPublicKey();

            if (!PrivateKeyUtil.validatePrivateKeyWithPublicKey(privateKey, publicKey)) {
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CAD_CHAVE_PRIVADA_ASSINATURA_INVALIDA, uid, "uid", String.valueOf(uid), "contexto", "salvarChaveiro");
                throw new IllegalArgumentException("A chave privada fornecida (usando a senha mestre) não corresponde ao certificado público.");
            }
        } catch (IllegalArgumentException e) { // Captura as exceções de validação lançadas acima
            throw e; // Relança para a camada superior, os logs específicos já foram feitos.
        } catch (Exception e) { // Outras exceções durante a validação (ex: formato PEM inválido)
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, uid, "uid", String.valueOf(uid), "contexto", "salvarChaveiro_validacao", "erro", e.getMessage());
            throw new IllegalArgumentException("Senha mestre inválida ou par chave/certificado incompatível. Detalhe: " + e.getMessage(), e);
        }
        
        Chaveiro novoChaveiro = new Chaveiro(0, uid, certificadoPem, chavePrivadaCriptografada); 
        Chaveiro chaveiroSalvo;
        try {
            chaveiroSalvo = chaveiroDAO.salvar(novoChaveiro);
        } catch (SQLException e) {
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CHAVEIRO_SALVAR_FALHA_BD, uid, "uid", String.valueOf(uid), "erro", e.getMessage());
            throw e; // Relança para a camada superior
        }
        
        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CHAVEIRO_SALVAR_SUCESSO, uid, "uid", String.valueOf(uid), "kid", String.valueOf(chaveiroSalvo.getKid()));
        System.out.println("[UsuarioServico] Novo chaveiro salvo com KID: " + chaveiroSalvo.getKid() + " para UID: " + uid);

        if (usuario.getKid() == null) { 
            usuarioDAO.atualizarKidPadrao(uid, chaveiroSalvo.getKid());
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.CHAVEIRO_DEFINIDO_COMO_PADRAO, uid, "uid", String.valueOf(uid), "kid", String.valueOf(chaveiroSalvo.getKid()));
            System.out.println("[UsuarioServico] Novo chaveiro KID " + chaveiroSalvo.getKid() + " definido como padrão para UID: " + uid);
        }
        return chaveiroSalvo;
    }

    public Optional<Chaveiro> buscarChaveiroPorKid(int kid) throws SQLException {
        return chaveiroDAO.buscarPorKid(kid);
    }

    public List<Chaveiro> listarChaveirosPorUid(long uid) throws SQLException {
        buscarPorId(uid); // Valida se usuário existe
        return chaveiroDAO.buscarPorUid(uid);
    }

    public static class CadastroUsuarioResult {
        public final Usuario usuario;
        public final String chaveTotpBase32;
        public CadastroUsuarioResult(Usuario usuario, String chaveTotpBase32) {
            this.usuario = usuario;
            this.chaveTotpBase32 = chaveTotpBase32;
        }
    }

    public CadastroUsuarioResult cadastrarNovoUsuario(String nomeInput, String emailInput, String senha, int gid, 
                                        String caminhoCertificado, String caminhoChavePrivada, 
                                        String fraseSecretaChave, Long adminUid) throws Exception {
        
        // Validação dos caminhos do certificado e chave, e frase secreta
        if (StringUtil.isAnyEmpty(caminhoCertificado, caminhoChavePrivada, fraseSecretaChave)) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_DADOS_INVALIDOS,
            // //     "motivo", "Certificado, chave privada e frase secreta são obrigatórios para este tipo de cadastro.",
            // //     "adminUid", String.valueOf(adminUid));
            throw new IllegalArgumentException("Caminho do certificado, caminho da chave privada e frase secreta são obrigatórios.");
        }

        if (StringUtil.isAnyEmpty(senha) || gid <= 0) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_DADOS_INVALIDOS,
            // //     "motivo", "Senha e GID válidos são obrigatórios.", "adminUid", String.valueOf(adminUid));
            throw new IllegalArgumentException("Senha e GID válidos são obrigatórios.");
        }

        X509Certificate certificate;
        PrivateKey privateKey;
        String certificadoPemString;
        byte[] chavePrivadaBytesParaChaveiro; // Usado para armazenar no Chaveiro (PKCS#8 DER criptografado)
        String nomeExtraidoCert;
        String emailExtraidoCert;

        // Processar certificado PRIMEIRO para obter nome e email
        try {
            System.out.println("[UsuarioServico.cadastrarNovoUsuario] Processando certificado fornecido: " + caminhoCertificado);
            certificate = CertificateUtil.loadCertificateFromFile(caminhoCertificado);
            if (certificate == null) {
                // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "adminUid", String.valueOf(adminUid), "caminho", caminhoCertificado);
                throw new IllegalArgumentException("Falha ao carregar o certificado do arquivo: " + caminhoCertificado);
            }

            nomeExtraidoCert = CertificateUtil.extractCNFromCertificate(certificate);
            emailExtraidoCert = CertificateUtil.extractEmailFromCertificate(certificate);

            if (StringUtil.isAnyEmpty(emailExtraidoCert)) {
                // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_DADOS_INVALIDOS, "adminUid", String.valueOf(adminUid), "motivo", "Email não pôde ser extraído do certificado.");
                throw new IllegalArgumentException("Não foi possível extrair o e-mail do certificado fornecido.");
            }
            if (StringUtil.isAnyEmpty(nomeExtraidoCert)) {
                nomeExtraidoCert = emailExtraidoCert; // Default para email se CN não encontrado
            }
            certificadoPemString = CertificateUtil.convertToPem(certificate);

        } catch (CertificateException | IOException e) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "adminUid", String.valueOf(adminUid), "caminho", caminhoCertificado, "erro", e.getMessage());
            throw new Exception("Erro ao processar arquivo de certificado: " + e.getMessage(), e);
        } catch (IllegalArgumentException iae) { // Captura as exceções de validação de nome/email do cert
            throw iae; 
        } catch (Exception e) { // Outras exceções genéricas ao processar certificado
             // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "adminUid", String.valueOf(adminUid), "caminho", caminhoCertificado, "erro_inesperado", e.getMessage());
            throw new Exception("Erro inesperado ao processar certificado: " + e.getMessage(), e);
        }
        
        // Agora que temos emailExtraidoCert, podemos logar corretamente e verificar existência
        // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_INICIO_FLUXO, "email_novo_usuario", emailExtraidoCert, "adminUid", String.valueOf(adminUid));
        System.out.println("[UsuarioServico] Iniciando cadastro de novo usuário (email do cert): " + emailExtraidoCert + " por admin: " + adminUid + " (Log MID: " + LogEventosMIDs.CAD_USUARIO_INICIO_FLUXO + ")");

        if (usuarioDAO.emailExiste(emailExtraidoCert)) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_EMAIL_JA_EXISTE_NOVO_USUARIO, "email", emailExtraidoCert);
            throw new IllegalArgumentException("Email extraído do certificado ('" + emailExtraidoCert + "') já cadastrado.");
        }

        Grupo grupo = grupoDAO.buscarPorId(gid)
                .orElseThrow(() -> {
                    // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_GRUPO_NAO_ENCONTRADO_NOVO_USUARIO, "gid", String.valueOf(gid));
                    return new IllegalArgumentException("Grupo com GID " + gid + " não encontrado.");
                });

        // Processar chave privada e validar par com certificado
        // A frase secreta da chave privada deve ser testada e a chave privada deve ser verificada...
        try {
            // System.out.println("[UsuarioServico] Processando chave privada: " + caminhoChavePrivada); // Log redundante, já temos o caminho
            // Validar e carregar chave privada
            if (!validarChavePrivadaComFrase(caminhoChavePrivada, fraseSecretaChave, certificate)) {
                 // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CHAVE_PRIVADA_ASSINATURA_INVALIDA, "email", emailExtraidoCert, "path_chave", caminhoChavePrivada);
                 throw new SecurityException("A chave privada fornecida não pôde ser validada com o certificado ou a frase secreta está incorreta.");
            }
            // Se chegou aqui, a chave é válida com a frase e corresponde ao certificado.
            // Agora, carregue a chave privada para criptografá-la para armazenamento.
            PrivateKey pkToEncrypt = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(caminhoChavePrivada, fraseSecretaChave);
             if (pkToEncrypt == null) { // Dupla checagem, embora validarChavePrivadaComFrase já faça isso.
                throw new SecurityException("Falha ao recarregar a chave privada para criptografia, mesmo após validação inicial.");
            }

            JcaPKCS8EncryptedPrivateKeyInfoBuilder pkcs8Builder =
                new JcaPKCS8EncryptedPrivateKeyInfoBuilder(pkToEncrypt);
            OutputEncryptor encryptor =
                new JcePKCSPBEOutputEncryptorBuilder(
                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC)
                        .setProvider("BC")
                        .build(fraseSecretaChave.toCharArray());
            PKCS8EncryptedPrivateKeyInfo encryptedInfo = pkcs8Builder.build(encryptor);
            chavePrivadaBytesParaChaveiro = encryptedInfo.getEncoded(); // Este é o formato binário criptografado para o BD

        } catch (SecurityException se) {
            throw se; // Relança exceções de segurança já logadas ou específicas
        } catch (Exception e) {
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CHAVE_PRIVADA_PATH_INVALIDO, "email", emailExtraidoCert, "path_chave", caminhoChavePrivada, "erro_proc_chave", e.getMessage());
            throw new Exception("Erro ao processar ou validar a chave privada: " + e.getMessage(), e);
        }
        
        String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
        String hashSenha = PasswordUtil.hashPassword(senha);

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nomeExtraidoCert); // Usar nome do certificado
        novoUsuario.setEmail(emailExtraidoCert); // Usar email do certificado
        novoUsuario.setSenha(hashSenha);
        novoUsuario.setGrupo(grupo.getNomeGrupo());
        novoUsuario.setContaAtiva(true);
        
        try {
            SecretKey chaveAESParaTotp = AESUtil.generateKeyFromSecret(senha, 256);
            byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), chaveAESParaTotp);
            novoUsuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
        } catch (Exception e) {
            throw new Exception("Falha ao criptografar a chave TOTP para o usuário " + emailExtraidoCert, e);
        }
        
        Usuario usuarioSalvo = usuarioDAO.salvar(novoUsuario, gid);

        Integer kidSalvo = null;
        // certificadoPemString e chavePrivadaBytesParaChaveiro devem estar populados se chegou aqui
        if (certificadoPemString != null && chavePrivadaBytesParaChaveiro != null) {
            Chaveiro novoChaveiro = new Chaveiro(0, usuarioSalvo.getId(), certificadoPemString, chavePrivadaBytesParaChaveiro);
            Chaveiro chaveiroSalvo = chaveiroDAO.salvar(novoChaveiro);
            usuarioDAO.atualizarKidPadrao(usuarioSalvo.getId(), chaveiroSalvo.getKid());
            usuarioSalvo.setKid(chaveiroSalvo.getKid()); 
            kidSalvo = chaveiroSalvo.getKid();
            System.out.println("[UsuarioServico] Chaveiro salvo para " + emailExtraidoCert + " com KID: " + kidSalvo);
        } else {
            // Isso não deveria acontecer se as validações de cert/chave obrigatórios estiverem corretas no início
            System.err.println("[UsuarioServico.cadastrarNovoUsuario] Alerta: Certificado PEM ou chave privada criptografada não disponíveis para salvar chaveiro para " + emailExtraidoCert);
            // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_DADOS_INVALIDOS, "email", emailExtraidoCert, "motivo", "dados_chaveiro_ausentes_inesperadamente");
        }
        
        // // registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_USUARIO_SUCESSO, 
        // //     "uidNovoUsuario", String.valueOf(usuarioSalvo.getId()), 
        // //     "emailNovoUsuario", usuarioSalvo.getEmail(),
        // //     "gidNovoUsuario", String.valueOf(gid),
        // //     "kidPadrao", kidSalvo != null ? String.valueOf(kidSalvo) : "N/A",
        // //     "adminUid", String.valueOf(adminUid)
        // // );
        System.out.println("[UsuarioServico] Usuário " + usuarioSalvo.getEmail() + " cadastrado com sucesso. UID: " + usuarioSalvo.getId() + ". Chave TOTP (plana - para debug): " + chaveSecretaTotpBase32);
        return new CadastroUsuarioResult(usuarioSalvo, chaveSecretaTotpBase32);
    }

    public TotpServico getTotpServico() {
        return this.totpServico;
    }
}