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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import java.sql.SQLException;

public class UsuarioServico {

    private final UsuarioDAO usuarioDAO;
    private final GrupoDAO grupoDAO;
    private final ChaveiroDAO chaveiroDAO;
    private final TotpServico totpServico;
    private static final int MAX_TENTATIVAS_SENHA = 3; // Exemplo, ajuste conforme necessário
    private static final int MINUTOS_BLOQUEIO_SENHA = 2; // Exemplo

    private String adminPassphraseSession = null;

    public UsuarioServico(TotpServico totpServico) {
        this.totpServico = totpServico;
        this.usuarioDAO = new UsuarioDAOImpl();
        this.grupoDAO = new GrupoDAOImpl();
        this.chaveiroDAO = new ChaveiroDAOImpl();
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
        try {
            Usuario usuarioAntes = buscarPorId(usuario.getId()); // Garante que o usuário existe
            if (!usuarioAntes.getEmail().equals(usuario.getEmail()) && existsByEmail(usuario.getEmail())) {
                 throw new EmailJaExisteException("Email já cadastrado para outro usuário: " + usuario.getEmail());
            }
            usuarioDAO.atualizar(usuario);
            return usuario; 
        } catch (SQLException e) {
            throw new RuntimeException("Erro de banco de dados ao atualizar usuário: " + usuario.getEmail(), e);
        } catch (UsuarioNaoEncontradoException e) { 
            throw e;
        }
    }
    
    public void bloquearUsuario(Long id, int minutos) {
        Usuario usuario = buscarPorId(id); // Lança exceção se não encontrado
        usuario.bloquearAcessoPorMinutos(minutos);
        atualizar(usuario); // Persiste a alteração
    }

    public void desbloquearUsuario(Long id) {
        Usuario usuario = buscarPorId(id); // Lança exceção se não encontrado
        usuario.desbloquearAcesso();
        atualizar(usuario); // Persiste a alteração
    }

    public String obterChaveTotpDescriptografada(Usuario usuario, String senhaCandidata) throws Exception {
        SecretKey chaveAES = AESUtil.generateKeyFromSecret(senhaCandidata, 256);
        byte[] chaveTotpCriptografada = Base64.getDecoder().decode(usuario.getChaveSecretaTotp());
        byte[] chaveTotpBytes = AESUtil.decrypt(chaveTotpCriptografada, chaveAES);
        return new String(chaveTotpBytes, StandardCharsets.UTF_8);
    }

