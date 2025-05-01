package br.com.cofredigital.autenticacao.servico;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TecladoVirtualServico {

    private static final String CARACTERES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+";
    private static final int CARACTERES_POR_TECLA = 3;

    public Map<String, List<Character>> gerarLayoutAleatorio() {
        Map<String, List<Character>> layout = new HashMap<>();
        List<Character> caracteresDisponiveis = new ArrayList<>();
        
        // Converte a string de caracteres em uma lista de caracteres
        for (char c : CARACTERES.toCharArray()) {
            caracteresDisponiveis.add(c);
        }
        
        // Embaralha a lista de caracteres
        Collections.shuffle(caracteresDisponiveis);
        
        // Distribui os caracteres pelas teclas (0-9)
        for (int i = 0; i < 10; i++) {
            String tecla = String.valueOf(i);
            List<Character> caracteresNaTecla = new ArrayList<>();
            
            // Adiciona CARACTERES_POR_TECLA caracteres para cada tecla
            for (int j = 0; j < CARACTERES_POR_TECLA && !caracteresDisponiveis.isEmpty(); j++) {
                caracteresNaTecla.add(caracteresDisponiveis.remove(0));
            }
            
            layout.put(tecla, caracteresNaTecla);
        }
        
        return layout;
    }

    public String processarEntrada(Map<String, List<Character>> layout, List<String> sequenciaTeclas) {
        StringBuilder resultado = new StringBuilder();
        String teclaPressionadaAnterior = null;
        int repeticoes = 0;
        
        for (String teclaPressionada : sequenciaTeclas) {
            if (teclaPressionada.equals(teclaPressionadaAnterior)) {
                // Mesma tecla pressionada novamente, incrementa repetições
                repeticoes++;
            } else {
                // Nova tecla pressionada, processa a anterior se existir
                if (teclaPressionadaAnterior != null) {
                    processarTecla(layout, teclaPressionadaAnterior, repeticoes, resultado);
                }
                
                // Reinicia contagem para a nova tecla
                teclaPressionadaAnterior = teclaPressionada;
                repeticoes = 0;
            }
        }
        
        // Processa a última tecla pressionada
        if (teclaPressionadaAnterior != null) {
            processarTecla(layout, teclaPressionadaAnterior, repeticoes, resultado);
        }
        
        return resultado.toString();
    }
    
    private void processarTecla(Map<String, List<Character>> layout, String tecla, int repeticoes, StringBuilder resultado) {
        List<Character> caracteresNaTecla = layout.get(tecla);
        if (caracteresNaTecla != null && !caracteresNaTecla.isEmpty()) {
            // Calcula o índice do caractere baseado no número de repetições
            int indice = repeticoes % caracteresNaTecla.size();
            resultado.append(caracteresNaTecla.get(indice));
        }
    }
    
    // Método para gerar um QR Code com o layout do teclado (para uso em aplicativos móveis)
    public String gerarQRCodeLayout(Map<String, List<Character>> layout) {
        StringBuilder layoutString = new StringBuilder();
        
        for (Map.Entry<String, List<Character>> entry : layout.entrySet()) {
            layoutString.append(entry.getKey()).append(":");
            for (Character c : entry.getValue()) {
                layoutString.append(c);
            }
            layoutString.append(";");
        }
        
        // Aqui poderia ser integrado com um gerador de QR Code
        // Por simplicidade, retornamos apenas a string do layout
        return layoutString.toString();
    }
    
    // Método para implementar medidas anti-keylogger
    public Map<String, List<Character>> gerarLayoutComMedidasAntiKeylogger() {
        Map<String, List<Character>> layout = gerarLayoutAleatorio();
        
        // Adiciona caracteres "chamariz" que serão ignorados no processamento
        // Isso dificulta a análise por keyloggers
        Random random = new Random();
        for (List<Character> caracteres : layout.values()) {
            // Adiciona um caractere chamariz aleatório à lista
            char chamariz = CARACTERES.charAt(random.nextInt(CARACTERES.length()));
            caracteres.add(chamariz);
        }
        
        return layout;
    }
} 