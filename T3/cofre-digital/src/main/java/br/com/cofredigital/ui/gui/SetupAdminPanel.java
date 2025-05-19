package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.util.StringUtil;
import br.com.cofredigital.crypto.CertificateUtil;

// Adicionar imports para BouncyCastle e manipulação de arquivos
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;

public class SetupAdminPanel extends JPanel {
    private JTextField txtCaminhoCertificado;
    private JTextField txtCaminhoChavePrivada;
    private JPasswordField pwdFraseSecretaChave;
    private JPasswordField pwdSenhaAdmin;
    private JPasswordField pwdConfirmarSenhaAdmin;
    private JButton btnSelecionarCertificado;
    private JButton btnSelecionarChavePrivada;
    private JButton btnConfigurarAdmin;

    private final UsuarioServico usuarioServico;
    private final RegistroServico registroServico;
    private MainFrame mainFrame; // Para callback

    public SetupAdminPanel(UsuarioServico usuarioServico, RegistroServico registroServico, MainFrame mainFrame) {
        this.usuarioServico = usuarioServico;
        this.registroServico = registroServico;
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        initComponents();
        addListeners();

        this.registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_TELA_APRESENTADA_GUI);
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        JLabel lblTitulo = new JLabel("Configuração Inicial do Administrador", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3; // Span across 3 columns
        gbc.weightx = 1.0;
        add(lblTitulo, gbc);

        // Caminho do Certificado
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(new JLabel("Caminho do Certificado (.cer/.pem):"), gbc);
        txtCaminhoCertificado = new JTextField(30);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(txtCaminhoCertificado, gbc);
        btnSelecionarCertificado = new JButton("Selecionar...");
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        add(btnSelecionarCertificado, gbc);

        // Caminho da Chave Privada
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Caminho da Chave Privada (.key):"), gbc);
        txtCaminhoChavePrivada = new JTextField(30);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(txtCaminhoChavePrivada, gbc);
        btnSelecionarChavePrivada = new JButton("Selecionar...");
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        add(btnSelecionarChavePrivada, gbc);
        
        // Frase Secreta da Chave Privada
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Frase Secreta da Chave Privada:"), gbc);
        pwdFraseSecretaChave = new JPasswordField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Span
        add(pwdFraseSecretaChave, gbc);
        gbc.gridwidth = 1; // Reset

        // Senha Pessoal do Administrador
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Senha Pessoal do Administrador:"), gbc);
        pwdSenhaAdmin = new JPasswordField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Span
        add(pwdSenhaAdmin, gbc);
        gbc.gridwidth = 1; // Reset

        // Confirmar Senha Pessoal
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Confirmar Senha Pessoal:"), gbc);
        pwdConfirmarSenhaAdmin = new JPasswordField(30);
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Span
        add(pwdConfirmarSenhaAdmin, gbc);
        gbc.gridwidth = 1; // Reset

        // Botão Configurar
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        btnConfigurarAdmin = new JButton("Configurar Administrador");
        btnConfigurarAdmin.setFont(new Font("Arial", Font.BOLD, 14));
        btnConfigurarAdmin.setBackground(new Color(70, 130, 180)); // SteelBlue
        btnConfigurarAdmin.setForeground(Color.WHITE);
        add(btnConfigurarAdmin, gbc);
    }

