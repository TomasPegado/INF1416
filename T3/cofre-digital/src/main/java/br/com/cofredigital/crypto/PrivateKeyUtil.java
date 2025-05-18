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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

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
        // Não verificar passphrase aqui, pois a Tentativa 2 (PEM puro) pode não precisar dela

        byte[] fileBytes; // Renomeado de keyFileBytes para evitar confusão com "chave" em si
        try {
            fileBytes = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("[PrivateKeyUtil] Falha ao ler o arquivo da chave privada: " + filePath + " - " + e.getMessage());
            return null; 
        }

        // Tentativa 1: Assumir que o arquivo está totalmente criptografado com AES,
        // e o conteúdo decriptografado é uma string PEM contendo a chave PKCS#8.
        // (Baseado na lógica do KeyManager.java original do usuário)
        if (passphrase != null && !passphrase.isEmpty()) {
            try {
                SecretKey aesKey = AESUtil.generateKeyFromSecret(passphrase, 256); // Roteiro especifica 256 bits
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, aesKey);
                byte[] decryptedBytes = cipher.doFinal(fileBytes);
                
                String pemString = new String(decryptedBytes, StandardCharsets.UTF_8);

                Pattern pattern = Pattern.compile(
                    "-----BEGIN (?:ENCRYPTED )?(RSA |EC |DSA |OPENSSH |PKCS8 )?PRIVATE KEY-----(.+?)-----END (?:ENCRYPTED )?(RSA |EC |DSA |OPENSSH |PKCS8 )?PRIVATE KEY-----",
                    Pattern.DOTALL
                );
                Matcher matcher = pattern.matcher(pemString);

                if (matcher.find()) {
                    String base64Block = matcher.group(2).replaceAll("\\s", "").replaceAll("\\r", "").replaceAll("\\n", "");
                    byte[] pkcs8Bytes = Base64.getDecoder().decode(base64Block);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                    
                    // Tentar adivinhar o algoritmo ou usar RSA como padrão
                    KeyFactory kf;
                    try {
                        kf = KeyFactory.getInstance("RSA", "BC"); 
                    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                        try {
                           kf = KeyFactory.getInstance("EC", "BC");
                        } catch (NoSuchAlgorithmException | NoSuchProviderException e2) {
                           kf = KeyFactory.getInstance("DSA", "BC"); // Última tentativa
                        }
                    }
                    PrivateKey resultKey = kf.generatePrivate(keySpec);
                    System.out.println("[PrivateKeyUtil] Tentativa 1 (AES Decrypt + PEM Extract): Sucesso para: " + filePath);
                    return resultKey;
                } else {
                    System.err.println("[PrivateKeyUtil] Tentativa 1 (AES Decrypt + PEM Extract) Falhou para " + filePath + ": Conteúdo decriptado não contém bloco PEM PKCS#8 reconhecível.");
                    // Não retorna, permite cair para a Tentativa 2
                }
            } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException bpe) {
                System.err.println("[PrivateKeyUtil] Tentativa 1 (AES Decrypt + PEM Extract) Falhou para " + filePath + ": Provável frase secreta incorreta ou arquivo não é AES criptografado como esperado. " + bpe.getMessage());
            } catch (Exception e) {
                System.err.println("[PrivateKeyUtil] Tentativa 1 (AES Decrypt + PEM Extract) Falhou para " + filePath + ": Erro geral. " + e.getClass().getName() + " - " + e.getMessage());
            }
        } else {
            System.out.println("[PrivateKeyUtil] Pulando Tentativa 1 (AES Decrypt + PEM Extract) para " + filePath + " porque a frase secreta está vazia ou nula.");
        }

        // Tentativa 2: Tratar o arquivo diretamente como um arquivo PEM (se a Tentativa 1 falhou ou foi pulada)
        // Esta é a lógica que loadPrivateKeyFromPEMFile usa, mas aplicada aqui aos fileBytes.
        System.out.println("[PrivateKeyUtil] Iniciando Tentativa 2 (PEM Parse Direto) para: " + filePath);
        try (StringReader stringReader = new StringReader(new String(fileBytes, StandardCharsets.UTF_8));
             PEMParser pemParser = new PEMParser(stringReader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object == null) {
                System.err.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) Falhou para " + filePath + ": Nenhum objeto PEM encontrado (arquivo pode não ser PEM textual UTF-8 ou estar vazio/corrompido).");
                // Não retorna null ainda, a falha final será impressa depois se ambas falharem.
            } else {
                System.out.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) para " + filePath + ": Objeto lido: " + object.getClass().getName());
                // ... (lógica existente para PKCS8EncryptedPrivateKeyInfo, PEMEncryptedKeyPair, PEMKeyPair, PrivateKeyInfo) ...
                // Assegure que as condições de passphrase para tipos criptografados PEM sejam verificadas aqui.
                 if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                    PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                    if (passphrase == null || passphrase.isEmpty()) {
                        System.err.println("[PrivateKeyUtil] Tentativa 2 Falhou para " + filePath + ": Senha necessária para PKCS8EncryptedPrivateKeyInfo, mas frase está vazia/nula.");
                        return null; // Retorna null aqui pois é um requisito
                    }
                    InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(passphrase.toCharArray());
                    PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                    System.out.println("[PrivateKeyUtil] Tentativa 2: Sucesso com PKCS8EncryptedPrivateKeyInfo.");
                    return converter.getPrivateKey(privateKeyInfo);
                } else if (object instanceof PEMEncryptedKeyPair) {
                    PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) object;
                    if (passphrase == null || passphrase.isEmpty()) {
                        System.err.println("[PrivateKeyUtil] Tentativa 2 Falhou para " + filePath + ": Senha necessária para PEMEncryptedKeyPair, mas frase está vazia/nula.");
                        return null;
                    }
                    PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().setProvider("BC").build(passphrase.toCharArray());
                    PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
                    System.out.println("[PrivateKeyUtil] Tentativa 2: Sucesso com PEMEncryptedKeyPair.");
                    return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
                } else if (object instanceof PEMKeyPair) { 
                    PEMKeyPair pemKeyPair = (PEMKeyPair) object;
                    System.out.println("[PrivateKeyUtil] Tentativa 2: Sucesso com PEMKeyPair (não criptografado).");
                    return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
                } else if (object instanceof PrivateKeyInfo) { 
                    PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
                    System.out.println("[PrivateKeyUtil] Tentativa 2: Sucesso com PrivateKeyInfo (PKCS#8 não criptografado em PEM).");
                    return converter.getPrivateKey(privateKeyInfo);
                } else {
                    System.err.println("[PrivateKeyUtil] Tentativa 2 Falhou para " + filePath + ": Formato de objeto PEM não suportado: " + object.getClass().getName());
                    // Não retorna null, deixa cair para a mensagem de falha geral
                }
            }
        } catch (OperatorCreationException oce) { 
            System.err.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) Falhou para " + filePath + ": OperatorCreationException (senha incorreta para PEM criptografado / formato incompatível?). " + oce.getMessage());
        } catch (PKCSException pkcse) { 
            System.err.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) Falhou para " + filePath + ": PKCSException (formato PEM inválido?). " + pkcse.getMessage());
        } catch (IOException ioe) { 
             System.err.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) Falhou para " + filePath + ": IOException no processamento. " + ioe.getMessage());
        } catch (Exception e) { 
            System.err.println("[PrivateKeyUtil] Tentativa 2 (PEM Parse Direto) Falhou para " + filePath + ": Exceção Geral. " + e.getClass().getName() + " - " + e.getMessage());
        }

        System.err.println("[PrivateKeyUtil] Todas as tentativas de carregar a chave privada falharam para: " + filePath);
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

    /**
     * Carrega uma chave privada a partir de bytes que representam o conteúdo de um arquivo de chave.
     * Este método assume que os bytes podem representar um formato PEM (potencialmente criptografado)
     * que o PEMParser do BouncyCastle pode processar.
     *
     * @param keyFileBytes Os bytes do arquivo de chave privada.
     * @param passphrase A frase secreta para decriptografia, se a chave PEM estiver criptografada.
     * @return Um objeto PrivateKey, ou null se o carregamento falhar.
     */
    public static PrivateKey loadEncryptedPKCS8PrivateKeyFromBytes(byte[] keyFileBytes, String passphrase) {
        if (keyFileBytes == null || keyFileBytes.length == 0) {
            System.err.println("[PrivateKeyUtil] Bytes da chave privada não podem ser nulos ou vazios.");
            return null;
        }
        if (passphrase == null) { 
            System.err.println("[PrivateKeyUtil] Frase secreta não pode ser nula (pode ser string vazia se a chave não for criptografada ou o formato PEM não a exigir explictamente).");
            return null;
        }

        // Tenta interpretar os bytes diretamente como um arquivo PEM e usar PEMParser
        // Isso lida com casos onde keyFileBytes é um arquivo PEM (possivelmente criptografado internamente pelo PEM).
        // System.out.println("[PrivateKeyUtil] Tentando carregar chave usando PEMParser diretamente nos bytes fornecidos.");
        try (StringReader stringReader = new StringReader(new String(keyFileBytes, StandardCharsets.UTF_8));
             PEMParser pemParser = new PEMParser(stringReader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object == null) {
                System.err.println("[PrivateKeyUtil] Falhou: Nenhum objeto PEM encontrado nos bytes fornecidos (o arquivo pode não ser PEM textual UTF-8 ou estar vazio/corrompido).");
                return null;
            }
            
            // System.out.println("[PrivateKeyUtil] Objeto lido pelo PEMParser: " + object.getClass().getName());

            if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                // Mesmo que a passphrase seja vazia, JceOpenSSLPKCS8DecryptorProviderBuilder pode lidar com isso ou lançar exceção se a chave estiver de fato criptografada.
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(passphrase.toCharArray());
                PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
                System.out.println("[PrivateKeyUtil] Sucesso com PKCS8EncryptedPrivateKeyInfo.");
                return converter.getPrivateKey(privateKeyInfo);
            } else if (object instanceof PEMEncryptedKeyPair) {
                PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) object;
                // PEMEncryptedKeyPair geralmente requer uma passphrase não vazia se estiver realmente criptografado.
                PEMDecryptorProvider decryptorProvider = new JcePEMDecryptorProviderBuilder().setProvider("BC").build(passphrase.toCharArray());
                PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
                System.out.println("[PrivateKeyUtil] Sucesso com PEMEncryptedKeyPair.");
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
            } else if (object instanceof PEMKeyPair) { 
                PEMKeyPair pemKeyPair = (PEMKeyPair) object;
                System.out.println("[PrivateKeyUtil] Sucesso com PEMKeyPair (não criptografado).");
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo) { 
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) object;
                System.out.println("[PrivateKeyUtil] Sucesso com PrivateKeyInfo (PKCS#8 não criptografado em PEM).");
                return converter.getPrivateKey(privateKeyInfo);
            } else {
                System.err.println("[PrivateKeyUtil] Falhou: Formato de objeto PEM não suportado: " + object.getClass().getName());
                return null;
            }
        } catch (IOException ioe) { 
             System.err.println("[PrivateKeyUtil] Falhou (IOException no processamento PEM): " + ioe.getMessage());
        } catch (OperatorCreationException oce) { 
            System.err.println("[PrivateKeyUtil] Falhou (OperatorCreationException na descriptografia PEM - senha incorreta ou formato incompatível?): " + oce.getMessage());
        } catch (PKCSException pkcse) { 
            System.err.println("[PrivateKeyUtil] Falhou (PKCSException no processamento PEM): " + pkcse.getMessage());
        } catch (Exception e) { 
            System.err.println("[PrivateKeyUtil] Falhou (Exceção Geral no processamento PEM): " + e.getClass().getName() + " - " + e.getMessage());
            // e.printStackTrace(); // Descomentar para debug detalhado da falha do PEMParser
        }

        System.err.println("[PrivateKeyUtil] Falha ao carregar a chave privada usando a abordagem PEMParser.");
        return null;
    }

    /**
     * Carrega uma chave pública de um arquivo de certificado (X.509/PEM).
     * @param certFilePath caminho do arquivo do certificado público
     * @return PublicKey ou null em caso de erro
     */
    public static java.security.PublicKey loadPublicKeyFromCertificateFile(String certFilePath) {
        if (certFilePath == null || certFilePath.trim().isEmpty()) {
            System.err.println("Caminho do arquivo do certificado não pode ser nulo ou vazio.");
            return null;
        }
        try (org.bouncycastle.openssl.PEMParser pemParser = new org.bouncycastle.openssl.PEMParser(new java.io.FileReader(certFilePath))) {
            Object object = pemParser.readObject();
            if (object instanceof org.bouncycastle.cert.X509CertificateHolder) {
                org.bouncycastle.cert.X509CertificateHolder certHolder = (org.bouncycastle.cert.X509CertificateHolder) object;
                java.security.cert.X509Certificate cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
                return cert.getPublicKey();
            } else {
                System.err.println("Certificado X.509 não encontrado no arquivo: " + certFilePath);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar chave pública do certificado: " + certFilePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Gera uma chave simétrica AES a partir de uma senha/frase secreta.
     * @param passphrase senha/frase secreta
     * @param keySizeBits tamanho da chave em bits (ex: 256)
     * @return SecretKey ou null em caso de erro
     */
    public static javax.crypto.SecretKey generateAESKeyFromPassphrase(String passphrase, int keySizeBits) {
        try {
            return AESUtil.generateKeyFromSecret(passphrase, keySizeBits);
        } catch (Exception e) {
            System.err.println("Erro ao gerar chave AES a partir da frase secreta: " + e.getMessage());
            return null;
        }
    }

    /**
     * Carrega uma chave privada PKCS#8 DER binária criptografada (não PEM) a partir de bytes.
     * @param derBytes bytes do arquivo PKCS#8 DER criptografado
     * @param passphrase frase secreta para decriptografia
     * @return PrivateKey ou null em caso de erro
     */
    public static PrivateKey loadEncryptedPKCS8PrivateKeyFromDERBytes(byte[] derBytes, String passphrase) {
        if (derBytes == null || derBytes.length == 0) {
            System.err.println("[PrivateKeyUtil] Bytes DER da chave privada não podem ser nulos ou vazios.");
            return null;
        }
        if (passphrase == null || passphrase.isEmpty()) {
            System.err.println("[PrivateKeyUtil] Frase secreta não pode ser nula ou vazia para chave PKCS#8 DER criptografada.");
            return null;
        }
        try {
            org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo encryptedInfo = new org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo(derBytes);
            org.bouncycastle.operator.InputDecryptorProvider decryptorProvider =
                new org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(passphrase.toCharArray());
            org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
            return new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().setProvider("BC").getPrivateKey(privateKeyInfo);
        } catch (Exception e) {
            System.err.println("[PrivateKeyUtil] Falha ao decriptografar chave PKCS#8 DER: " + e.getMessage());
            return null;
        }
    }
} 