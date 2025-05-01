package br.com.cofredigital.autenticacao.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginRequest {
    private String email;
    private String sessaoId;
    private List<String> sequenciaTeclas;
} 