    private void addListeners() {
        btnSelecionarCertificado.addActionListener(e -> selecionarArquivo("Selecionar Certificado Digital", txtCaminhoCertificado, "cer", "pem"));
        btnSelecionarChavePrivada.addActionListener(e -> selecionarArquivo("Selecionar Chave Privada", txtCaminhoChavePrivada, "key", "der"));

        btnConfigurarAdmin.addActionListener(e -> {
            this.registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_BOTAO_CONFIGURAR_PRESSIONADO_GUI);
            processarConfiguracaoAdmin();
        });
    }

    private void selecionarArquivo(String titulo, JTextField campoTexto, String... extensoes) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(titulo);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extensoes != null && extensoes.length > 0) {
            javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                    String.join(", ", extensoes).toUpperCase() + " Files", extensoes);
            fileChooser.setFileFilter(filter);
        }

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            campoTexto.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void processarConfiguracaoAdmin() {
        String caminhoCertOriginal = txtCaminhoCertificado.getText().trim();
        String caminhoChave = txtCaminhoChavePrivada.getText().trim();
        String fraseSecreta = new String(pwdFraseSecretaChave.getPassword());
        String senha = new String(pwdSenhaAdmin.getPassword());
        String confirmarSenha = new String(pwdConfirmarSenhaAdmin.getPassword());

        if (StringUtil.isAnyEmpty(caminhoCertOriginal, caminhoChave, fraseSecreta, senha, confirmarSenha)) {
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_DADOS_INVALIDOS_GUI, "motivo", "Campos obrigatórios não preenchidos.");
            JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_DADOS_INVALIDOS_GUI, "motivo", "Senhas não coincidem.");
            JOptionPane.showMessageDialog(this, "As senhas pessoais não coincidem.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            pwdSenhaAdmin.setText("");
            pwdConfirmarSenhaAdmin.setText("");
            pwdSenhaAdmin.requestFocus();
            return;
        }
        
        // Validações de senha (exemplo: comprimento mínimo)
        if (senha.length() < 8) { // Ajuste conforme política de senha
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_DADOS_INVALIDOS_GUI, "motivo", "Senha pessoal muito curta.");
            JOptionPane.showMessageDialog(this, "A senha pessoal deve ter no mínimo 8 caracteres.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            pwdSenhaAdmin.setText("");
            pwdConfirmarSenhaAdmin.setText("");
            pwdSenhaAdmin.requestFocus();
            return;
        }

        // Adicionar o provider BouncyCastle se ainda não estiver presente
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        Path tempCertPath = null;
        String caminhoCertParaServico = null;

        try {
            // Passo 0: Pré-processar o certificado para garantir que está em formato PEM puro
            X509Certificate certificateObject;
            try (FileReader fileReader = new FileReader(caminhoCertOriginal);
                 PEMParser pemParser = new PEMParser(fileReader)) {
                Object parsedObj = pemParser.readObject();
                if (parsedObj instanceof X509CertificateHolder) {
                    X509CertificateHolder certHolder = (X509CertificateHolder) parsedObj;
                    certificateObject = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder);
                } else {
                    throw new IOException("Não foi possível ler o certificado do arquivo: " + caminhoCertOriginal +
                                          ". Objeto lido: " + (parsedObj != null ? parsedObj.getClass().getName() : "null"));
                }
            }

            String pemLimpo = CertificateUtil.convertToPem(certificateObject);
            tempCertPath = Files.createTempFile("admin_cert_temp_", ".pem");
            Files.writeString(tempCertPath, pemLimpo, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            caminhoCertParaServico = tempCertPath.toAbsolutePath().toString();

            // Agora `caminhoCertParaServico` contém o caminho para o certificado PEM "limpo"
            // Todas as chamadas subsequentes que precisam do caminho do certificado usarão `caminhoCertParaServico`

            // Passo 1: Validar o certificado (agora usando o arquivo PEM limpo) e mostrar confirmação
            // A variável 'certificate' abaixo será carregada a partir do 'caminhoCertParaServico'
            X509Certificate certificate = CertificateUtil.loadCertificateFromFile(caminhoCertParaServico);
            if (certificate == null) {
                 registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "caminho", caminhoCertOriginal); // Logar caminho original
                 JOptionPane.showMessageDialog(this, "Não foi possível carregar o certificado do caminho especificado (após processamento).", "Erro de Certificado", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            // Tentar carregar a chave para ver se a frase secreta está correta ANTES de mostrar o diálogo de confirmação.
            // Usa 'caminhoChave' (original) e 'certificate' (do PEM limpo)
            try {
                boolean chaveValida = usuarioServico.validarChavePrivadaComFrase(caminhoChave, fraseSecreta, certificate);
                if (!chaveValida) {
                    registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, "path_chave", caminhoChave, "motivo", "Frase secreta incorreta ou chave incompatível com certificado.");
                    JOptionPane.showMessageDialog(this, "A frase secreta da chave privada está incorreta ou a chave não corresponde ao certificado.", "Erro de Chave Privada", JOptionPane.ERROR_MESSAGE);
                    pwdFraseSecretaChave.setText("");
                    pwdFraseSecretaChave.requestFocus();
                    return;
                }
            } catch (Exception ex) {
                 registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CHAVE_PRIVADA_FRASE_SECRETA_INVALIDA, "path_chave", caminhoChave, "erro_validacao", ex.getMessage());
                 JOptionPane.showMessageDialog(this, "Erro ao validar a chave privada: " + ex.getMessage(), "Erro de Chave Privada", JOptionPane.ERROR_MESSAGE);
                 return;
            }

            String cn = CertificateUtil.extractCNFromCertificate(certificate);
            String emailCert = CertificateUtil.extractEmailFromCertificate(certificate);
            String issuer = certificate.getIssuerDN().getName();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String validFrom = sdf.format(certificate.getNotBefore());
            String validTo = sdf.format(certificate.getNotAfter());

            String mensagemConfirmacao = String.format(
                "<html><b>Confirme os Dados do Certificado do Administrador:</b><br><br>" +
                "<b>Sujeito (CN):</b> %s<br>" +
                "<b>E-mail (SAN):</b> %s<br>" +
                "<b>Emissor:</b> %s<br>" +
                "<b>Válido De:</b> %s<br>" +
                "<b>Válido Até:</b> %s<br><br>" +
                "Este certificado será usado para identificar o administrador principal do sistema.<br>" +
                "O e-mail extraído (%s) será o login do administrador.<br>" +
                "Deseja continuar com esta configuração?</html>",
                cn, (emailCert != null ? emailCert : "Não encontrado"), issuer, validFrom, validTo, (emailCert != null ? emailCert : "N/A")
            );
            
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_CONFIRMACAO_CERTIFICADO_APRESENTADA_GUI,
                "cn_cert", cn, "email_cert", (emailCert != null ? emailCert : "N/A"), "validade_cert", validTo
            );

            int confirm = JOptionPane.showConfirmDialog(this,
                new JLabel(mensagemConfirmacao), // Usar JLabel para suportar HTML
                "Confirmar Dados do Certificado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_CONFIRMACAO_CERTIFICADO_ACEITA_GUI);
                // Delegar TODA a lógica de submissão, criação do admin, feedback e navegação para o MainFrame.
                // O MainFrame.onSetupAdminSubmit já faz isso.
                mainFrame.onSetupAdminSubmit(caminhoCertParaServico, // Usa o caminho do cert PEM limpo
                                            caminhoChave,
                                            fraseSecreta,
                                            senha);
            } else {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_CONFIRMACAO_CERTIFICADO_REJEITADA_GUI);
            }
        } catch (IOException ioe) {
            registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, "caminho", caminhoCertOriginal, "erro_io", ioe.getMessage());
            JOptionPane.showMessageDialog(this, "Erro de I/O ao processar o arquivo de certificado: " + ioe.getMessage(), "Erro de Arquivo", JOptionPane.ERROR_MESSAGE);
            ioe.printStackTrace();
        } catch (Exception exGeral) {
            // Captura qualquer outra exceção durante o processamento do certificado ou chave
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_FALHA_GERAL_GUI, "erro_inesperado", exGeral.getMessage());
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado: " + exGeral.getMessage(), "Erro Geral", JOptionPane.ERROR_MESSAGE);
            exGeral.printStackTrace();
        } finally {
            if (tempCertPath != null) {
                try {
                    Files.deleteIfExists(tempCertPath);
                } catch (IOException exDel) {
                    // Logar falha na deleção do arquivo temporário, mas não impedir o fluxo
                    System.err.println("Falha ao deletar arquivo de certificado temporário: " + tempCertPath + " - " + exDel.getMessage());
                    registroServico.registrarEventoDoSistema(LogEventosMIDs.SISTEMA_ALERTA, "tipo", "FalhaDelecaoArquivoTemporario", "path", tempCertPath.toString(), "erro", exDel.getMessage());
                }
            }
        }
    }
    
    // Método auxiliar para limpar os campos
    public void limparCampos() {
        txtCaminhoCertificado.setText("");
        txtCaminhoChavePrivada.setText("");
        pwdFraseSecretaChave.setText("");
        pwdSenhaAdmin.setText("");
        pwdConfirmarSenhaAdmin.setText("");
    }
} 