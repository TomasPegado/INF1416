package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import br.com.cofredigital.crypto.PasswordUtil;
import br.com.cofredigital.crypto.AESUtil;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;
import br.com.cofredigital.tecladovirtual.TecladoVirtualAuthInput;

public class UsuarioServico implements TecladoVirtualAuthInput {

    private final Map<Long, Usuario> usuariosMap = new ConcurrentHashMap<>();
    private final Map<String, Long> emailParaIdMap = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);
    
    private final TotpServico totpServico;

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
            throw new UsuarioNaoEncontradoException(email);
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

    public String obterChaveTotpDescriptografada(Usuario usuario, String senha) throws Exception {
        // Exemplo: descriptografa a chave TOTP usando a senha do usuário
        // Ajuste conforme sua lógica real de criptografia
        javax.crypto.SecretKey chaveAES = br.com.cofredigital.crypto.AESUtil.generateAESKeyFromPassphrase(senha, 256);
        byte[] chaveTotpCriptografada = java.util.Base64.getDecoder().decode(usuario.getChaveSecretaTotp());
        byte[] chaveTotpBytes = br.com.cofredigital.crypto.AESUtil.decrypt(chaveTotpCriptografada, chaveAES);
        return new String(chaveTotpBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public boolean autenticarComTecladoVirtual(String email, char[] senha) {
        if (email == null || senha == null || senha.length == 0) return false;
        Usuario usuario;
        try {
            usuario = buscarPorEmail(email);
        } catch (Exception e) {
            return false;
        }
        // Converte char[] para String apenas aqui, para compatibilidade com PasswordUtil
        boolean ok = br.com.cofredigital.crypto.PasswordUtil.checkPassword(new String(senha), usuario.getSenha());
        // Limpa o array de senha por segurança
        java.util.Arrays.fill(senha, '\0');
        return ok;
    }
} 