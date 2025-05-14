package br.com.cofredigital.tecladovirtual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Responsável por gerar e embaralhar o layout das teclas do teclado virtual.
 * Permite customização do conjunto de teclas.
 */
public class TecladoVirtualLogic {
    private final List<Character> teclas;
    private final List<Character> layoutAtual;

    public TecladoVirtualLogic(List<Character> teclasBase) {
        this.teclas = new ArrayList<>(teclasBase);
        this.layoutAtual = new ArrayList<>(teclasBase);
        embaralharTeclas();
    }

    /**
     * Embaralha o layout das teclas.
     */
    public void embaralharTeclas() {
        Collections.shuffle(layoutAtual);
    }

    /**
     * Retorna o layout atual das teclas (embaralhado).
     */
    public List<Character> getLayoutAtual() {
        return new ArrayList<>(layoutAtual);
    }

    /**
     * Retorna o conjunto base de teclas (sem embaralhar).
     */
    public List<Character> getTeclasBase() {
        return new ArrayList<>(teclas);
    }
} 