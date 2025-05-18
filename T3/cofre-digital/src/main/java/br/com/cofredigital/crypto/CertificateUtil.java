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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            System.err.println("Caminho do arquivo de certificado não pode ser nulo ou vazio.");
            throw new IllegalArgumentException("Caminho do arquivo de certificado não pode ser nulo ou vazio.");
        }

        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);

        Pattern pattern = Pattern.compile(
            "-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(fileContent);

        if (matcher.find()) {
            String pemBlock = matcher.group(1); 
            String base64Content = pemBlock.replaceAll("\\s", "").replaceAll("\\r", "").replaceAll("\\n", "");
            
            byte[] decoded = Base64.getDecoder().decode(base64Content);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
        } else {
            // Se o bloco PEM não for encontrado, tenta carregar como um fluxo de certificado direto (para DER, por exemplo)
            // Isso é uma tentativa de fallback, pode ou não ser relevante para os arquivos do professor
            try (InputStream fis = new FileInputStream(filePath)){
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                return (X509Certificate) cf.generateCertificate(fis);
            } catch (IOException | CertificateException e_fallback) {
                System.err.println("Bloco PEM de certificado não encontrado no arquivo E falha ao carregar como fluxo direto: " + filePath);
                System.err.println("Erro original da busca PEM: Nenhum bloco encontrado.");
                System.err.println("Erro do fallback (fluxo direto): " + e_fallback.getMessage());
                throw new CertificateException("Não foi possível carregar o certificado do arquivo: nem como PEM extraído, nem como fluxo direto. Arquivo: " + filePath, e_fallback);
            }
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
            System.err.println("[CertificateUtil.extractEmail] Certificado é nulo.");
            return null;
        }
        System.out.println("[CertificateUtil.extractEmail] Tentando extrair email do certificado: " + cert.getSubjectX500Principal().getName());

        // 1. Tentar extrair do SubjectAlternativeName (SAN)
        try {
            if (cert.getSubjectAlternativeNames() != null) {
                System.out.println("[CertificateUtil.extractEmail] Verificando SubjectAlternativeNames (SAN)...");
                for (java.util.List<?> sanItem : cert.getSubjectAlternativeNames()) {
                    if (sanItem != null && sanItem.size() >= 2) {
                        Integer tag = (Integer) sanItem.get(0);
                        Object value = sanItem.get(1);
                        if (tag != null && tag == 1 && value instanceof String) {
                            System.out.println("[CertificateUtil.extractEmail] Email encontrado na SAN: " + (String) value);
                            return (String) value;
                        }
                    }
                }
                System.out.println("[CertificateUtil.extractEmail] Email não encontrado na SAN após iteração.");
            } else {
                System.out.println("[CertificateUtil.extractEmail] SubjectAlternativeNames (SAN) está nulo.");
            }
        } catch (CertificateException e) {
            System.err.println("[CertificateUtil.extractEmail] Erro ao processar SubjectAlternativeNames: " + e.getMessage());
        }

        // 2. Se não encontrado na SAN, tentar extrair do SubjectDN
        String subjectDN = cert.getSubjectX500Principal().getName();
        System.out.println("[CertificateUtil.extractEmail] SubjectDN completo: '" + subjectDN + "'");
        String[] dnComponents = subjectDN.split(",");
        System.out.println("[CertificateUtil.extractEmail] SubjectDN componentes (após split por vírgula):");
        for (int i = 0; i < dnComponents.length; i++) {
            System.out.println("  Componente[" + i + "]: '" + dnComponents[i] + "'");
        }

        for (String component : dnComponents) {
            String trimmedComponent = component.trim();
            System.out.println("[CertificateUtil.extractEmail] Processando DN component (trimmed): '" + trimmedComponent + "'");
            if (trimmedComponent.toUpperCase().startsWith("E=")) {
                String email = trimmedComponent.substring(2);
                System.out.println("[CertificateUtil.extractEmail] Email encontrado por E=: " + email);
                return email;
            } else if (trimmedComponent.toUpperCase().startsWith("EMAILADDRESS=")) {
                String email = trimmedComponent.substring(13);
                System.out.println("[CertificateUtil.extractEmail] Email encontrado por EMAILADDRESS=: " + email);
                return email;
            } else if (trimmedComponent.toUpperCase().startsWith("CN=")) {
                System.out.println("[CertificateUtil.extractEmail] Componente CN encontrado: '" + trimmedComponent + "'");
                int emailMarkerPos = trimmedComponent.indexOf("/emailAddress="); 
                System.out.println("[CertificateUtil.extractEmail] Posição de '/emailAddress=' no CN: " + emailMarkerPos);
                if (emailMarkerPos != -1) {
                    String email = trimmedComponent.substring(emailMarkerPos + "/emailAddress=".length()); 
                    System.out.println("[CertificateUtil.extractEmail] Email extraído do CN via /emailAddress=: " + email);
                    return email; 
                }
            }
        }
        System.out.println("[CertificateUtil.extractEmail] Email não encontrado após análise dos componentes do SubjectDN.");

        // 3. Fallback com regex no SubjectDN completo
        System.out.println("[CertificateUtil.extractEmail] Tentando fallback com regex no SubjectDN completo...");
        Pattern emailPattern = Pattern.compile("(?:E=|EMAILADDRESS=|OID\\.1\\.2\\.840\\.113549\\.1\\.9\\.1=)([^,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(subjectDN); 
        if (matcher.find()) {
            String email = matcher.group(1);
            System.out.println("[CertificateUtil.extractEmail] Email encontrado via fallback regex: " + email);
            return email;
        }
        System.out.println("[CertificateUtil.extractEmail] Email NÃO encontrado por nenhuma estratégia.");
        return null;
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