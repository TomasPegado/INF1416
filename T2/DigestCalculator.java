
import java.io.*;
import java.security.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class DigestCalculator {

    // Classe auxiliar para armazenar os resultados do processamento de cada
    // arquivo.
    static class FileResult {
        String fileName;
        String computedDigest;
        String status;

        FileResult(String fileName, String computedDigest, String status) {
            this.fileName = fileName;
            this.computedDigest = computedDigest;
            this.status = status;
        }
    }

    public static void main(String[] args) {
        // 1. Validação dos argumentos de linha de comando.
        if (args.length != 3) {
            System.out.println("Uso: java DigestCalculator <Tipo_Digest> <Caminho_da_Pasta> <Caminho_ArqListaDigest>");
            System.exit(1);
        }

        // 2. Extração dos parâmetros.
        String userDigestType = args[0].toUpperCase(); // p.ex.: MD5, SHA1, SHA256 ou SHA512
        String algorithm = null;
        // Mapeamento do tipo de digest informado para o nome do algoritmo conforme
        // usado pela JCA.
        if (userDigestType.equals("MD5")) {
            algorithm = "MD5";
        } else if (userDigestType.equals("SHA1") || userDigestType.equals("SHA-1")) {
            algorithm = "SHA-1";
        } else if (userDigestType.equals("SHA256") || userDigestType.equals("SHA-256")) {
            algorithm = "SHA-256";
        } else if (userDigestType.equals("SHA512") || userDigestType.equals("SHA-512")) {
            algorithm = "SHA-512";
        } else {
            System.out.println("Tipo de digest não suportado. Utilize MD5, SHA1, SHA256 ou SHA512.");
            System.exit(1);
        }

        String folderPath = args[1];
        String xmlFilePath = args[2];

        // 3. Verifica se a pasta existe e é realmente um diretório.
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("A pasta especificada não existe ou não é uma pasta: " + folderPath);
            System.exit(1);
        }

        // 4. Processamento do arquivo XML com os digests conhecidos.
        File xmlFile = new File(xmlFilePath);
        // Se o arquivo XML existir, vamos parseá-lo; caso contrário, criaremos um novo
        // documento XML.
        Document xmlDoc = null;
        Map<String, Map<String, String>> knownDigests = new HashMap<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            if (xmlFile.exists()) {
                xmlDoc = dBuilder.parse(xmlFile);
                xmlDoc.getDocumentElement().normalize();
                // Monta um mapa com as entradas conhecidas: arquivo -> (tipo de digest ->
                // valor)
                knownDigests = parseCatalog(xmlDoc);
            } else {
                // Cria um novo Document com a tag raiz <CATALOG>
                xmlDoc = dBuilder.newDocument();
                Element rootElement = xmlDoc.createElement("CATALOG");
                xmlDoc.appendChild(rootElement);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Erro ao processar o arquivo XML: " + e.getMessage());
            System.exit(1);
        }

        // 5. Processamento dos arquivos na pasta.
        File[] files = folder.listFiles();
        if (files == null) {
            System.out.println("Não há arquivos na pasta.");
            System.exit(1);
        }

        List<FileResult> results = new ArrayList<>();
        // Mapa para detectar colisões: digest calculado -> nome do arquivo
        Map<String, String> computedDigestMap = new HashMap<>();

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algoritmo de digest não encontrado: " + algorithm);
            System.exit(1);
        }

        // Para cada arquivo encontrado na pasta, calcular o digest.
        for (File file : files) {
            if (!file.isFile())
                continue; // ignora subpastas
            String fileName = file.getName();
            String computedHex = "";
            try {
                computedHex = calculateDigest(file, md);
            } catch (IOException e) {
                System.out.println("Erro ao ler arquivo " + fileName + ": " + e.getMessage());
                continue;
            }

            String status = "";
            // 6. Verificação de colisão entre os digests calculados.
            if (computedDigestMap.containsKey(computedHex) && !computedDigestMap.get(computedHex).equals(fileName)) {
                status = "COLISION";
            } else {
                computedDigestMap.put(computedHex, fileName);
                // Verifica se o arquivo já possui um registro conhecido no XML.
                if (knownDigests.containsKey(fileName)) {
                    Map<String, String> fileDigests = knownDigests.get(fileName);
                    if (fileDigests.containsKey(userDigestType)) {
                        String knownDigest = fileDigests.get(userDigestType);
                        if (computedHex.equalsIgnoreCase(knownDigest.trim())) {
                            status = "OK";
                        } else {
                            status = "NOT OK";
                        }
                    } else {
                        status = "NOT FOUND";
                    }
                } else {
                    status = "NOT FOUND";
                }
            }

            // Verifica se a digest calculada colide com algum digest registrado de outro
            // arquivo.
            if (!status.equals("COLISION")) {
                for (Map.Entry<String, Map<String, String>> entry : knownDigests.entrySet()) {
                    String knownFileName = entry.getKey();
                    if (!knownFileName.equals(fileName)) {
                        Map<String, String> fileDigests = entry.getValue();
                        if (fileDigests.containsKey(userDigestType)) {
                            if (computedHex.equalsIgnoreCase(fileDigests.get(userDigestType).trim())) {
                                status = "COLISION";
                                break;
                            }
                        }
                    }
                }
            }

            // Armazena o resultado final.
            results.add(new FileResult(fileName, computedHex, status));
        }

        // 7. Impressão dos resultados no formato especificado.
        for (FileResult res : results) {
            System.out
                    .println(res.fileName + " " + userDigestType + " " + res.computedDigest + " (" + res.status + ")");
        }

        // 8. Atualização do arquivo XML: para arquivos com status NOT FOUND, incluir a
        // entrada do digest calculado.
        boolean updatedXML = false;
        for (FileResult res : results) {
            if (res.status.equals("NOT FOUND")) {
                Element fileEntry = findFileEntry(xmlDoc, res.fileName);
                if (fileEntry == null) {
                    // Cria uma nova entrada para o arquivo.
                    fileEntry = xmlDoc.createElement("FILE_ENTRY");
                    Element fileNameElement = xmlDoc.createElement("FILE_NAME");
                    fileNameElement.appendChild(xmlDoc.createTextNode(res.fileName));
                    fileEntry.appendChild(fileNameElement);
                    // Adiciona a nova entrada no elemento raiz.
                    xmlDoc.getDocumentElement().appendChild(fileEntry);
                }
                // Se não houver um DIGEST_ENTRY para o tipo solicitado, adiciona-o.
                if (!hasDigestType(fileEntry, userDigestType)) {
                    Element digestEntry = xmlDoc.createElement("DIGEST_ENTRY");
                    Element digestTypeElement = xmlDoc.createElement("DIGEST_TYPE");
                    digestTypeElement.appendChild(xmlDoc.createTextNode(userDigestType));
                    Element digestHexElement = xmlDoc.createElement("DIGEST_HEX");
                    digestHexElement.appendChild(xmlDoc.createTextNode(res.computedDigest));
                    digestEntry.appendChild(digestTypeElement);
                    digestEntry.appendChild(digestHexElement);
                    fileEntry.appendChild(digestEntry);
                    updatedXML = true;
                }
            }
        }

        // Se houve atualizações, salva o documento XML de volta no arquivo.
        if (updatedXML) {
            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                // Para formatar a saída do XML
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(xmlDoc);
                StreamResult resultFile = new StreamResult(xmlFile);
                transformer.transform(source, resultFile);
                System.out.println("Arquivo XML atualizado com novos digests.");
            } catch (TransformerException e) {
                System.out.println("Erro ao salvar o arquivo XML: " + e.getMessage());
            }
        }
    }

    // Método que calcula o digest de um arquivo e retorna a string em hexadecimal.
    public static String calculateDigest(File file, MessageDigest md) throws IOException {
        md.reset(); // Garante que o MessageDigest esteja limpo.
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = bis.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        bis.close();
        byte[] digestBytes = md.digest();
        return bytesToHex(digestBytes);
    }

    // Método auxiliar para converter um array de bytes em uma string hexadecimal.
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    // Método que parseia o documento XML e retorna um mapa com as entradas
    // conhecidas.
    // Estrutura: nome do arquivo -> (tipo de digest -> valor em hexadecimal)
    private static Map<String, Map<String, String>> parseCatalog(Document doc) {
        Map<String, Map<String, String>> catalog = new HashMap<>();
        NodeList fileEntries = doc.getElementsByTagName("FILE_ENTRY");
        for (int i = 0; i < fileEntries.getLength(); i++) {
            Node fileEntryNode = fileEntries.item(i);
            if (fileEntryNode.getNodeType() == Node.ELEMENT_NODE) {
                Element fileEntryElement = (Element) fileEntryNode;
                String fileName = getTagValue("FILE_NAME", fileEntryElement);
                if (fileName == null)
                    continue;
                Map<String, String> digestMap = new HashMap<>();
                NodeList digestEntries = fileEntryElement.getElementsByTagName("DIGEST_ENTRY");
                for (int j = 0; j < digestEntries.getLength(); j++) {
                    Node digestEntryNode = digestEntries.item(j);
                    if (digestEntryNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element digestEntryElement = (Element) digestEntryNode;
                        String digestType = getTagValue("DIGEST_TYPE", digestEntryElement);
                        String digestHex = getTagValue("DIGEST_HEX", digestEntryElement);
                        if (digestType != null && digestHex != null) {
                            digestMap.put(digestType.toUpperCase(), digestHex.trim());
                        }
                    }
                }
                catalog.put(fileName, digestMap);
            }
        }
        return catalog;
    }

    // Método auxiliar para obter o conteúdo de uma tag em um elemento XML.
    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    // Procura por uma entrada FILE_ENTRY no XML para um determinado arquivo.
    private static Element findFileEntry(Document doc, String fileName) {
        NodeList fileEntries = doc.getElementsByTagName("FILE_ENTRY");
        for (int i = 0; i < fileEntries.getLength(); i++) {
            Node node = fileEntries.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element fileEntry = (Element) node;
                String name = getTagValue("FILE_NAME", fileEntry);
                if (fileName.equals(name)) {
                    return fileEntry;
                }
            }
        }
        return null;
    }

    // Verifica se a entrada FILE_ENTRY já possui um DIGEST_ENTRY para um tipo de
    // digest.
    private static boolean hasDigestType(Element fileEntry, String digestType) {
        NodeList digestEntries = fileEntry.getElementsByTagName("DIGEST_ENTRY");
        for (int i = 0; i < digestEntries.getLength(); i++) {
            Node node = digestEntries.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element digestEntry = (Element) node;
                String type = getTagValue("DIGEST_TYPE", digestEntry);
                if (digestType.equalsIgnoreCase(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
