package br.com.cofredigital.autenticacao.modelo;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void deveCriarUsuarioComSucesso() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("senha123");
        usuario.setChaveSecretaTotp("ABCDEFGHIJKLMNOP");
        usuario.setAtivo(true);

        assertEquals(1L, usuario.getId());
        assertEquals("Teste", usuario.getNome());
        assertEquals("teste@example.com", usuario.getEmail());
        assertEquals("senha123", usuario.getSenha());
        assertEquals("ABCDEFGHIJKLMNOP", usuario.getChaveSecretaTotp());
        assertTrue(usuario.isAtivo());
    }

    @Test
    void deveVerificarSenhaCorretamente() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaCriptografada = encoder.encode("senha123");
        
        Usuario usuario = new Usuario();
        usuario.setSenha(senhaCriptografada);
        
        assertTrue(encoder.matches("senha123", usuario.getSenha()));
        assertFalse(encoder.matches("senhaErrada", usuario.getSenha()));
    }
} 