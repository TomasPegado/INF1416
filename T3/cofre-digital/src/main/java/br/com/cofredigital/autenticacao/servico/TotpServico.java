package br.com.cofredigital.autenticacao.servico;

import br.com.cofredigital.crypto.TOTP;
import br.com.cofredigital.util.Base32;
import java.security.SecureRandom;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class TotpServico {

    public TotpServico() {
        // Construtor limpo, inicializações da biblioteca antiga removidas
    }

    public String gerarChaveSecreta() {
        SecureRandom random = new SecureRandom();
        byte[] secretBytes = new byte[20]; // Chave secreta de 20 bytes (160 bits)
        random.nextBytes(secretBytes);

        Base32 base32Encoder = new Base32(Base32.Alphabet.BASE32, false, true); // Standard Base32, no padding, uppercase
        return base32Encoder.toString(secretBytes);
    }

    public boolean validarCodigo(String chaveSecretaBase32, String codigo)
            throws Exception { // Adicionado throws Exception para cobrir construtor de TOTP e validateCode de TOTP
        if (chaveSecretaBase32 == null || chaveSecretaBase32.isEmpty() || codigo == null || codigo.isEmpty()) {
            // Tratar argumentos inválidos, talvez lançar IllegalArgumentException
            return false;
        }
        TOTP totpGenerator = new TOTP(chaveSecretaBase32); // Usa o timeStep padrão de 30s
        return totpGenerator.validateCode(codigo);
    }

    public String gerarUrlQRCode(String chaveSecretaBase32, String email) {
        String appName = "Cofre Digital";
        String label = appName + ":" + email;
        String issuer = appName;

        try {
            // Codificar os componentes da URL para evitar problemas com caracteres especiais
            String encodedLabel = java.net.URLEncoder.encode(label, java.nio.charset.StandardCharsets.UTF_8.name());
            String encodedIssuer = java.net.URLEncoder.encode(issuer, java.nio.charset.StandardCharsets.UTF_8.name());

            // A chave secreta Base32 geralmente não precisa de URL encoding adicional,
            // pois o alfabeto Base32 é seguro para URLs, mas é bom garantir que não haja padding problemático
            // ou outros caracteres se a implementação de Base32 os introduzir.
            // O alfabeto padrão (A-Z, 2-7) é seguro.

            String otpAuthUri = String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedLabel,
                chaveSecretaBase32, // Chave Base32 já está no formato correto para a URI
                encodedIssuer
            );
            
            // Opcional: Gerar a imagem QR como Data URI
            // return gerarImagemQrCodeComoDataUri(otpAuthUri);
            
            // Por agora, vamos retornar apenas a URI otpauth:// como string.
            return otpAuthUri;

        } catch (java.io.UnsupportedEncodingException e) {
            // Esta exceção é improvável com StandardCharsets.UTF_8
            // Tratar como um erro crítico ou logar
            // System.err.println("Erro de encoding ao gerar URL QRCode: " + e.getMessage());
            throw new RuntimeException("Erro de encoding ao gerar URL para QRCode", e);
        }
    }

    // Método auxiliar para gerar imagem QR como Data URI (pode ser adaptado/removido)
    // Este método usa a biblioteca zxing que mantivemos no pom.xml
    private String gerarImagemQrCodeComoDataUri(String qrDataUri) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrDataUri, BarcodeFormat.QR_CODE, 200, 200);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            // Logar o erro ou tratar de forma apropriada
            // System.err.println("Erro ao gerar imagem QR Code: " + e.getMessage());
            return null; // Ou lançar uma exceção customizada
        }
    }
} 