package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import br.com.cofredigital.crypto.PasswordUtil;
import br.com.cofredigital.crypto.AESUtil;
import br.com.cofredigital.crypto.CertificateUtil;
import br.com.cofredigital.crypto.PrivateKeyUtil;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;

public class UsuarioServico {

    private final Map<Long, Usuario> usuariosMap = new ConcurrentHashMap<>();
    private final Map<String, Long> emailParaIdMap = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);
    
    private final TotpServico totpServico;
    private static final int MAX_TENTATIVAS_SENHA = 3;
    private static final int MINUTOS_BLOQUEIO_SENHA = 2;

    // Campos para gerenciamento da primeira execução e frase secreta do admin
    private String adminPassphraseSession = null; // Armazena a frase secreta do admin para a sessão atual
    // A lógica de 'primeira execução' será determinada pela ausência de um admin.

    // Simulação da tabela Chaveiro (KID que é o Long ID do usuário -> DadosChaveiro)
    private static class DadosChaveiro {
        Long kid; // Agora é Long, correspondendo ao ID do usuário
        String certificadoPem;
        byte[] chavePrivadaCriptografadaOriginal;

        public DadosChaveiro(Long kid, String certificadoPem, byte[] chavePrivadaCriptografadaOriginal) {
            this.kid = kid;
            this.certificadoPem = certificadoPem;
            this.chavePrivadaCriptografadaOriginal = chavePrivadaCriptografadaOriginal;
        }
    }
    private final Map<Long, DadosChaveiro> chaveiroMap = new ConcurrentHashMap<>(); // Chave do Map agora é Long

    public UsuarioServico(TotpServico totpServico) {
        this.totpServico = totpServico;
    }

    public Usuario cadastrarUsuario(Usuario usuario, String senhaOriginal) throws Exception {
        if (emailParaIdMap.containsKey(usuario.getEmail())) {
            throw new EmailJaExisteException(usuario.getEmail());
        }
        
        usuario.setSenha(PasswordUtil.hashPassword(senhaOriginal));
        
        String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
        
        SecretKey chaveAES = AESUtil.generateKeyFromSecret(senhaOriginal, 256);
        
        byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), chaveAES);
        
        usuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
        
        long idGerado = proximoId.getAndIncrement();
        usuario.setId(idGerado);
        usuario.setKid(idGerado); // KID é o mesmo que o ID do usuário (Long)

        usuariosMap.put(idGerado, usuario);
        emailParaIdMap.put(usuario.getEmail(), idGerado);
        
        System.out.println("[UsuarioServico] Usuário cadastrado: " + usuario.getEmail() + " com chave TOTP: " + chaveSecretaTotpBase32 + " e KID: " + usuario.getKid());
        // TODO: Para usuários normais, também precisaria armazenar cert/chave no chaveiroMap se aplicável e se eles tiverem cert/chaves próprios.
        // Se o usuário normal não tiver certificado/chave própria no cadastro inicial, o chaveiroMap só se aplicaria ao admin inicialmente.
        return usuario;
    }

    public Usuario buscarPorId(Long id) {
        return Optional.ofNullable(usuariosMap.get(id))
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    public Usuario buscarPorEmail(String email) {
        Long id = emailParaIdMap.get(email);
        if (id == null) {
            return null;
        }
        return buscarPorId(id);
    }
    
    public boolean existsByEmail(String email) {
        return emailParaIdMap.containsKey(email);
    }

    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuariosMap.values());
    }

    public Usuario atualizar(Usuario usuario) {
        Usuario existente = buscarPorId(usuario.getId());
        if (!existente.getEmail().equals(usuario.getEmail()) && emailParaIdMap.containsKey(usuario.getEmail())){
            throw new EmailJaExisteException("Email já cadastrado para outro usuário: " + usuario.getEmail());
        }
        
        if (!existente.getEmail().equals(usuario.getEmail())) {
            emailParaIdMap.remove(existente.getEmail());
            emailParaIdMap.put(usuario.getEmail(), usuario.getId());
        }

        usuariosMap.put(usuario.getId(), usuario);
        return usuario;
    }

    public void bloquearUsuario(Long id, int minutos) {
        Usuario usuario = buscarPorId(id);
        usuario.bloquearAcessoPorMinutos(minutos);
    }

    public void desbloquearUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.desbloquearAcesso();
    }

    public String obterChaveTotpDescriptografada(Usuario usuario, String senhaCandidata) throws Exception {
        SecretKey chaveAES = AESUtil.generateKeyFromSecret(senhaCandidata, 256);
        byte[] chaveTotpCriptografada = Base64.getDecoder().decode(usuario.getChaveSecretaTotp());
        byte[] chaveTotpBytes = AESUtil.decrypt(chaveTotpCriptografada, chaveAES);
        return new String(chaveTotpBytes, StandardCharsets.UTF_8);
    }

    /**
     * Autentica um usuário usando a sequência de pares de dígitos do teclado virtual.
     * Gera todas as combinações de senha possíveis a partir dos pares e verifica cada uma
     * contra o hash da senha armazenada do usuário.
     *
     * @param email O email do usuário.
     * @param sequenciaPares A lista de pares de caracteres selecionados no teclado virtual.
     * @return Um Optional contendo a senha em texto plano que foi validada se a autenticação
     *         for bem-sucedida, ou Optional.empty() caso contrário.
     * @throws Exception para outros erros.
     */
    public Optional<String> autenticarComTecladoVirtual(String email, List<Character[]> sequenciaPares) throws Exception {
        if (email == null || email.trim().isEmpty() || sequenciaPares == null || sequenciaPares.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = buscarPorEmail(email);
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
            atualizar(usuario); 
            return Optional.of(senhaAutenticada);
        } else {
            usuario.registrarFalhaSenha();
            if (usuario.getTentativasFalhasSenha() >= MAX_TENTATIVAS_SENHA) {
                usuario.bloquearAcessoPorMinutos(MINUTOS_BLOQUEIO_SENHA);
                System.err.println("Usuário bloqueado por " + MINUTOS_BLOQUEIO_SENHA + " minutos devido a " + usuario.getTentativasFalhasSenha() + " tentativas de senha: " + email);
            }
            atualizar(usuario); 
            return Optional.empty();
        }
    }

    private String verificarCombinacoesDePares(StringBuilder senhaCandidataAtual, 
                                                List<Character[]> sequenciaPares, 
                                                int indiceParAtual, 
                                                String senhaHash) {
        if (indiceParAtual == sequenciaPares.size()) {
            String candidataFinal = senhaCandidataAtual.toString();
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

    /**
     * Verifica se é a primeira execução do sistema.
     * Considera primeira execução se não houver nenhum usuário administrador cadastrado.
     * @return true se for a primeira execução, false caso contrário.
     */
    public boolean isFirstExecution() {
        return usuariosMap.values().stream()
                .noneMatch(u -> "Administrador".equalsIgnoreCase(u.getGrupo()));
    }

    /**
     * Chamado após o cadastro bem-sucedido do primeiro administrador (via setupInitialAdmin)
     * ou após a validação bem-sucedida da frase secreta em execuções subsequentes.
     * Armazena a frase secreta do administrador para a sessão atual.
     * @param admin O usuário administrador relevante (pode ser null se apenas armazenando frase validada).
     * @param passphrase A frase secreta validada a ser armazenada para a sessão.
     */
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
    
    /**
     * Armazena a frase secreta do administrador para a sessão atual.
     * Usado após a validação bem-sucedida da frase em execuções subsequentes.
     * @param passphrase A frase secreta validada.
     */
    public void storeAdminPassphraseForSession(String passphrase) {
        this.adminPassphraseSession = passphrase;
         System.out.println("[UsuarioServico] Frase secreta do administrador armazenada para a sessão (via storeAdminPassphraseForSession).");
    }

    /**
     * Obtém a frase secreta do administrador que foi validada e armazenada para a sessão atual.
     * @return A frase secreta do administrador, ou null se não estiver disponível/validada.
     */
    public String getAdminPassphraseForSession() {
        return this.adminPassphraseSession;
    }

    /**
     * Verifica se a frase secreta do administrador já foi validada e armazenada para a sessão atual.
     * @return true se a frase estiver disponível, false caso contrário.
     */
    public boolean isAdminPassphraseValidatedForSession() {
        return this.adminPassphraseSession != null && !this.adminPassphraseSession.isEmpty();
    }
    
    /**
     * Valida a frase secreta candidata do administrador em execuções subsequentes.
     * EM IMPLEMENTAÇÃO - Esta é uma simulação básica.
     * A validação real deve envolver a tentativa de usar a frase para descriptografar
     * a chave privada do administrador armazenada de forma segura.
     * @param candidatePassphrase A frase a ser validada.
     * @return true se a frase for considerada válida (placeholder), false caso contrário.
     */
    public boolean validateAdminPassphrase(String candidatePassphrase) {
        if (!isFirstExecution()) {
            Usuario admin = usuariosMap.values().stream()
                .filter(u -> "Administrador".equalsIgnoreCase(u.getGrupo()))
                .findFirst()
                .orElse(null);

            if (admin == null) {
                System.err.println("[UsuarioServico] Erro crítico: Não é primeira execução, mas nenhum administrador encontrado para validar frase.");
                return false; 
            }
            
            System.err.println("[UsuarioServico] TODO: Implementar validação completa da frase secreta do admin em execuções subsequentes.");
            if (this.adminPassphraseSession != null && this.adminPassphraseSession.equals(candidatePassphrase)){
                return true; 
            }
            // A validação real da passphrase ocorreria ao tentar carregar a chave privada do admin
            // usando PrivateKeyUtil.loadEncryptedPKCS8PrivateKey com os dados do chaveiroMap e candidatePassphrase.
            // Se carregar com sucesso e validar contra o certificado, a passphrase é válida.
            // Esta simulação aqui é muito básica e depende do fluxo externo.
            if (candidatePassphrase != null && !candidatePassphrase.isEmpty()) {
                // Tentativa de simular a validação (sem realmente fazer a criptografia aqui)
                DadosChaveiro dadosAdmin = chaveiroMap.get(admin.getKid());
                if (dadosAdmin != null) {
                    // Em um cenário real, tentaríamos carregar a chave privada com candidatePassphrase
                    // Se PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(pathDoArquivoOriginal, candidatePassphrase) funcionar, é válida.
                    // Como não temos o path aqui, e sim os bytes, a lógica seria um pouco diferente
                    // ou precisaríamos de um método em PrivateKeyUtil que aceite bytes criptografados.
                    // Por ora, esta validação de frase para execuções subsequentes continua um TODO complexo.
                    System.out.println("[UsuarioServico] Simulação: Se a frase secreta funcionar para decriptar a chave do admin (não implementado aqui), seria válida.");
                    return true; // Simulação básica de sucesso
                }
            }
            return false; 
        } else {
            // Na primeira execução, a "validação" da frase é se ela consegue decriptar a chave fornecida.
            return candidatePassphrase != null && !candidatePassphrase.isEmpty();
        }
    }

    /**
     * Configura o administrador inicial do sistema.
     * Este método é chamado durante a primeira execução.
     *
     * @param nomeInput Nome fornecido no formulário (será substituído pelo do certificado).
     * @param emailInput Email fornecido no formulário (será substituído pelo do certificado).
     * @param caminhoCertificado Path para o arquivo do certificado digital do administrador (PEM X.509).
     * @param caminhoChavePrivada Path para o arquivo da chave privada do administrador (PKCS#8 binário, criptografado com AES/ECB/PKCS5Padding pela fraseSecretaChave).
     * @param fraseSecretaChave Frase secreta para descriptografar a chave privada do arquivo.
     * @param senhaPessoal Senha numérica pessoal do administrador (para login e chave TOTP).
     * @param grupo Grupo do usuário (deve ser "Administrador").
     * @return O objeto Usuario do administrador configurado.
     * @throws Exception Se ocorrer qualquer erro durante o setup.
     */
    public Usuario setupInitialAdmin(String nomeInputIgnorado, String emailInputIgnorado, 
                                     String caminhoCertificado, String caminhoChavePrivada, 
                                     String fraseSecretaChave, String senhaPessoal, String grupo) throws Exception {
        
        System.out.println("[UsuarioServico] Iniciando setup do administrador inicial... (MSG 1005)");
        if (!isFirstExecution()) {
            System.err.println("[UsuarioServico] Tentativa de setup do admin inicial, mas não é a primeira execução.");
            throw new IllegalStateException("Setup do administrador inicial só pode ocorrer na primeira execução.");
        }

        if (caminhoCertificado == null || caminhoCertificado.trim().isEmpty() ||
            caminhoChavePrivada == null || caminhoChavePrivada.trim().isEmpty() ||
            fraseSecretaChave == null || fraseSecretaChave.trim().isEmpty() ||
            senhaPessoal == null || senhaPessoal.trim().isEmpty() ||
            !"Administrador".equalsIgnoreCase(grupo)) {
            System.err.println("[UsuarioServico] Dados inválidos fornecidos para setup do admin.");
            throw new IllegalArgumentException("Dados insuficientes ou inválidos para o cadastro do administrador.");
        }

        X509Certificate certificate;
        PublicKey publicKey;
        PrivateKey privateKey;
        String nomeDoCertificado;
        String emailDoCertificado;
        String certificadoPem;
        byte[] chavePrivadaOriginalCriptografadaBytes;

        try {
            System.out.println("[UsuarioServico] Carregando certificado de: " + caminhoCertificado);
            certificate = CertificateUtil.loadCertificateFromFile(caminhoCertificado);
            if (certificate == null) {
                System.err.println("[UsuarioServico] Falha ao carregar o certificado. (MSG 6004 - adaptado)");
                throw new RuntimeException("Falha ao carregar o certificado do administrador.");
            }
            System.out.println("[UsuarioServico] Certificado carregado. Sujeito: " + certificate.getSubjectX500Principal().getName());

            publicKey = CertificateUtil.getPublicKeyFromCertificate(certificate);
            if (publicKey == null) {
                 System.err.println("[UsuarioServico] Falha ao extrair chave pública do certificado.");
                throw new RuntimeException("Falha ao extrair chave pública do certificado.");
            }

            System.out.println("[UsuarioServico] Lendo bytes da chave privada original de: " + caminhoChavePrivada);
            chavePrivadaOriginalCriptografadaBytes = Files.readAllBytes(Paths.get(caminhoChavePrivada));

            System.out.println("[UsuarioServico] Carregando e decriptografando chave privada de: " + caminhoChavePrivada);
            privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(caminhoChavePrivada, fraseSecretaChave);
            if (privateKey == null) {
                System.err.println("[UsuarioServico] Falha ao carregar/decriptografar a chave privada. Verifique o caminho ou frase secreta. (MSG 6005 ou 6006)");
                throw new RuntimeException("Falha ao carregar ou decriptografar a chave privada do administrador. Verifique o caminho ou a frase secreta.");
            }
            System.out.println("[UsuarioServico] Chave privada carregada e decriptografada.");

            System.out.println("[UsuarioServico] Validando chave privada com chave pública do certificado...");
            boolean chaveValida = PrivateKeyUtil.validatePrivateKeyWithPublicKey(privateKey, publicKey);
            if (!chaveValida) {
                System.err.println("[UsuarioServico] Chave privada não corresponde à chave pública do certificado. (MSG 6007)");
                throw new SecurityException("Chave privada não corresponde ao certificado. (MSG 6007)");
            }
            System.out.println("[UsuarioServico] Chave privada validada com sucesso.");

            nomeDoCertificado = CertificateUtil.extractCNFromCertificate(certificate);
            emailDoCertificado = CertificateUtil.extractEmailFromCertificate(certificate);
            if (emailDoCertificado == null || emailDoCertificado.trim().isEmpty()) {
                System.err.println("[UsuarioServico] Não foi possível extrair o e-mail do certificado.");
                throw new RuntimeException("Não foi possível extrair o e-mail do certificado do administrador.");
            }
            if (nomeDoCertificado == null || nomeDoCertificado.trim().isEmpty()) {
                System.err.println("[UsuarioServico] Não foi possível extrair o Nome Comum (CN) do certificado. Usando o email como nome.");
                nomeDoCertificado = emailDoCertificado; 
            }
             System.out.println("[UsuarioServico] Dados extraídos do certificado - Nome: " + nomeDoCertificado + ", Email: " + emailDoCertificado);

            if (emailParaIdMap.containsKey(emailDoCertificado)) {
                System.err.println("[UsuarioServico] Email extraído do certificado já existe no sistema: " + emailDoCertificado);
                throw new EmailJaExisteException("Email do certificado ('" + emailDoCertificado + "') já cadastrado.");
            }

            certificadoPem = CertificateUtil.convertToPem(certificate);
            if (certificadoPem == null) {
                 System.err.println("[UsuarioServico] Falha ao converter certificado para o formato PEM.");
                throw new RuntimeException("Falha ao converter certificado para formato PEM.");
            }

        } catch (CertificateException | IOException e) {
            System.err.println("[UsuarioServico] Erro ao processar certificado/chave: " + e.getMessage());
            throw new Exception("Erro ao processar arquivos de certificado ou chave privada: " + e.getMessage(), e);
        } catch (SecurityException e) { 
             throw e; 
        } catch (Exception e) {
            System.err.println("[UsuarioServico] Erro inesperado durante o setup criptográfico do admin: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erro inesperado no setup do administrador: " + e.getMessage(), e);
        }

        Usuario adminUsuario = new Usuario();
        adminUsuario.setNome(nomeDoCertificado);
        adminUsuario.setEmail(emailDoCertificado);
        adminUsuario.setGrupo("Administrador"); 

        try {
            String hashedSenhaPessoal = PasswordUtil.hashPassword(senhaPessoal);
            adminUsuario.setSenha(hashedSenhaPessoal);
            System.out.println("[UsuarioServico] Senha pessoal do admin hasheada.");

            String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
            SecretKey aesKeyForTotp = AESUtil.generateKeyFromSecret(senhaPessoal, 256);
            byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), aesKeyForTotp);
            adminUsuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
            System.out.println("[UsuarioServico] Chave TOTP do admin gerada ("+ chaveSecretaTotpBase32 +") e criptografada.");

        } catch (Exception e) {
            System.err.println("[UsuarioServico] Erro ao configurar senha ou TOTP para o admin: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erro ao configurar credenciais do administrador: " + e.getMessage(), e);
        }
        
        long idGerado = proximoId.getAndIncrement();
        adminUsuario.setId(idGerado);
        adminUsuario.setKid(idGerado); // KID é Long, igual ao ID do usuário

        usuariosMap.put(idGerado, adminUsuario);
        emailParaIdMap.put(adminUsuario.getEmail(), idGerado);
        System.out.println("[UsuarioServico] Usuário admin salvo no mapa de usuários: " + adminUsuario.getEmail());

        // Armazenar no Chaveiro (Simulado)
        // Agora o construtor de DadosChaveiro e o put no chaveiroMap usam Long para o KID
        DadosChaveiro dadosChaveiroAdmin = new DadosChaveiro(adminUsuario.getKid(), certificadoPem, chavePrivadaOriginalCriptografadaBytes);
        chaveiroMap.put(adminUsuario.getKid(), dadosChaveiroAdmin);
        System.out.println("[UsuarioServico] Certificado PEM e chave privada original criptografada do admin armazenados no chaveiro (simulado) com KID: " + adminUsuario.getKid());
        
        this.completeAdminFirstSetup(adminUsuario, fraseSecretaChave);
        
        System.out.println("[UsuarioServico] Setup do administrador inicial concluído com sucesso para: " + adminUsuario.getEmail());
        return adminUsuario;
    }
} 