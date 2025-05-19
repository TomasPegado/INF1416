// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.util.exception;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(String email) {
        super("Usuário não encontrado com o email: " + email);
    }
    
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário não encontrado com o ID: " + id);
    }
} 