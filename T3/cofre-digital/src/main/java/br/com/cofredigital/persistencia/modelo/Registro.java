// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.modelo;

import java.time.LocalDateTime;

public class Registro {
    private long rid;
    private LocalDateTime dataHora;
    private int mid;
    private Long uid; // Pode ser nulo para eventos do sistema
    private String detalhesAdicionais;

    public Registro() {
    }

    public Registro(long rid, LocalDateTime dataHora, int mid, Long uid, String detalhesAdicionais) {
        this.rid = rid;
        this.dataHora = dataHora;
        this.mid = mid;
        this.uid = uid;
        this.detalhesAdicionais = detalhesAdicionais;
    }

    // Construtor sem RID, pois será gerado pelo banco
    public Registro(LocalDateTime dataHora, int mid, Long uid, String detalhesAdicionais) {
        this.dataHora = dataHora;
        this.mid = mid;
        this.uid = uid;
        this.detalhesAdicionais = detalhesAdicionais;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getDetalhesAdicionais() {
        return detalhesAdicionais;
    }

    public void setDetalhesAdicionais(String detalhesAdicionais) {
        this.detalhesAdicionais = detalhesAdicionais;
    }

    @Override
    public String toString() {
        return "Registro{" +
               "rid=" + rid +
               ", dataHora=" + dataHora +
               ", mid=" + mid +
               ", uid=" + uid +
               ", detalhesAdicionais='" + detalhesAdicionais + '\'' +
               '}';
    }
} 