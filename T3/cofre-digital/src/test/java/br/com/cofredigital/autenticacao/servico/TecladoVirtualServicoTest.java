package br.com.cofredigital.autenticacao.servico;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TecladoVirtualServicoTest {

    @InjectMocks
    private TecladoVirtualServico tecladoVirtualServico;

    @Test
    void deveGerarLayoutAleatorio() {
        Map<String, List<Character>> layout = tecladoVirtualServico.gerarLayoutAleatorio();
        
        assertNotNull(layout);
        assertFalse(layout.isEmpty());
        
        // Verifica se todas as teclas estão presentes
        assertEquals(10, layout.size());
        
        // Verifica se cada tecla tem caracteres associados
        for (List<Character> caracteres : layout.values()) {
            assertFalse(caracteres.isEmpty());
        }
    }

    @Test
    void deveGerarLayoutsDiferentes() {
        Map<String, List<Character>> layout1 = tecladoVirtualServico.gerarLayoutAleatorio();
        Map<String, List<Character>> layout2 = tecladoVirtualServico.gerarLayoutAleatorio();
        
        // É altamente improvável que dois layouts aleatórios sejam idênticos
        assertNotEquals(layout1, layout2);
    }

    @Test
    void deveProcessarEntradaCorretamente() {
        // Simula um layout onde a tecla "1" contém 'a', 'b', 'c'
        Map<String, List<Character>> layout = Map.of(
            "1", List.of('a', 'b', 'c'),
            "2", List.of('d', 'e', 'f')
        );
        
        // Simula sequência de teclas pressionadas: "1" (3 vezes) e "2" (1 vez)
        List<String> sequenciaTeclas = List.of("1", "1", "1", "2");
        
        String resultado = tecladoVirtualServico.processarEntrada(layout, sequenciaTeclas);
        
        assertEquals("cd", resultado);
    }
} 