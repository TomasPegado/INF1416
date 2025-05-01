package br.com.cofredigital.util.exception;

public class EmailJaExisteException extends RuntimeException {
    public EmailJaExisteException(String email) {
        super("Email já cadastrado: " + email);
    }
} 