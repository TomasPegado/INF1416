package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.repositorio.UsuarioRepositorio;
import br.com.cofredigital.util.exception.EmailJaExisteException;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServicoTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TotpServico totpServico;

    @InjectMocks
    private UsuarioServico usuarioServico;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("senha123");
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        when(usuarioRepositorio.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senhaCriptografada");
        when(totpServico.gerarChaveSecreta()).thenReturn("CHAVE_SECRETA");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuario);

        Usuario resultado = usuarioServico.cadastrarUsuario(usuario);

        assertNotNull(resultado);
        assertEquals("teste@example.com", resultado.getEmail());
        verify(passwordEncoder).encode("senha123");
        verify(totpServico).gerarChaveSecreta();
        verify(usuarioRepositorio).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaExiste() {
        when(usuarioRepositorio.existsByEmail(anyString())).thenReturn(true);

        assertThrows(EmailJaExisteException.class, () -> {
            usuarioServico.cadastrarUsuario(usuario);
        });

        verify(usuarioRepositorio).existsByEmail("teste@example.com");
        verify(usuarioRepositorio, never()).save(any(Usuario.class));
    }

    @Test
    void deveBuscarUsuarioPorEmail() {
        when(usuarioRepositorio.findByEmail(anyString())).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioServico.buscarPorEmail("teste@example.com");

        assertNotNull(resultado);
        assertEquals("teste@example.com", resultado.getEmail());
        verify(usuarioRepositorio).findByEmail("teste@example.com");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(usuarioRepositorio.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontradoException.class, () -> {
            usuarioServico.buscarPorEmail("naoexiste@example.com");
        });

        verify(usuarioRepositorio).findByEmail("naoexiste@example.com");
    }
} 