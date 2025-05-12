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

public class AESUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String PRNG_ALGORITHM = "SHA1PRNG"; // As per project specification

    /**
     * Gera uma chave AES a partir de uma frase secreta.
     *
     * @param passphrase A frase secreta para derivar a chave.
     * @param keySizeBits O tamanho da chave em bits (ex: 128, 192, 256).
     *                    A especificação do projeto menciona 256 bits.
     * @return A SecretKey gerada.
     * @throws NoSuchAlgorithmException Se o algoritmo AES ou SHA1PRNG não for encontrado.
     */
    public static SecretKey generateAESKeyFromPassphrase(String passphrase, int keySizeBits)
            throws NoSuchAlgorithmException {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase cannot be null or empty.");
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        
        // Usar SHA1PRNG para semear o SecureRandom, conforme especificado.
        SecureRandom secureRandom = SecureRandom.getInstance(PRNG_ALGORITHM);
        secureRandom.setSeed(passphrase.getBytes(StandardCharsets.UTF_8));
        
        keyGenerator.init(keySizeBits, secureRandom);
        return keyGenerator.generateKey();
    }

    /**
     * Criptografa dados usando AES/ECB/PKCS5Padding.
     *
     * @param data Os dados a serem criptografados.
     * @param key A SecretKey AES.
     * @return Os dados criptografados.
     * @throws NoSuchAlgorithmException Se AES não for encontrado.
     * @throws NoSuchPaddingException Se PKCS5Padding não for encontrado.
     * @throws InvalidKeyException Se a chave for inválida.
     * @throws IllegalBlockSizeException Se o tamanho do bloco for ilegal.
     * @throws BadPaddingException Se o padding for ruim.
     */
    public static byte[] encrypt(byte[] data, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                   IllegalBlockSizeException, BadPaddingException {
        if (data == null || key == null) {
            throw new IllegalArgumentException("Data and key cannot be null.");
        }
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Decripta dados usando AES/ECB/PKCS5Padding.
     *
     * @param encryptedData Os dados criptografados.
     * @param key A SecretKey AES.
     * @return Os dados decriptados.
     * @throws NoSuchAlgorithmException Se AES não for encontrado.
     * @throws NoSuchPaddingException Se PKCS5Padding não for encontrado.
     * @throws InvalidKeyException Se a chave for inválida.
     * @throws IllegalBlockSizeException Se o tamanho do bloco for ilegal.
     * @throws BadPaddingException Se o padding for ruim.
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                   IllegalBlockSizeException, BadPaddingException {
        if (encryptedData == null || key == null) {
            throw new IllegalArgumentException("Encrypted data and key cannot be null.");
        }
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }
} 