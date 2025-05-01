package br.com.cofredigital.autenticacao.repositorio;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UsuarioRepositorioTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    void deveSalvarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("senha123");
        
        Usuario usuarioSalvo = usuarioRepositorio.save(usuario);
        
        assertNotNull(usuarioSalvo.getId());
    }

    @Test
    void deveBuscarUsuarioPorEmail() {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("senha123");
        
        entityManager.persist(usuario);
        entityManager.flush();
        
        Optional<Usuario> encontrado = usuarioRepositorio.findByEmail("teste@example.com");
        
        assertTrue(encontrado.isPresent());
        assertEquals("teste@example.com", encontrado.get().getEmail());
    }

    @Test
    void deveVerificarSeEmailExiste() {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("senha123");
        
        entityManager.persist(usuario);
        entityManager.flush();
        
        boolean existe = usuarioRepositorio.existsByEmail("teste@example.com");
        boolean naoExiste = usuarioRepositorio.existsByEmail("naoexiste@example.com");
        
        assertTrue(existe);
        assertFalse(naoExiste);
    }
} 