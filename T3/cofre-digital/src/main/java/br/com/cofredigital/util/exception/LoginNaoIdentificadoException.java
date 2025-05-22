// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.util.exception;

public class LoginNaoIdentificadoException extends Exception {
    public LoginNaoIdentificadoException(String email) {
        super("Login (email) não identificado: " + email);
    }
} 