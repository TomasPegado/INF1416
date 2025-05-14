package br.com.cofredigital.tecladovirtual;

/**
 * Interface para integração do teclado virtual com o backend de autenticação.
 * Garante que apenas entradas do teclado virtual sejam aceitas.
 */
public interface TecladoVirtualAuthInput {
    /**
     * Recebe o email e a senha digitada pelo teclado virtual para autenticação.
     * @param email email do usuário
     * @param senha char[] com a senha digitada
     * @return true se a autenticação for bem-sucedida, false caso contrário
     */
    boolean autenticarComTecladoVirtual(String email, char[] senha);
} 