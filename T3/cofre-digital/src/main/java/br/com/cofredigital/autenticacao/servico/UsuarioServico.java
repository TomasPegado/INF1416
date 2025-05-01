package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.repositorio.UsuarioRepositorio;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServico {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final TotpServico totpServico;

    @Autowired
    public UsuarioServico(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder, TotpServico totpServico) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.totpServico = totpServico;
    }

    public Usuario cadastrarUsuario(Usuario usuario) {
        if (usuarioRepositorio.existsByEmail(usuario.getEmail())) {
            throw new EmailJaExisteException(usuario.getEmail());
        }
        
        // Criptografar a senha
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        
        // Gerar chave secreta para TOTP
        usuario.setChaveSecretaTotp(totpServico.gerarChaveSecreta());
        
        return usuarioRepositorio.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepositorio.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepositorio.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(email));
    }

    public List<Usuario> listarTodos() {
        return usuarioRepositorio.findAll();
    }

    public Usuario atualizar(Usuario usuario) {
        // Verifica se o usuário existe
        buscarPorId(usuario.getId());
        
        return usuarioRepositorio.save(usuario);
    }

    public void desativar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(false);
        usuarioRepositorio.save(usuario);
    }

    public void ativar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(true);
        usuarioRepositorio.save(usuario);
    }
} 