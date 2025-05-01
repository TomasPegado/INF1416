package br.com.cofredigital.autenticacao.servico;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.SecretGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpServicoTest {

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private CodeVerifier codeVerifier;

    @InjectMocks
    private TotpServico totpServico;

    @Test
    void deveGerarChaveSecreta() {
        when(secretGenerator.generate()).thenReturn("ABCDEFGHIJKLMNOP");

        String chaveSecreta = totpServico.gerarChaveSecreta();

        assertEquals("ABCDEFGHIJKLMNOP", chaveSecreta);
        verify(secretGenerator).generate();
    }

    @Test
    void deveValidarCodigoCorretamente() {
        when(codeVerifier.isValidCode(anyString(), anyString())).thenReturn(true);

        boolean resultado = totpServico.validarCodigo("ABCDEFGHIJKLMNOP", "123456");

        assertTrue(resultado);
        verify(codeVerifier).isValidCode("ABCDEFGHIJKLMNOP", "123456");
    }

    @Test
    void deveGerarUrlParaQRCode() {
        String url = totpServico.gerarUrlQRCode("ABCDEFGHIJKLMNOP", "usuario@example.com");
        
        assertNotNull(url);
        assertTrue(url.contains("usuario@example.com"));
        assertTrue(url.contains("ABCDEFGHIJKLMNOP"));
    }
} 