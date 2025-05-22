// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.autenticacao.modelo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private Long id;

    private String nome;

    private String email;

    private String senha;

    private String chaveSecretaTotp;

    private int tentativasFalhasSenha = 0;
    private int tentativasFalhasToken = 0;
    private LocalDateTime bloqueadoAte;

    private String grupo;
    private Integer kid;
    private int totalAcessos = 0;
    private boolean contaAtiva = true;

    private LocalDateTime dataCriacao = LocalDateTime.now();
    private LocalDateTime ultimoAcesso;

    public boolean isAcessoBloqueado() {
        return this.bloqueadoAte != null && LocalDateTime.now().isBefore(this.bloqueadoAte);
    }

    public void registrarFalhaSenha() {
        this.tentativasFalhasSenha++;
    }

    public void registrarFalhaToken() {
        this.tentativasFalhasToken++;
    }

    public void resetarTodosOsContadoresDeFalha() {
        this.tentativasFalhasSenha = 0;
        this.tentativasFalhasToken = 0;
    }

    public void resetarContadorFalhaSenha() {
        this.tentativasFalhasSenha = 0;
    }

    public void resetarContadorFalhaToken() {
        this.tentativasFalhasToken = 0;
    }

    public void bloquearAcessoPorMinutos(int minutos) {
        this.bloqueadoAte = LocalDateTime.now().plusMinutes(minutos);
    }

    public void desbloquearAcesso() {
        this.bloqueadoAte = null;
    }
    
    public void incrementarTotalAcessos() {
        this.totalAcessos++;
    }

    public Integer getKid() {
        return kid;
    }

    public void setKid(Integer kid) {
        this.kid = kid;
    }
} 