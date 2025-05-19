package br.com.cofredigital.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.util.Base64;

public class ArquivoProtegidoUtil {
    /**
     * Decripta o envelope digital (.env) usando a chave privada do admin para obter a semente.
     */
    public static byte[] decriptarEnvelope(byte[] envelopeBytes, PrivateKey chavePrivadaAdmin) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, chavePrivadaAdmin);
        return cipher.doFinal(envelopeBytes);
    }

    /**
     * Gera a chave AES a partir da semente usando SHA1PRNG.
     */
    public static SecretKey gerarChaveAES(byte[] semente) throws Exception {
        SecureRandom sha1Prng = SecureRandom.getInstance("SHA1PRNG");
        sha1Prng.setSeed(semente);
        byte[] chave = new byte[32]; // AES-256
        sha1Prng.nextBytes(chave);
        return new SecretKeySpec(chave, "AES");
    }

    /**
     * Decripta o arquivo .enc usando AES/ECB/PKCS5Padding.
     */
    public static byte[] decriptarArquivoAES(byte[] dadosCriptografados, SecretKey chaveAES) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, chaveAES);
        return cipher.doFinal(dadosCriptografados);
    }

    /**
     * Verifica a assinatura digital do arquivo usando a chave pública do admin.
     */
    public static boolean verificarAssinatura(byte[] dados, byte[] assinatura, PublicKey chavePublicaAdmin) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(chavePublicaAdmin);
        sig.update(dados);
        return sig.verify(assinatura);
    }

    /**
     * Utilitário para ler todos os bytes de um arquivo.
     */
    public static byte[] lerArquivo(String caminho) throws Exception {
        return Files.readAllBytes(new File(caminho).toPath());
    }
} 