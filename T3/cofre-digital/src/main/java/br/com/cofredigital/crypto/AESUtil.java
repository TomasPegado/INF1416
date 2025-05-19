// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

public final class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding"; // Conforme roteiro para chave privada e TOTP
    // IV não é usado em ECB, mas mantido para CBC se necessário no futuro. Removido para clareza no ECB.
    // private static final byte[] IV = new byte[16]; // Um IV fixo não é seguro para CBC, aqui apenas como placeholder.

    private AESUtil() {
        // Classe utilitária
    }

    /**
     * Gera uma chave AES de um tamanho específico a partir de um segredo (senha ou frase secreta)
     * usando SHA1PRNG para semear o KeyGenerator. Conforme especificado no roteiro.
     *
     * @param secret A string secreta (senha ou frase secreta).
     * @param keySizeInBits O tamanho da chave desejada em bits (ex: 256 para o projeto).
     * @return A SecretKey gerada.
     * @throws NoSuchAlgorithmException Se o algoritmo AES ou SHA1PRNG não for encontrado.
     */
    public static SecretKey generateKeyFromSecret(String secret, int keySizeInBits)
            throws NoSuchAlgorithmException {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("O segredo não pode ser nulo ou vazio.");
        }
        // O roteiro especifica 256 bits para as chaves AES derivadas.
        if (keySizeInBits != 256) {
             // Poderia ser mais flexível, mas para o roteiro, 256 é o esperado.
            System.err.println("Atenção: O roteiro especifica chaves AES de 256 bits. Solicitado: " + keySizeInBits);
             // Mantendo a flexibilidade do método, mas com aviso.
        }
        if (keySizeInBits != 128 && keySizeInBits != 192 && keySizeInBits != 256) {
            throw new IllegalArgumentException("Tamanho de chave inválido. Deve ser 128, 192 ou 256 bits.");
        }


        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(secret.getBytes(StandardCharsets.UTF_8));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        // Para AES, o KeyGenerator ignora o SecureRandom para os bytes da chave em si se já estiver semeado com setSeed,
        // mas o SecureRandom ainda pode influenciar a escolha se não houver bytes suficientes de setSeed.
        // A inicialização com keySizeInBits e o SecureRandom é a forma correta.
        keyGenerator.init(keySizeInBits, secureRandom);

        return keyGenerator.generateKey();
    }

    /**
     * Criptografa dados usando AES/ECB/PKCS5Padding.
     *
     * @param plainData Os dados em bytes a serem criptografados.
     * @param key A chave secreta AES.
     * @return Os dados criptografados em bytes.
     * @throws NoSuchAlgorithmException Se o algoritmo não for encontrado.
     * @throws NoSuchPaddingException Se o padding não for encontrado.
     * @throws InvalidKeyException Se a chave for inválida.
     * @throws IllegalBlockSizeException Se o tamanho do bloco for ilegal.
     * @throws BadPaddingException Se o padding for ruim.
     */
    public static byte[] encrypt(byte[] plainData, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                   IllegalBlockSizeException, BadPaddingException {
        if (plainData == null || key == null) {
            throw new IllegalArgumentException("Dados e chave não podem ser nulos para criptografia.");
        }
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        // Para ECB, o IvParameterSpec não é usado/necessário.
        // Se fosse CBC: IvParameterSpec ivSpec = new IvParameterSpec(IV);
        // cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plainData);
    }

    /**
     * Decriptografa dados usando AES/ECB/PKCS5Padding.
     *
     * @param encryptedData Os dados criptografados em bytes.
     * @param key A chave secreta AES.
     * @return Os dados decriptografados em bytes.
     * @throws NoSuchAlgorithmException Se o algoritmo não for encontrado.
     * @throws NoSuchPaddingException Se o padding não for encontrado.
     * @throws InvalidKeyException Se a chave for inválida.
     * @throws IllegalBlockSizeException Se o tamanho do bloco for ilegal.
     * @throws BadPaddingException Se o padding for ruim.
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                   IllegalBlockSizeException, BadPaddingException {
        if (encryptedData == null || key == null) {
            throw new IllegalArgumentException("Dados criptografados e chave não podem ser nulos para decriptografia.");
        }
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        // Para ECB, o IvParameterSpec não é usado/necessário.
        // Se fosse CBC: IvParameterSpec ivSpec = new IvParameterSpec(IV);
        // cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }

    // Métodos utilitários para converter para Base64 String se necessário para exibição ou armazenamento textual.
    // Não diretamente pedidos para a criptografia da chave privada (binária), mas podem ser úteis.

    public static String encryptAndBase64Encode(String plainText, SecretKey key) throws Exception {
        byte[] encryptedBytes = encrypt(plainText.getBytes(StandardCharsets.UTF_8), key);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptBase64AndDecode(String encryptedTextBase64, SecretKey key) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedTextBase64);
        byte[] decryptedBytes = decrypt(encryptedBytes, key);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
} 