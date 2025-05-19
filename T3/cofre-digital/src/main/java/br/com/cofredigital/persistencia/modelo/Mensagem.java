// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.modelo;

public class Mensagem {
    private int mid;
    private String textoMensagem;

    public Mensagem() {
    }

    public Mensagem(int mid, String textoMensagem) {
        this.mid = mid;
        this.textoMensagem = textoMensagem;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public String getTextoMensagem() {
        return textoMensagem;
    }

    public void setTextoMensagem(String textoMensagem) {
        this.textoMensagem = textoMensagem;
    }

    @Override
    public String toString() {
        return "Mensagem{" +
               "mid=" + mid +
               ", textoMensagem='" + textoMensagem + '\'' +
               '}';
    }
} 