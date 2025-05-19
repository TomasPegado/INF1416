// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.modelo;

import java.util.Arrays;

public class Chaveiro {
    private int kid;
    private long uid; // ID do usuário associado
    private String certificadoPem;
    private byte[] chavePrivadaCriptografada;

    public Chaveiro() {
    }

    public Chaveiro(int kid, long uid, String certificadoPem, byte[] chavePrivadaCriptografada) {
        this.kid = kid;
        this.uid = uid;
        this.certificadoPem = certificadoPem;
        this.chavePrivadaCriptografada = chavePrivadaCriptografada;
    }

    public int getKid() {
        return kid;
    }

    public void setKid(int kid) {
        this.kid = kid;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getCertificadoPem() {
        return certificadoPem;
    }

    public void setCertificadoPem(String certificadoPem) {
        this.certificadoPem = certificadoPem;
    }

    public byte[] getChavePrivadaCriptografada() {
        return chavePrivadaCriptografada;
    }

    public void setChavePrivadaCriptografada(byte[] chavePrivadaCriptografada) {
        this.chavePrivadaCriptografada = chavePrivadaCriptografada;
    }

    @Override
    public String toString() {
        return "Chaveiro{" +
               "kid=" + kid +
               ", uid=" + uid +
               ", certificadoPem='" + certificadoPem + '\'' +
               ", chavePrivadaCriptografada(hash)=" + (chavePrivadaCriptografada != null ? Arrays.hashCode(chavePrivadaCriptografada) : "null") +
               '}';
    }
} 