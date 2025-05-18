package br.com.cofredigital.crypto;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class CertificateUtil {

    private CertificateUtil() {
        // Classe utilitária, não deve ser instanciada
    }

    /**
     * Carrega um certificado X.509 de um arquivo no formato PEM.
     *
     * @param filePath O caminho para o arquivo do certificado PEM.
     * @return Um objeto X509Certificate.
     * @throws IOException Se ocorrer um erro de I/O ao ler o arquivo.
     * @throws CertificateException Se ocorrer um erro ao processar o certificado.
     */
    public static X509Certificate loadCertificateFromFile(String filePath) 
            throws IOException, CertificateException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("O caminho do arquivo do certificado não pode ser nulo ou vazio.");
        }

        String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));

        // Remove cabeçalho, rodapé e quebras de linha do conteúdo PEM
        String base64Encoded = pemContent
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", ""); // Remove todos os espaços em branco (incluindo quebras de linha)

        byte[] decodedDerBytes = Base64.getDecoder().decode(base64Encoded);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream inStream = new ByteArrayInputStream(decodedDerBytes)) {
            return (X509Certificate) certificateFactory.generateCertificate(inStream);
        }
    }

    /**
     * Extrai o Common Name (CN) do Subject DN de um certificado X.509.
     *
     * @param cert O certificado X.509.
     * @return O valor do Common Name (CN) ou null se não encontrado.
     */
    public static String extractCNFromCertificate(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        // Obtém o Subject Distinguished Name (DN)
        String subjectDN = cert.getSubjectX500Principal().getName();

        // O DN é uma string como "CN=Nome Comum, OU=Unidade, O=Organizacao, C=BR"
        // Precisamos extrair o valor do CN.
        // Uma forma simples é usando split, mas regex pode ser mais robusto para casos complexos.
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            String trimmedComponent = component.trim();
            if (trimmedComponent.toUpperCase().startsWith("CN=")) {
                return trimmedComponent.substring(3); // Retorna a string após "CN="
            }
        }
        return null; // CN não encontrado
    }

    /**
     * Extrai o endereço de e-mail de um certificado X.509.
     * Prioriza o SubjectAlternativeName (SAN) e, como fallback, o SubjectDN.
     *
     * @param cert O certificado X.509.
     * @return O endereço de e-mail ou null se não encontrado.
     */
    public static String extractEmailFromCertificate(X509Certificate cert) {
        if (cert == null) {
            return null;
        }

        // 1. Tentar extrair do SubjectAlternativeName (SAN) - Tipo rfc822Name (tag 1)
        try {
            if (cert.getSubjectAlternativeNames() != null) {
                for (java.util.List<?> sanItem : cert.getSubjectAlternativeNames()) {
                    if (sanItem != null && sanItem.size() >= 2) {
                        Integer tag = (Integer) sanItem.get(0);
                        Object value = sanItem.get(1);
                        // Tag 1 é para rfc822Name (email)
                        if (tag != null && tag == 1 && value instanceof String) {
                            return (String) value;
                        }
                    }
                }
            }
        } catch (CertificateException e) {
            // Logar ou tratar o erro se a extensão SAN for malformada, mas continuar para o DN
            System.err.println("Erro ao processar SubjectAlternativeNames: " + e.getMessage());
        }

        // 2. Se não encontrado na SAN, tentar extrair do SubjectDN (campos E ou EMAILADDRESS)
        String subjectDN = cert.getSubjectX500Principal().getName();
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            String trimmedComponent = component.trim();
            if (trimmedComponent.toUpperCase().startsWith("E=")) {
                return trimmedComponent.substring(2);
            } else if (trimmedComponent.toUpperCase().startsWith("EMAILADDRESS=")) {
                return trimmedComponent.substring(13);
            }
        }

        return null; // Email não encontrado
    }

    /**
     * Obtém a chave pública de um certificado X.509.
     *
     * @param cert O certificado X.509.
     * @return O objeto PublicKey ou null se o certificado for null.
     */
    public static java.security.PublicKey getPublicKeyFromCertificate(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        return cert.getPublicKey();
    }
    
    /**
     * Converte um objeto X509Certificate para o formato PEM (String).
     *
     * @param cert O certificado X.509 a ser convertido.
     * @return Uma string no formato PEM representando o certificado, ou null se o certificado for null ou ocorrer um erro.
     */
    public static String convertToPem(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        try {
            java.util.Base64.Encoder encoder = java.util.Base64.getMimeEncoder(64, "\n".getBytes());
            byte[] encodedCert = cert.getEncoded();
            String b64Cert = new String(encoder.encode(encodedCert));
            return "-----BEGIN CERTIFICATE-----\n" + b64Cert + "\n-----END CERTIFICATE-----\n";
        } catch (java.security.cert.CertificateEncodingException e) {
            // Idealmente, logar a exceção
            System.err.println("Erro ao codificar o certificado para PEM: " + e.getMessage());
            return null;
        }
    }

    public static X509Certificate loadCertificateFromPEMString(String pemString) throws Exception {
        if (pemString == null || pemString.trim().isEmpty()) {
            throw new IllegalArgumentException("PEM string não pode ser nula ou vazia.");
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // A string PEM já deve estar no formato correto, incluindo BEGIN/END e base64 com quebras de linha.
        // Se ela estiver apenas o base64 puro, a lógica de leitura/decode precisa ser ajustada aqui.
        // Assumindo que pemString é o conteúdo completo do arquivo .pem
        InputStream is = new ByteArrayInputStream(pemString.getBytes(StandardCharsets.UTF_8));
        return (X509Certificate) cf.generateCertificate(is);
    }
} 