package br.com.cofredigital.tecladovirtual;

import java.util.Arrays;

/**
 * Gerencia a entrada do usuário via teclado virtual.
 * Armazena a senha digitada de forma segura (char[]).
 */
public class TecladoVirtualInputHandler {
    private char[] senha;
    private int tamanho;
    private static final int TAMANHO_MAX = 64; // Limite arbitrário

    public TecladoVirtualInputHandler() {
        this.senha = new char[TAMANHO_MAX];
        this.tamanho = 0;
    }

    /**
     * Adiciona um caractere à senha.
     */
    public void adicionarChar(char c) {
        if (tamanho < TAMANHO_MAX) {
            senha[tamanho++] = c;
        }
    }

    /**
     * Remove o último caractere da senha.
     */
    public void removerUltimoChar() {
        if (tamanho > 0) {
            senha[--tamanho] = '\0';
        }
    }

    /**
     * Limpa a senha digitada.
     */
    public void limpar() {
        Arrays.fill(senha, '\0');
        tamanho = 0;
    }

    /**
     * Retorna a senha digitada até o momento.
     * IMPORTANTE: Retorne uma cópia para evitar vazamento de referência.
     */
    public char[] getSenha() {
        return Arrays.copyOf(senha, tamanho);
    }

    /**
     * Retorna o tamanho atual da senha.
     */
    public int getTamanho() {
        return tamanho;
    }
} 