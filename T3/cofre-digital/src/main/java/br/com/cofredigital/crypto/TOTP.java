package br.com.cofredigital.crypto;

import br.com.cofredigital.util.Base32; // Assuming Base32 is in this package
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.nio.ByteBuffer; // Added for counter to byte[] conversion

public class TOTP {
    private byte[] key = null;
    private long timeStepInSeconds = 30;

    // Construtor da classe. Recebe a chave secreta em BASE32 e o intervalo
    // de tempo a ser adotado (default = 30 segundos). Deve decodificar a
    // chave secreta e armazenar em key. Em caso de erro, gera Exception.
    public TOTP(String base32EncodedSecret, long timeStepInSeconds)
            throws Exception {
        // Implementation to be added
        if (base32EncodedSecret == null || base32EncodedSecret.isEmpty()) {
            throw new IllegalArgumentException("Base32 encoded secret cannot be null or empty.");
        }
        Base32 base32 = new Base32(Base32.Alphabet.BASE32, false, false);
        this.key = base32.fromString(base32EncodedSecret);
        if (this.key == null) {
            throw new Exception("Failed to decode Base32 secret.");
        }
        if (timeStepInSeconds > 0) {
            this.timeStepInSeconds = timeStepInSeconds;
        }
    }

    // Construtor que usa o timeStep padrão de 30 segundos
    public TOTP(String base32EncodedSecret) throws Exception {
        this(base32EncodedSecret, 30L);
    }

    // Recebe o HASH HMAC-SHA1 e determina o código TOTP de 6 algarismos
    // decimais, prefixado com zeros quando necessário.
    private String getTOTPCodeFromHash(byte[] hash) {
        if (hash == null || hash.length < 4) { // SHA-1 hash is 20 bytes, so this check is generous
            throw new IllegalArgumentException("Hash must not be null and have a minimum length.");
        }

        // Get the last nibble of the hash (RFC 4226, Section 5.4)
        int offset = hash[hash.length - 1] & 0x0F;

        // Ensure offset + 4 bytes is within bounds for the hash array
        if (offset + 3 >= hash.length) {
            // This case should be rare with a full SHA-1 hash but good to guard
            offset = hash.length - 4;
        }

        // Dynamically truncate the hash (RFC 4226, Section 5.3)
        // Get 4 bytes from the hash starting at the offset
        int binary = ((hash[offset] & 0x7F) << 24) |
                     ((hash[offset + 1] & 0xFF) << 16) |
                     ((hash[offset + 2] & 0xFF) << 8) |
                     (hash[offset + 3] & 0xFF);

        // Get the 6-digit code (RFC 4226, Section 5.3, Step 3)
        int otp = binary % 1000000; // 1,000,000 for 6 digits

        // Format as a 6-digit string, pre-padding with zeros if necessary
        return String.format("%06d", otp);
    }

    // Recebe o contador e a chave secreta para produzir o hash HMAC-SHA1.
    private byte[] HMAC_SHA1(long counter, byte[] keyByteArray)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (keyByteArray == null) {
            throw new InvalidKeyException("Key cannot be null.");
        }
        // Convert counter to byte array (8 bytes, big-endian)
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, counter);
        byte[] counterBytes = buffer.array();

        // Create an HmacSHA1 Mac instance
        Mac hmacSha1 = Mac.getInstance("HmacSHA1");

        // Create secret key spec
        SecretKeySpec secretKey = new SecretKeySpec(keyByteArray, "HmacSHA1");

        // Initialize Mac with key
        hmacSha1.init(secretKey);

        // Compute HMAC
        return hmacSha1.doFinal(counterBytes);
    }

    // Recebe o intervalo de tempo e executa o algoritmo TOTP para produzir
    // o código TOTP. Usa os métodos auxiliares getTOTPCodeFromHash e HMAC_SHA1.
    private String TOTPCode(long timeInterval) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        // 1. Gerar o hash HMAC-SHA1 usando o timeInterval como contador e a chave secreta da instância
        byte[] hash = HMAC_SHA1(timeInterval, this.key);

        // 2. Converter o hash para o código TOTP de 6 dígitos
        return getTOTPCodeFromHash(hash);
    }

    // Método que é utilizado para solicitar a geração do código TOTP.
    public String generateCode() 
            throws NoSuchAlgorithmException, InvalidKeyException {
        // Calcular o timeInterval atual
        // C_T = floor( (CurrentUnixTime - T0) / X )
        // Onde T0 é 0 (época Unix) e X é timeStepInSeconds
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long timeInterval = currentTimeSeconds / this.timeStepInSeconds;

        // Gerar e retornar o código TOTP para o timeInterval atual
        return TOTPCode(timeInterval);
    }

    // Método que é utilizado para validar um código TOTP (inputTOTP).
    // Deve considerar um atraso ou adiantamento de 30 segundos no
    // relógio da máquina que gerou o código TOTP.
    public boolean validateCode(String inputTOTP) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (inputTOTP == null || !inputTOTP.matches("^\\d{6}$")) {
            // O código de entrada deve ser uma string de 6 dígitos
            return false; 
        }

        // Calcular o timeInterval atual
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long currentInterval = currentTimeSeconds / this.timeStepInSeconds;

        // Verificar o código para o intervalo atual, o anterior e o próximo
        // para tolerar pequenas dessincronias de relógio.
        for (int i = -1; i <= 1; i++) {
            long intervalToTest = currentInterval + i;
            String generatedCode = TOTPCode(intervalToTest);
            if (generatedCode != null && generatedCode.equals(inputTOTP)) {
                return true; // Código válido encontrado
            }
        }

        return false; // Código não corresponde a nenhum na janela de tempo
    }
} 