package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import br.com.cofredigital.crypto.PasswordUtil;
import br.com.cofredigital.crypto.AESUtil;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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

    public UsuarioServico(TotpServico totpServico) {
        this.totpServico = totpServico;
    }

    public Usuario cadastrarUsuario(Usuario usuario, String senhaOriginal) throws Exception {
        if (emailParaIdMap.containsKey(usuario.getEmail())) {
            throw new EmailJaExisteException(usuario.getEmail());
        }
        
        usuario.setSenha(PasswordUtil.hashPassword(senhaOriginal));
        
        String chaveSecretaTotpBase32 = totpServico.gerarChaveSecreta();
        
        SecretKey chaveAES = AESUtil.generateAESKeyFromPassphrase(senhaOriginal, 256);
        
        byte[] chaveTotpCriptografadaBytes = AESUtil.encrypt(chaveSecretaTotpBase32.getBytes(StandardCharsets.UTF_8), chaveAES);
        
        usuario.setChaveSecretaTotp(Base64.getEncoder().encodeToString(chaveTotpCriptografadaBytes));
        
        long idGerado = proximoId.getAndIncrement();
        usuario.setId(idGerado);
        usuariosMap.put(idGerado, usuario);
        emailParaIdMap.put(usuario.getEmail(), idGerado);
        
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
        SecretKey chaveAES = AESUtil.generateAESKeyFromPassphrase(senhaCandidata, 256);
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
} 