    public Optional<String> autenticarComTecladoVirtual(String email, List<Character[]> sequenciaPares) throws Exception {
        if (email == null || email.trim().isEmpty() || sequenciaPares == null || sequenciaPares.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = buscarPorEmail(email); // Busca o usuário via DAO
        if (usuario == null) {
             return Optional.empty(); 
        }

        if (usuario.isAcessoBloqueado()) {
            System.err.println("Tentativa de login para usuário bloqueado: " + email + ". Bloqueado até: " + usuario.getBloqueadoAte());
            return Optional.empty(); 
        }

        String senhaHash = usuario.getSenha(); 
        if (senhaHash == null || senhaHash.isEmpty()) {
            System.err.println("Usuário não possui hash de senha configurado: " + email);
            return Optional.empty(); 
        }

        if (sequenciaPares.size() < 8 || sequenciaPares.size() > 10) { 
             System.err.println("Tentativa de login com comprimento de senha inválido: " + sequenciaPares.size() + " para usuário " + email);
             return Optional.empty();
        }

        String senhaAutenticada = verificarCombinacoesDePares(new StringBuilder(), sequenciaPares, 0, senhaHash);

        if (senhaAutenticada != null) {
            usuario.resetarContadoresDeFalha();
            usuario.incrementarTotalAcessos(); 
            if(usuario.isAcessoBloqueado()) usuario.desbloquearAcesso(); 
            atualizar(usuario); // Persiste as alterações no usuário via DAO
            return Optional.of(senhaAutenticada);
        } else {
            usuario.registrarFalhaSenha();
            if (usuario.getTentativasFalhasSenha() >= MAX_TENTATIVAS_SENHA) {
                usuario.bloquearAcessoPorMinutos(MINUTOS_BLOQUEIO_SENHA);
                System.err.println("Usuário bloqueado por " + MINUTOS_BLOQUEIO_SENHA + " minutos devido a " + usuario.getTentativasFalhasSenha() + " tentativas de senha: " + email);
            }
            atualizar(usuario); // Persiste as alterações no usuário via DAO
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
    
    public boolean validateAdminPassphrase(String candidatePassphrase) {
        if (isFirstExecution()) {
            return candidatePassphrase != null && !candidatePassphrase.trim().isEmpty();
        }
        
        try {
            List<Usuario> admins = usuarioDAO.listarTodos().stream()
                .filter(u -> u.getGrupo() != null && "Administrador".equalsIgnoreCase(u.getGrupo()))
                .toList();

            if (admins.isEmpty()) {
                 System.err.println("[UsuarioServico] Erro crítico: Não é primeira execução, mas nenhum administrador encontrado para validar frase.");
                 return false; 
            }
            Usuario admin = admins.get(0); 

            if (admin.getKid() == null) {
                System.err.println("[UsuarioServico] Administrador não possui KID associado. Não é possível validar frase secreta.");
                return false;
            }

            Optional<Chaveiro> chaveiroAdminOpt = chaveiroDAO.buscarPorKid(admin.getKid());
            if (chaveiroAdminOpt.isEmpty()) {
                System.err.println("[UsuarioServico] Dados do chaveiro não encontrados para o KID do administrador: " + admin.getKid());
                return false;
            }
            Chaveiro chaveiroAdmin = chaveiroAdminOpt.get();
            
            if (candidatePassphrase == null || candidatePassphrase.trim().isEmpty()) {
                return false;
            }

            try {
                X509Certificate certificate = CertificateUtil.loadCertificateFromPEMString(chaveiroAdmin.getCertificadoPem());
                PublicKey publicKey = CertificateUtil.getPublicKeyFromCertificate(certificate);
                PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromBytes(chaveiroAdmin.getChavePrivadaCriptografada(), candidatePassphrase);
                
                if (PrivateKeyUtil.validatePrivateKeyWithPublicKey(privateKey, publicKey)) {
                    storeAdminPassphraseForSession(candidatePassphrase); 
                    return true;
                }
                return false; 
            } catch (Exception e) {
                System.err.println("Falha ao validar frase secreta do admin com dados do chaveiro: " + e.getMessage());
                return false; 
            }
        } catch (SQLException e) {
             throw new RuntimeException("Erro de banco de dados ao validar frase do admin.", e);
        }
    }

    public Usuario setupInitialAdmin(String nomeInputIgnorado, String emailInputIgnorado, 
                                     String caminhoCertificado, String caminhoChavePrivada, 
                                     String fraseSecretaChave, String senhaPessoal, String grupoNome) throws Exception {
        
        System.out.println("[UsuarioServico] Iniciando setup do administrador inicial... (MSG 1005)");
        if (!isFirstExecution()) {
            System.err.println("[UsuarioServico] Tentativa de setup do admin inicial, mas não é a primeira execução.");
            throw new IllegalStateException("Setup do administrador inicial só pode ocorrer na primeira execução.");
        }

        if (caminhoCertificado == null || caminhoCertificado.trim().isEmpty() ||
            caminhoChavePrivada == null || caminhoChavePrivada.trim().isEmpty() ||
            fraseSecretaChave == null || fraseSecretaChave.trim().isEmpty() ||
            senhaPessoal == null || senhaPessoal.trim().isEmpty() ||
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
        byte[] chavePrivadaOriginalCriptografadaBytes;

        try {
            System.out.println("[UsuarioServico] Carregando certificado de: " + caminhoCertificado);
            certificate = CertificateUtil.loadCertificateFromFile(caminhoCertificado);
            if (certificate == null) {
                throw new RuntimeException("Falha ao carregar o certificado do administrador. (MSG 6004 - adaptado)");
            }
            publicKey = CertificateUtil.getPublicKeyFromCertificate(certificate);
            
            chavePrivadaOriginalCriptografadaBytes = Files.readAllBytes(Paths.get(caminhoChavePrivada));
            privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromBytes(chavePrivadaOriginalCriptografadaBytes, fraseSecretaChave);
            
            if (!PrivateKeyUtil.validatePrivateKeyWithPublicKey(privateKey, publicKey)) {
                throw new SecurityException("Chave privada não corresponde ao certificado. (MSG 6007)");
            }

            nomeDoCertificado = CertificateUtil.extractCNFromCertificate(certificate);
            emailDoCertificado = CertificateUtil.extractEmailFromCertificate(certificate);
            if (emailDoCertificado == null || emailDoCertificado.trim().isEmpty()) {
                throw new RuntimeException("Não foi possível extrair o e-mail do certificado do administrador.");
            }
            if (nomeDoCertificado == null || nomeDoCertificado.trim().isEmpty()) nomeDoCertificado = emailDoCertificado; 

            if (usuarioDAO.emailExiste(emailDoCertificado)) { // Usa DAO
                throw new EmailJaExisteException("Email do certificado ('" + emailDoCertificado + "') já cadastrado.");
            }
            certificadoPemString = CertificateUtil.convertToPem(certificate);

        } catch (CertificateException | IOException e) {
            throw new Exception("Erro ao processar arquivos de certificado ou chave privada: " + e.getMessage(), e);
        } catch (SecurityException e) { 
             throw e; 
        } catch (Exception e) {
            throw new Exception("Erro inesperado no setup criptográfico do admin: " + e.getMessage(), e);
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
        Usuario adminSalvo = usuarioDAO.salvar(adminUsuario, adminGrupoOpt.get().getGid()); // Salva e obtém UID

        // Armazenar no Chaveiro (BD)
        Chaveiro chaveiroAdmin = new Chaveiro(0, adminSalvo.getId(), certificadoPemString, chavePrivadaOriginalCriptografadaBytes); // KID será gerado pelo BD
        Chaveiro chaveiroSalvo = chaveiroDAO.salvar(chaveiroAdmin); // Salva e obtém KID
        
        // Atualizar o usuário admin com o KID do seu chaveiro principal
        adminSalvo.setKid(chaveiroSalvo.getKid()); // setKid espera Integer
        usuarioDAO.atualizarKidPadrao(adminSalvo.getId(), adminSalvo.getKid());

        System.out.println("[UsuarioServico] Certificado PEM e chave privada criptografada do admin armazenados no Chaveiro (BD) com KID: " + chaveiroSalvo.getKid() + " para UID: " + adminSalvo.getId());
        
        this.storeAdminPassphraseForSession(fraseSecretaChave); // Armazena frase validada
        
        System.out.println("[UsuarioServico] Setup do administrador inicial concluído com sucesso para: " + adminSalvo.getEmail() + ". Chave TOTP (plana): " + chaveSecretaTotpBase32);
        return adminSalvo;
    }

    // --- Métodos adicionais para gerenciamento de Chaveiro ---
    public void associarKidAoUsuario(long uid, int kid) throws SQLException {
        Usuario usuario = buscarPorId(uid); // Valida se usuário existe
        
        Optional<Chaveiro> chaveiroOpt = chaveiroDAO.buscarPorKid(kid);
        if(chaveiroOpt.isEmpty()){
            throw new SQLException("Chaveiro com KID " + kid + " não encontrado.");
        }
        // Opcional: verificar se chaveiroOpt.get().getUid() == uid se necessário

        usuarioDAO.atualizarKidPadrao(uid, kid);
        System.out.println("[UsuarioServico] KID " + kid + " associado ao usuário UID " + uid + " como KID padrão.");
    }

    public Chaveiro salvarChaveiro(long uid, String certificadoPem, byte[] chavePrivadaCriptografada, String senhaMestreDoCertificado) throws Exception {
        Usuario usuario = buscarPorId(uid); // Valida se usuário existe

        try {
            PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromBytes(chavePrivadaCriptografada, senhaMestreDoCertificado);
            X509Certificate certificate = CertificateUtil.loadCertificateFromPEMString(certificadoPem);
            PublicKey publicKey = certificate.getPublicKey();

            if (!PrivateKeyUtil.validatePrivateKeyWithPublicKey(privateKey, publicKey)) {
                throw new IllegalArgumentException("A chave privada fornecida (usando a senha mestre) não corresponde ao certificado público.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Senha mestre inválida ou par chave/certificado incompatível. Detalhe: " + e.getMessage(), e);
        }
        
        Chaveiro novoChaveiro = new Chaveiro(0, uid, certificadoPem, chavePrivadaCriptografada); 
        Chaveiro chaveiroSalvo = chaveiroDAO.salvar(novoChaveiro);
        System.out.println("[UsuarioServico] Novo chaveiro salvo com KID: " + chaveiroSalvo.getKid() + " para UID: " + uid);

        if (usuario.getKid() == null) { // Se não houver KID padrão, define este
            usuarioDAO.atualizarKidPadrao(uid, chaveiroSalvo.getKid());
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
}