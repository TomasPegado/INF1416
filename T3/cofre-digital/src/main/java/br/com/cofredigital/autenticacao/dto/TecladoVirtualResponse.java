package br.com.cofredigital.autenticacao.dto;

import lombok.Data;

import java.util.Set;

@Data
public class TecladoVirtualResponse {
    private String sessaoId;
    private Set<String> teclas;
} 