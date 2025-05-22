// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import javax.swing.*;
import java.awt.*;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;

public class TotpValidationPanel extends JPanel {
    private static final int MAX_TENTATIVAS_TOTP_PERMITIDAS = 3; // Should align with UsuarioServico.MAX_TENTATIVAS_TOTP
    private static final int TEMPO_BLOQUEIO_MINUTOS_TOTP = 2; // Should align with UsuarioServico.MINUTOS_BLOQUEIO_SENHA

    private final JLabel instrucaoLabel = new JLabel("Digite o código de 6 dígitos do seu app autenticador:");
    private final JTextField codigoField = new JTextField(8);
    private final JButton validarButton = new JButton("Validar");
    private final JButton voltarButton = new JButton("Voltar");
    private final JLabel statusLabel = new JLabel(" ");
    
    private final MainFrame mainFrame; // Added to navigate and access services
    private final UsuarioServico usuarioServico; // Added to call new service method
    private final RegistroServico registroServico;
    
    private Usuario usuarioCorrenteParaTotp; // To hold the user object passed from MainFrame
    private String senhaOriginalParaTotp;   // To hold the original password for decrypting TOTP key

    public TotpValidationPanel(MainFrame mainFrame, UsuarioServico usuarioServico, RegistroServico registroServico) {
        this.mainFrame = mainFrame;
        this.usuarioServico = usuarioServico;
        this.registroServico = registroServico;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(instrucaoLabel, gbc);

        gbc.gridy++;
        add(codigoField, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(validarButton, gbc);
        gbc.gridx = 1;
        add(voltarButton, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        add(statusLabel, gbc);

        validarButton.addActionListener(e -> {
            String codigoInput = codigoField.getText().trim();
            // Log da tentativa de validação do TOTP pela GUI
            Long uidParaLog = (usuarioCorrenteParaTotp != null) ? usuarioCorrenteParaTotp.getId() : null;
            String emailParaLog = (usuarioCorrenteParaTotp != null) ? usuarioCorrenteParaTotp.getEmail() : "N/A";
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_BOTAO_VALIDAR_PRESSIONADO_GUI, uidParaLog, "email_usuario", emailParaLog, "codigo_tentativa", codigoInput);

            if (codigoInput.isEmpty() || !codigoInput.matches("\\d{6}")) {
                setStatus("Código TOTP deve ter 6 dígitos numéricos.");
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CODIGO_INVALIDO_FORMATO_GUI, uidParaLog, "email_usuario", emailParaLog, "codigo_fornecido", codigoInput);
                codigoField.requestFocusInWindow();
                return;
            }

            if (usuarioCorrenteParaTotp == null || senhaOriginalParaTotp == null) {
                setStatus("Erro interno: Informações do usuário ou senha original ausentes.");
                // // registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA3_ERRO_INTERNO_GUI, "motivo", "usuarioCorrente ou senhaOriginal nulos no painel TOTP");
                return;
            }

            try {
                statusLabel.setText("Validando código TOTP...");
                statusLabel.setForeground(Color.BLACK);

                // 1. Obter a chave TOTP descriptografada
                String chaveSecretaTotpPlana;
                try {
                    chaveSecretaTotpPlana = usuarioServico.obterChaveTotpDescriptografada(usuarioCorrenteParaTotp, senhaOriginalParaTotp);
                } catch (Exception ex) {
                    // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CHAVE_TOTP_DEC_FALHA_GUI, uidParaLog, "email", emailParaLog, "erro", ex.getMessage());
                    setStatus("Falha ao preparar validação TOTP. Senha mestre pode estar incorreta ou chave corrompida.");
                    // Neste ponto, não é uma falha de TOTP em si, mas da etapa anterior (senha).
                    // Considerar se deve voltar para a tela de senha ou de email.
                    // Por segurança, voltar para o início do fluxo.
                    mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL);
                    return;
                }

                // 2. Processar a tentativa de TOTP usando o novo método de serviço
                boolean totpValido = usuarioServico.processarTentativaTotp(usuarioCorrenteParaTotp, codigoInput, chaveSecretaTotpPlana);

                if (totpValido) {
                    statusLabel.setForeground(new Color(0,128,0));
                    setStatus("TOTP válido! Login completo.");
                    // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_VALIDACAO_SUCESSO_GUI, uidParaLog, "email_usuario", emailParaLog, "grupo_usuario", usuarioCorrenteParaTotp.getGrupo());
                    
                    // Navegar para a tela principal da aplicação
                    if ("Administrador".equalsIgnoreCase(usuarioCorrenteParaTotp.getGrupo())) {
                        mainFrame.getAdminMainPanel().setAdminLogado(usuarioCorrenteParaTotp); // Assumindo getter em MainFrame
                        mainFrame.showScreen(MainFrame.ADMIN_MAIN_PANEL);
                    } else {
                        mainFrame.getUserMainPanel().preparePanel(usuarioCorrenteParaTotp); // Assumindo getter em MainFrame
                        mainFrame.showScreen(MainFrame.USER_MAIN_PANEL);
                    }
                    resetPanelState(); // Limpa dados sensíveis do painel
                } else {
                    // TOTP foi inválido, verificar se a conta foi bloqueada
                    Usuario usuarioAposTentativa = usuarioServico.buscarPorEmail(emailParaLog); // Re-fetch para estado atualizado
                    if (usuarioAposTentativa == null) { // Não deveria acontecer
                        setStatus("Erro crítico ao verificar status do usuário.");
                        mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL);
                        return;
                    }
                    
                    this.usuarioCorrenteParaTotp = usuarioAposTentativa; // Atualiza o usuário no painel

                    statusLabel.setForeground(Color.RED);
                    if (usuarioCorrenteParaTotp.isAcessoBloqueado()) {
                        String mensagemBloqueio = "Conta bloqueada por " + TEMPO_BLOQUEIO_MINUTOS_TOTP + " minutos devido a tentativas de TOTP excedidas.";
                        setStatus(mensagemBloqueio); // Também no label para consistência
                        JOptionPane.showMessageDialog(TotpValidationPanel.this,
                            mensagemBloqueio,
                            "Acesso Bloqueado",
                            JOptionPane.WARNING_MESSAGE);
                        mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL);
                        resetPanelState();
                    } else {
                        int tentativasFeitas = usuarioCorrenteParaTotp.getTentativasFalhasToken();
                        int tentativasRestantes = MAX_TENTATIVAS_TOTP_PERMITIDAS - tentativasFeitas;
                        setStatus("Código TOTP inválido. Tentativas restantes: " + Math.max(0, tentativasRestantes));
                        codigoField.setText(""); // Limpa para nova tentativa
                        codigoField.requestFocusInWindow();
                    }
                }
            } catch (Exception ex) {
                // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_ERRO_INESPERADO_GUI, uidParaLog, "email", emailParaLog, "erro", ex.getMessage());
                setStatus("Erro inesperado durante a validação TOTP: " + ex.getMessage());
                ex.printStackTrace();
                codigoField.setText("");
                codigoField.requestFocusInWindow();
            }
        });

        voltarButton.addActionListener(e -> {
            Long uidParaLogVoltar = (usuarioCorrenteParaTotp != null) ? usuarioCorrenteParaTotp.getId() : null;
            String emailParaLogVoltar = (usuarioCorrenteParaTotp != null) ? usuarioCorrenteParaTotp.getEmail() : "N/A";
            // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_BOTAO_VOLTAR_PRESSIONADO_GUI, uidParaLogVoltar, "email_usuario", emailParaLogVoltar);
            resetPanelState();
            // Ao voltar, o usuárioEmLogin e senhaEmLogin no MainFrame devem ser limpos.
            // MainFrame deve ter um método para limpar esses estados ou o showScreen do email/password panel deve fazer isso.
            mainFrame.clearSensitiveLoginData(); // Supõe um método em MainFrame para limpar usuarioEmLogin e senhaEmLogin
            mainFrame.showScreen(MainFrame.PASSWORD_PANEL); // Volta para a tela de senha
        });
    }

    public void prepareForValidation(Usuario usuario, String senhaOriginal) {
        this.usuarioCorrenteParaTotp = usuario;
        this.senhaOriginalParaTotp = senhaOriginal;
        setStatus("Usuário: " + usuario.getEmail() + ". Insira o código TOTP.");
        statusLabel.setForeground(Color.BLACK);
        codigoField.setText("");
        SwingUtilities.invokeLater(() -> codigoField.requestFocusInWindow());
    }
    
    private void resetPanelState() {
        this.usuarioCorrenteParaTotp = null;
        this.senhaOriginalParaTotp = null;
        codigoField.setText("");
        statusLabel.setText(" ");
    }

    public String getCodigoTotp() {
        return codigoField.getText().trim();
    }

    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    public void resetFields() {
        codigoField.setText("");
        statusLabel.setText(" ");
    }
} 