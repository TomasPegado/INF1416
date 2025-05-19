// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.util.exception;

public class EmailJaExisteException extends RuntimeException {
    public EmailJaExisteException(String email) {
        super("Email já cadastrado: " + email);
    }
} 