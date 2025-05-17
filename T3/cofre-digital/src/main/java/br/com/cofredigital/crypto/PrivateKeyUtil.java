package br.com.cofredigital.crypto;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public final class PrivateKeyUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private PrivateKeyUtil() {
        // Classe utilitária não deve ser instanciada
    }

    /**
     * Carrega uma chave privada de um arquivo PEM.
     * A chave pode estar criptografada (dentro do PEM) ou não.
     *
     * @param path O caminho para o arquivo PEM da chave privada.
     * @param password A senha para decriptografar a chave privada (se criptografada no PEM). Pode ser null se não criptografada.
     * @return Um objeto PrivateKey, ou null se o carregamento falhar.
     */
    public static PrivateKey loadPrivateKeyFromPEMFile(String path, String password) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Caminho do arquivo PEM da chave privada não pode ser nulo ou vazio.");
            return null;
        }

        try (FileReader fileReader = new FileReader(path);
             PEMParser pemParser = new PEMParser(fileReader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object == null) {
                System.err.println("Nenhum objeto PEM encontrado no arquivo: " + path);
                return null;
            }
            
            if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                if (password == null || password.trim().isEmpty()) {
                    System.err.println("Senha necessária para chave PKCS#8 criptografada no PEM, mas não fornecida.");
                    return null;
                }
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .build(password.toCharArray());
                PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
                return converter.getPrivateKey(privateKeyInfo);

            } else if (object instanceof PEMEncryptedKeyPair) {
                 if (password == null || password.trim().isEmpty()) {
                    System.err.println("Senha necessária para chave PEM criptografada, mas não fornecida.");
                    return null;
                }
                PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) object;
                PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder()
                        .build(password.toCharArray());
                PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());

            } else if (object instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) object;
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());

            } else if (object instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
                return converter.getPrivateKey(privateKeyInfo);
            } else {
                System.err.println("Formato de chave privada não suportado no arquivo PEM: " + object.getClass().getName());
                return null;
            }

        } catch (FileNotFoundException e) {
            System.err.println("Arquivo PEM da chave privada não encontrado: " + path + " - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de I/O ao ler o arquivo PEM da chave privada: " + path + " - " + e.getMessage());
        } catch (OperatorCreationException e) {
            System.err.println("Erro ao criar o operador de decriptografia (provavelmente senha incorreta ou algoritmo não suportado para chave no PEM): " + e.getMessage());
        } catch (PKCSException e) {
            System.err.println("Erro PKCS ao processar a chave PEM (formato inválido ou senha incorreta): " + e.getMessage());
        } catch (Exception e) { 
            System.err.println("Erro inesperado ao carregar a chave privada do arquivo PEM: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Carrega uma chave privada PKCS#8 de um arquivo binário criptografado com AES/ECB/PKCS5Padding.
     * A chave AES para decriptografia é derivada da frase secreta usando SHA1PRNG.
     * Conforme especificado no roteiro para a chave privada do administrador.
     *
     * @param filePath O caminho para o arquivo binário da chave privada criptografada.
     * @param passphrase A frase secreta para derivar a chave AES de decriptografia.
     * @return Um objeto PrivateKey, ou null se o carregamento falhar.
     */
    public static PrivateKey loadEncryptedPKCS8PrivateKey(String filePath, String passphrase) {
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Caminho do arquivo da chave privada criptografada não pode ser nulo ou vazio.");
            return null;
        }
        if (passphrase == null || passphrase.trim().isEmpty()) {
            System.err.println("Frase secreta não pode ser nula ou vazia para decriptografar a chave privada.");
            return null;
        }

        try {
            // 1. Ler todos os bytes do arquivo binário criptografado
            byte[] encryptedPkcs8Bytes = Files.readAllBytes(Paths.get(filePath));

            // 2. Derivar a chave AES de 256 bits da frase secreta usando SHA1PRNG
            SecretKey aesKey = AESUtil.generateKeyFromSecret(passphrase, 256);

            // 3. Decriptografar os bytes usando AES/ECB/PKCS5Padding
            byte[] decryptedPkcs8Bytes = AESUtil.decrypt(encryptedPkcs8Bytes, aesKey);

            // 4. Converter os bytes PKCS#8 decriptografados para um objeto PrivateKey
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedPkcs8Bytes);
            
            // Tenta adivinhar o algoritmo da chave (RSA, EC, DSA) para o KeyFactory.
            // Isso é uma heurística. Idealmente, o algoritmo seria conhecido.
            // Para o trabalho, provavelmente será RSA.
            String keyAlgorithm = अनुमानितKeyAlgorithmFromPKCS8(decryptedPkcs8Bytes); 
            // Se não conseguir adivinhar, tentamos os mais comuns
            KeyFactory keyFactory;
            if (keyAlgorithm != null) {
                 keyFactory = KeyFactory.getInstance(keyAlgorithm, "BC"); // Tenta com o provedor BC
            } else {
                // Tentar com algoritmos comuns se a adivinhação falhar
                try {
                    keyFactory = KeyFactory.getInstance("RSA", "BC");
                } catch (NoSuchAlgorithmException | NoSuchProviderException eRSA) {
                    try {
                        keyFactory = KeyFactory.getInstance("EC", "BC");
                    } catch (NoSuchAlgorithmException | NoSuchProviderException eEC) {
                         try {
                            keyFactory = KeyFactory.getInstance("DSA", "BC");
                        } catch (NoSuchAlgorithmException | NoSuchProviderException eDSA) {
                             System.err.println("Não foi possível obter KeyFactory para RSA, EC ou DSA com provedor BC.");
                             throw eDSA; // Relança a última exceção
                        }
                    }
                }
            }
            return keyFactory.generatePrivate(keySpec);

        } catch (FileNotFoundException e) {
            System.err.println("Arquivo da chave privada criptografada não encontrado: " + filePath + " - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de I/O ao ler o arquivo da chave privada criptografada: " + filePath + " - " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algoritmo não encontrado durante o carregamento da chave privada criptografada (AES, SHA1PRNG, ou algoritmo da chave): " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.err.println("Especificação de chave inválida (PKCS8) para a chave privada decriptografada: " + e.getMessage());
        } catch (Exception e) { // Captura para erros de AESUtil (NoSuchPadding, InvalidKey, etc.) ou outros
            System.err.println("Erro ao carregar/decriptografar a chave privada PKCS#8: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Método auxiliar heurístico para tentar determinar o algoritmo da chave a partir de bytes PKCS#8
    // Isto é complexo e não 100% garantido. Para o escopo do trabalho, pode ser simplificado
    // se o tipo de chave for sempre conhecido (ex: RSA).
    private static String अनुमानितKeyAlgorithmFromPKCS8(byte[] pkcs8Bytes) {
        // Esta é uma implementação MUITO simplificada e pode não ser robusta.
        // Uma análise real dos OIDs ASN.1 dentro da estrutura PKCS#8 seria necessária.
        // Exemplo: RSA OID: 1.2.840.113549.1.1.1
        // EC OID: 1.2.840.10045.2.1
        // DSA OID: 1.2.840.10040.4.1
        // Por simplicidade, e como o projeto provavelmente usará RSA, podemos focar nisso
        // ou assumir RSA se a detecção falhar.
        // Para o objetivo aqui, vamos retornar null e deixar o chamador tentar algoritmos comuns.
        return null; 
    }


    /**
     * Valida se uma chave privada corresponde a uma chave pública.
     * Isso é feito assinando um array aleatório de 8192 bytes com a chave privada
     * e verificando a assinatura com a chave pública, conforme roteiro.
     *
     * @param privateKey A chave privada a ser validada.
     * @param publicKey A chave pública para usar na validação.
     * @return true se a chave privada corresponder à chave pública, false caso contrário.
     */
    public static boolean validatePrivateKeyWithPublicKey(PrivateKey privateKey, java.security.PublicKey publicKey) {
        if (privateKey == null || publicKey == null) {
            System.err.println("Chave privada e chave pública não podem ser nulas para validação.");
            return false;
        }

        try {
            String keyAlgorithm = publicKey.getAlgorithm();
            String signatureAlgorithm;

            if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withRSA";
            } else if ("EC".equalsIgnoreCase(keyAlgorithm) || "ECDSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withECDSA";
            } else if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withDSA";
            } else {
                System.err.println("Algoritmo de chave pública não suportado para determinar o algoritmo de assinatura confiável: " + keyAlgorithm);
                return false;
            }

            java.security.Signature sig = java.security.Signature.getInstance(signatureAlgorithm, "BC");

            // Gera um array aleatório de 8192 bytes
            byte[] testData = new byte[8192];
            SecureRandom random = new SecureRandom();
            random.nextBytes(testData);

            sig.initSign(privateKey);
            sig.update(testData);
            byte[] signatureBytes = sig.sign();

            sig.initVerify(publicKey);
            sig.update(testData);

            return sig.verify(signatureBytes);

        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Algoritmo de assinatura não encontrado: " + e.getMessage());
        } catch (java.security.NoSuchProviderException e) {
            System.err.println("Provedor BouncyCastle (BC) não encontrado: " + e.getMessage());
        } catch (java.security.InvalidKeyException e) {
            System.err.println("Chave inválida para o algoritmo de assinatura (pode ser incompatibilidade entre chave e algoritmo): " + e.getMessage());
        } catch (java.security.SignatureException e) {
            System.err.println("Erro durante a operação de assinatura ou verificação: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado durante a validação da chave privada com a pública: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Assina dados usando uma chave privada.
     *
     * @param privateKey A chave privada a ser usada para assinar.
     * @param data Os dados a serem assinados.
     * @return Um array de bytes representando a assinatura digital, ou null em caso de erro.
     */
    public static byte[] signData(PrivateKey privateKey, byte[] data) {
        if (privateKey == null || data == null) {
            System.err.println("Chave privada e dados não podem ser nulos para assinatura.");
            return null;
        }

        try {
            String keyAlgorithm = privateKey.getAlgorithm();
            String signatureAlgorithm;

            if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withRSA";
            } else if ("EC".equalsIgnoreCase(keyAlgorithm) || "ECDSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withECDSA";
            } else if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withDSA";
            } else {
                System.err.println("Algoritmo de chave privada não suportado para determinar o algoritmo de assinatura: " + keyAlgorithm);
                return null; 
            }

            java.security.Signature sig = java.security.Signature.getInstance(signatureAlgorithm, "BC");
            sig.initSign(privateKey);
            sig.update(data);
            return sig.sign();

        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Algoritmo de assinatura não encontrado: " + e.getMessage());
        } catch (java.security.NoSuchProviderException e) {
            System.err.println("Provedor BouncyCastle (BC) não encontrado: " + e.getMessage());
        } catch (java.security.InvalidKeyException e) {
            System.err.println("Chave inválida para o algoritmo de assinatura: " + e.getMessage());
        } catch (java.security.SignatureException e) {
            System.err.println("Erro durante a operação de assinatura: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao assinar dados: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Decriptografa dados usando uma chave privada (tipicamente RSA).
     *
     * @param privateKey A chave privada RSA a ser usada para decriptografia.
     * @param encryptedData Os dados criptografados (com a chave pública correspondente).
     * @return Um array de bytes representando os dados decriptografados, ou null em caso de erro.
     */
    public static byte[] decryptDataWithPrivateKey(PrivateKey privateKey, byte[] encryptedData) {
        if (privateKey == null || encryptedData == null) {
            System.err.println("Chave privada e dados criptografados não podem ser nulos para decriptografia.");
            return null;
        }

        if (!"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
            System.err.println("A decriptografia de dados com chave privada é suportada explicitamente apenas para chaves RSA neste utilitário.");
            return null;
        }

        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedData);

        } catch (javax.crypto.NoSuchPaddingException | java.security.NoSuchAlgorithmException e) {
            System.err.println("Algoritmo de decriptografia ou padding não encontrado: " + e.getMessage());
        } catch (java.security.NoSuchProviderException e) {
            System.err.println("Provedor BouncyCastle (BC) não encontrado: " + e.getMessage());
        } catch (java.security.InvalidKeyException e) {
            System.err.println("Chave inválida para decriptografia: " + e.getMessage());
        } catch (javax.crypto.IllegalBlockSizeException | javax.crypto.BadPaddingException e) {
            System.err.println("Erro durante a decriptografia (tamanho do bloco ilegal ou padding incorreto): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado ao decriptografar dados: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
} 