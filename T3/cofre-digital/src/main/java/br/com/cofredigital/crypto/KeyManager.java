package br.com.cofredigital.crypto;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import java.util.Base64;
import java.security.cert.X509Certificate;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerenciador de chaves criptográficas do Cofre Digital.
 * Responsável por carregar chaves privadas/públicas e gerar chaves simétricas (AES) a partir de senha.
 * Utiliza arquivos da pasta Keys e frases secretas conforme especificação do projeto.
 */
public class KeyManager {

    /**
     * Carrega uma chave privada de um arquivo (PKCS8/PEM), protegida por senha.
     * @param keyFile arquivo da chave privada
     * @param passphrase senha/frase secreta para descriptografar a chave
     * @return PrivateKey
     * @throws Exception caso ocorra erro de leitura ou descriptografia
     */
    public PrivateKey loadPrivateKey(File keyFile, String passphrase) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        // 1. Ler arquivo binário criptografado
        byte[] encryptedBytes = java.nio.file.Files.readAllBytes(keyFile.toPath());
        try {
            // 2. Derivar chave AES da frase secreta (SHA1-PRNG, 128 bits)
            SecretKey aesKey = AESUtil.generateKeyFromSecret(passphrase, 256);
            // 3. Descriptografar com AES/ECB/PKCS5
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            // Remover logs de debug sensíveis
            // String pem = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
            String pem = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);

            // 4. Extrair bloco BASE64 do PEM decriptado (regex mais robusto)
            Pattern pattern = Pattern.compile(
                "-----BEGIN ([A-Z0-9 ]+)-----([^-]+)-----END \\1-----",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            Matcher matcher = pattern.matcher(pem);
            if (matcher.find()) {
                String base64Block = matcher.group(2).replaceAll("\\s", "");
                byte[] pkcs8Bytes = java.util.Base64.getDecoder().decode(base64Block);
                // 5. Construir PrivateKey via KeyFactory
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePrivate(keySpec);
            } else {
                // Se não encontrar bloco PEM, lançar exceção clara
                throw new IOException("Conteúdo decriptado não contém bloco PEM válido: " + keyFile.getAbsolutePath());
            }
        } catch (Exception e) {
            // Fallback: tentar fluxo antigo para arquivos PEM/PKCS8 não criptografados ou PEMs legados
            try (org.bouncycastle.openssl.PEMParser pemParser = new org.bouncycastle.openssl.PEMParser(new java.io.FileReader(keyFile))) {
                Object object = pemParser.readObject();
                if (object == null) {
                    throw new IOException("Arquivo de chave não contém um bloco PEM válido: " + keyFile.getAbsolutePath(), e);
                }
                org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().setProvider("BC");
                try {
                    if (object instanceof org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) {
                        org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo encInfo = (org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) object;
                        org.bouncycastle.operator.InputDecryptorProvider decProv = new org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder().build(passphrase.toCharArray());
                        org.bouncycastle.asn1.pkcs.PrivateKeyInfo pkInfo = encInfo.decryptPrivateKeyInfo(decProv);
                        return converter.getPrivateKey(pkInfo);
                    } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                        return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
                    } else if (object instanceof org.bouncycastle.openssl.PEMEncryptedKeyPair) {
                        org.bouncycastle.openssl.PEMEncryptedKeyPair encryptedKeyPair = (org.bouncycastle.openssl.PEMEncryptedKeyPair) object;
                        org.bouncycastle.openssl.PEMDecryptorProvider decProv =
                                new org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray());
                        org.bouncycastle.openssl.PEMKeyPair keyPair = encryptedKeyPair.decryptKeyPair(decProv);
                        return converter.getKeyPair(keyPair).getPrivate();
                    } else if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                        org.bouncycastle.openssl.PEMKeyPair keyPair = (org.bouncycastle.openssl.PEMKeyPair) object;
                        return converter.getKeyPair(keyPair).getPrivate();
                    } else {
                        throw new IOException("Formato de chave privada não suportado no arquivo PEM: " + object.getClass() + " em " + keyFile.getAbsolutePath(), e);
                    }
                } catch (org.bouncycastle.openssl.PEMException ex) {
                    throw new IOException("Falha ao processar chave privada PEM: " + keyFile.getAbsolutePath() + ". Motivo: " + ex.getMessage(), ex);
                } catch (Exception ex) {
                    throw ex; // Propaga exceções inesperadas
                }
            } catch (IOException ioex) {
                throw ioex; // Propaga IOException
            } catch (Exception ex) {
                throw new IOException("Erro inesperado ao tentar restaurar chave privada do arquivo PEM: " + keyFile.getAbsolutePath(), ex);
            }
        }
    }

    /**
     * Carrega uma chave pública de um arquivo de certificado (X.509/PEM).
     * @param certFile arquivo do certificado público
     * @return PublicKey
     * @throws Exception caso ocorra erro de leitura
     */
    public PublicKey loadPublicKey(File certFile) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try (PEMParser pemParser = new PEMParser(new FileReader(certFile))) {
            Object object = pemParser.readObject();
            if (object instanceof X509CertificateHolder) {
                X509CertificateHolder certHolder = (X509CertificateHolder) object;
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
                return cert.getPublicKey();
            } else {
                throw new IOException("Certificado X.509 não encontrado no arquivo");
            }
        }
    }

    /**
     * Gera uma chave simétrica AES a partir de uma senha/frase secreta.
     * @param passphrase senha/frase secreta
     * @param keySizeBits tamanho da chave em bits (ex: 256)
     * @return SecretKey
     * @throws Exception caso ocorra erro de geração
     */
    public SecretKey generateAESKeyFromPassphrase(String passphrase, int keySizeBits) throws Exception {
        // Pode delegar para AESUtil.generateKeyFromSecret
        return AESUtil.generateKeyFromSecret(passphrase, keySizeBits);
    }


} 