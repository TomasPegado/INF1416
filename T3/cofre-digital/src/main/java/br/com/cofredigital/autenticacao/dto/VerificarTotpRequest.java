package br.com.cofredigital.autenticacao.dto;

import lombok.Data;

@Data
public class VerificarTotpRequest {
    private String codigoTotp;
} 