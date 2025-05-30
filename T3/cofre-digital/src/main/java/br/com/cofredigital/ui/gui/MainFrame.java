// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.ui.gui.CadastroUsuarioPanel;
// import br.com.cofredigital.ui.gui.LoginPanel; // Removed unused import
import br.com.cofredigital.ui.gui.TotpQrCodePanel;
import br.com.cofredigital.ui.gui.TotpValidationPanel;
import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.ui.gui.SetupAdminPanel;
import br.com.cofredigital.ui.gui.ValidateAdminPassphrasePanel;
import br.com.cofredigital.util.StringUtil;
import br.com.cofredigital.persistencia.modelo.Chaveiro;
import br.com.cofredigital.crypto.CertificateUtil;
import br.com.cofredigital.crypto.PrivateKeyUtil;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
// Import javax.swing.SwingUtilities; // Não diretamente necessário aqui, mas útil para updates de UI
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.Map;

public class MainFrame extends JFrame {

    private final UsuarioServico usuarioServico;
    private final TotpServico totpServico;
    private final RegistroServico registroServico;

    private CardLayout cardLayout;
    private JPanel mainPanel; // Painel que conterá outros painéis (telas)

    // Constantes para os nomes dos painéis (telas)
    public static final String CADASTRO_PANEL = "CadastroUsuarioPanel";
    public static final String TOTP_QRCODE_PANEL = "TotpQrCodePanel";
    public static final String TOTP_VALIDATION_PANEL = "TotpValidationPanel";
    public static final String SETUP_ADMIN_PANEL = "SetupAdminPanel";
    public static final String VALIDATE_ADMIN_PASSPHRASE_PANEL = "ValidateAdminPassphrasePanel";
    public static final String ADMIN_MAIN_PANEL = "AdminMainPanel"; // Nova constante
    public static final String USER_REGISTRATION_ADMIN_PANEL = "UserRegistrationAdminPanel"; // Nova constante
    public static final String USER_MAIN_PANEL = "UserMainPanel"; // Adicionada
    public static final String LOGOUT_EXIT_PANEL = "LogoutExitPanel"; // Adicionada
    public static final String CONSULTAR_ARQUIVOS_SECRETOS_PANEL = "ConsultarArquivosSecretosPanel";
    
    // Painéis para o novo fluxo de autenticação em duas etapas
    public static final String EMAIL_VERIFICATION_PANEL = "EmailVerificationPanel";
    public static final String PASSWORD_PANEL = "PasswordPanel";

    // Estado temporário para integração do fluxo
    private Usuario usuarioEmCadastro;
    private String senhaEmCadastro;
    private Usuario usuarioEmLogin;
    private String senhaEmLogin; // Adicionado para armazenar a senha original temporariamente

    private SetupAdminPanel setupAdminPanel;
    private ValidateAdminPassphrasePanel validateAdminPassphrasePanel;
    private AdminMainPanel adminMainPanel; // Novo painel
    private UserRegistrationAdminPanel userRegistrationAdminPanel; // Novo painel de cadastro pelo admin
    private UserMainPanel userMainPanel; // Adicionado
    private LogoutExitPanel logoutExitPanel; // Adicionado
    private ConsultarArquivosSecretosPanel consultarArquivosSecretosPanel;

    // Painéis do novo fluxo
    private EmailVerificationPanel emailVerificationPanel;
    private PasswordPanel passwordPanel;

    // private LoginPanel loginPanel; // Old combined login panel, now superseded

    public MainFrame(UsuarioServico usuarioServico, TotpServico totpServico, RegistroServico registroServico) {
        this.usuarioServico = usuarioServico;
        this.totpServico = totpServico;
        this.registroServico = registroServico;

        setTitle("Cofre Digital");
        setSize(600, 500); // Ajuste de tamanho para melhor visualização
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
        determineInitialScreen();
        // O log AUTH_ETAPA1_INICIADA só deve ocorrer quando a tela de Login é efetivamente mostrada.
        // Removido daqui, será logado no showScreen quando LOGIN_PANEL for o destino.
        /*
        if (this.registroServico != null) {
            this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA1_INICIADA);
        }
        */
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);


        // Painel de Cadastro - passar registroServico
        Long adminUid = (usuarioEmLogin != null) ? usuarioEmLogin.getId() : null;
        CadastroUsuarioPanel cadastroPanel = new CadastroUsuarioPanel(usuarioServico, registroServico, adminUid) {
            @Override
            protected void onCadastroSuccess() {
                MainFrame.this.usuarioEmCadastro = usuarioServico.buscarPorEmail(getEmail());
                MainFrame.this.senhaEmCadastro = getSenha();
                if (MainFrame.this.usuarioEmCadastro != null && MainFrame.this.senhaEmCadastro != null) {
                    showTotpQrCodePanel(MainFrame.this.usuarioEmCadastro, MainFrame.this.senhaEmCadastro);
                } else {
                     JOptionPane.showMessageDialog(MainFrame.this, 
                                                "Erro ao obter dados para cadastro do TOTP.", 
                                                "Erro de Cadastro", JOptionPane.ERROR_MESSAGE);
                     showScreen(CADASTRO_PANEL);
                }
                MainFrame.this.senhaEmCadastro = null;
            }

            @Override
            protected void onGoToLogin() {
                // Log de ir para login a partir do painel de cadastro já é feito no CadastroUsuarioPanel
                showScreen(EMAIL_VERIFICATION_PANEL);
            }
        };
        cadastroPanel.setName(CADASTRO_PANEL);

        // Painel de QR Code TOTP
        TotpQrCodePanel qrCodePanel = new TotpQrCodePanel() {
            @Override
            protected void onContinue() {
                showScreen(EMAIL_VERIFICATION_PANEL);
            }
        };
        qrCodePanel.setName(TOTP_QRCODE_PANEL);

        // Painel de validação TOTP - passar registroServico
        TotpValidationPanel totpValidationPanel = new TotpValidationPanel(this, usuarioServico, registroServico) {
            /* // Old onTotpValidated and onBack logic is now internal to TotpValidationPanel
            @Override
            protected void onTotpValidated() {
// ... existing code ...
            protected void onBack() {
                // Log de voltar já está no painel
                MainFrame.this.usuarioEmLogin = null;
                MainFrame.this.senhaEmLogin = null;
                showScreen(EMAIL_VERIFICATION_PANEL); 
            }
            */
        };
        totpValidationPanel.setName(TOTP_VALIDATION_PANEL);

        // Novo Painel de Setup do Admin
        setupAdminPanel = new SetupAdminPanel(usuarioServico, registroServico, this);
        setupAdminPanel.setName(SETUP_ADMIN_PANEL);

        // Novo Painel de Validação de Frase Secreta do Admin
        validateAdminPassphrasePanel = new ValidateAdminPassphrasePanel(usuarioServico, registroServico, this);
        validateAdminPassphrasePanel.setName(VALIDATE_ADMIN_PASSPHRASE_PANEL);

        adminMainPanel = new AdminMainPanel(this, usuarioServico, registroServico); // Instanciação
        adminMainPanel.setName(ADMIN_MAIN_PANEL);

        userRegistrationAdminPanel = new UserRegistrationAdminPanel(this, usuarioServico); // Instanciação
        userRegistrationAdminPanel.setName(USER_REGISTRATION_ADMIN_PANEL);

        userMainPanel = new UserMainPanel(this); // Instanciação
        userMainPanel.setName(USER_MAIN_PANEL);

        logoutExitPanel = new LogoutExitPanel(this); // Instanciação
        logoutExitPanel.setName(LOGOUT_EXIT_PANEL);

        consultarArquivosSecretosPanel = new ConsultarArquivosSecretosPanel();
        consultarArquivosSecretosPanel.setName(CONSULTAR_ARQUIVOS_SECRETOS_PANEL);

        // Instanciar novos painéis de autenticação
        emailVerificationPanel = new EmailVerificationPanel(usuarioServico, registroServico, this);
        emailVerificationPanel.setName(EMAIL_VERIFICATION_PANEL);

        passwordPanel = new PasswordPanel(usuarioServico, registroServico, this);
        passwordPanel.setName(PASSWORD_PANEL);

        mainPanel.add(cadastroPanel, CADASTRO_PANEL);
        mainPanel.add(qrCodePanel, TOTP_QRCODE_PANEL);
        mainPanel.add(totpValidationPanel, TOTP_VALIDATION_PANEL);
        mainPanel.add(setupAdminPanel, SETUP_ADMIN_PANEL);
        mainPanel.add(validateAdminPassphrasePanel, VALIDATE_ADMIN_PASSPHRASE_PANEL);
        mainPanel.add(adminMainPanel, ADMIN_MAIN_PANEL); // Adicionar ao CardLayout
        mainPanel.add(userRegistrationAdminPanel, USER_REGISTRATION_ADMIN_PANEL); // Adicionar ao CardLayout
        mainPanel.add(userMainPanel, USER_MAIN_PANEL); // Adicionar ao CardLayout
        mainPanel.add(logoutExitPanel, LOGOUT_EXIT_PANEL); // Adicionar ao CardLayout
        mainPanel.add(consultarArquivosSecretosPanel, CONSULTAR_ARQUIVOS_SECRETOS_PANEL);

        // Adicionar novos painéis ao CardLayout
        mainPanel.add(emailVerificationPanel, EMAIL_VERIFICATION_PANEL);
        mainPanel.add(passwordPanel, PASSWORD_PANEL);

        add(mainPanel);
    }

    private void determineInitialScreen() {
        if (usuarioServico.isFirstExecution()) {
            // Adiciona o log 1005 para cadastro do admin
            registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN);
            showScreen(SETUP_ADMIN_PANEL);
        } else {
            // Não é primeira execução, solicitar frase secreta do admin
            if (usuarioServico.isAdminPassphraseValidatedForSession()) {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_OPERACAO_NORMAL);
                showScreen(EMAIL_VERIFICATION_PANEL); // Novo: Inicia com a verificação de email
            } else {
                showScreen(VALIDATE_ADMIN_PASSPHRASE_PANEL);
            }
        }
    }

    // Método para trocar a tela visível
    public void showScreen(String panelName) {
        // Se estivermos mostrando o painel de login (que agora é o EmailVerificationPanel no fluxo normal),
        // ou o próprio EmailVerificationPanel diretamente, resetamos seus campos.
        if (EMAIL_VERIFICATION_PANEL.equals(panelName)) {
            if (emailVerificationPanel != null) { // Adiciona verificação de nulidade por segurança
                emailVerificationPanel.resetPanel(); 
            }
        } else if (PASSWORD_PANEL.equals(panelName)) {
            if (passwordPanel != null) {
                // passwordPanel.resetPanel(); // PasswordPanel já é preparado via prepareForUser ou resetado ao voltar
            }
        }
        // Adicionar logs de transição de tela aqui, se necessário
        // Ex: registroServico.registrarEventoDoSistema(MID_GUI_TELA_EXIBIDA, "nome_tela", panelName);

        cardLayout.show(mainPanel, panelName);
        
        // Se a tela de login (agora EmailVerificationPanel) for mostrada, registrar início da Etapa 1 de autenticação.
        if (EMAIL_VERIFICATION_PANEL.equals(panelName)) {
            if (registroServico != null) {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA1_INICIADA);
            }
        }
        
        if (registroServico != null) {
            if (CADASTRO_PANEL.equals(panelName)) {
                // Para CADASTRO_PANEL, precisamos saber se é um admin operando ou um novo usuário se auto-registrando (se permitido).
                // Se for um admin logado (usuarioEmLogin != null e é admin), logar com UID dele.
                // Por agora, um log genérico.
                Long operadorUid = (usuarioEmLogin != null) ? usuarioEmLogin.getId() : null;
                String operadorEmail = (usuarioEmLogin != null) ? usuarioEmLogin.getEmail() : null;
                String operadorGrupo = (usuarioEmLogin != null) ? usuarioEmLogin.getGrupo() : null;
            } else if (TOTP_VALIDATION_PANEL.equals(panelName)) {
                TotpValidationPanel tvp = (TotpValidationPanel) getPanelByName(TOTP_VALIDATION_PANEL);
                if (tvp != null) {
                    tvp.resetFields();
                    if (usuarioEmLogin != null) {
                        // tvp.setCurrentUserEmailForLog(usuarioEmLogin.getEmail()); // Removed, panel gets user object
                        // Log de apresentação da tela TOTP (já deve estar sendo feito ou pode ser adicionado aqui)
                        // // registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_TELA_APRESENTADA_GUI, usuarioEmLogin.getId(), "email_usuario", usuarioEmLogin.getEmail(), "grupo_usuario", usuarioEmLogin.getGrupo());
                    } else {
                        // // registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA3_TELA_APRESENTADA_GUI, "erro", "usuario_contexto_ausente_para_log_tela_totp");
                    }
                }
            } else if (SETUP_ADMIN_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            } else if (VALIDATE_ADMIN_PASSPHRASE_PANEL.equals(panelName)) {
                // Log já no construtor do painel
                // Garantir que o foco vá para o campo de senha
                if (validateAdminPassphrasePanel != null) {
                    validateAdminPassphrasePanel.requestFocusInWindow();
                    validateAdminPassphrasePanel.limparCampo(); // Limpa o campo ao ser exibido
                }
            } else if (ADMIN_MAIN_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            } else if (USER_REGISTRATION_ADMIN_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            } else if (USER_MAIN_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            } else if (LOGOUT_EXIT_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            } else if (CONSULTAR_ARQUIVOS_SECRETOS_PANEL.equals(panelName)) {
                // Log já no construtor do painel
            }
            // Adicionar logs para outras telas conforme são implementadas (ex: TELA_PRINCIPAL_APRESENTADA)
        }
    }

    // Método para exibir o painel de QR Code após cadastro
    private void showTotpQrCodePanel(Usuario usuario, String senhaOriginal) {
        try {
            String chaveSecreta = usuarioServico.obterChaveTotpDescriptografada(usuario, senhaOriginal);
            String email = usuario.getEmail();
            String otpauthUrl = totpServico.gerarUrlQRCode(chaveSecreta, email);

            // Gerar imagem QR Code usando ZXing
            BufferedImage qrImage = br.com.cofredigital.util.QrCodeUtil.generateQrCodeImage(otpauthUrl, 200, 200);

            TotpQrCodePanel qrCodePanelRef = (TotpQrCodePanel) getPanelByName(TOTP_QRCODE_PANEL);
            qrCodePanelRef.setQrCodeImage(qrImage);
            qrCodePanelRef.setSecretKey(chaveSecreta);

            showScreen(TOTP_QRCODE_PANEL);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar QR Code: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            showScreen(EMAIL_VERIFICATION_PANEL);
        }
    }

    // Utilitário para obter painel pelo nome
    private JPanel getPanelByName(String name) {
        for (Component comp : mainPanel.getComponents()) {
            if (mainPanel.getLayout() instanceof CardLayout) {
                if (comp.getName() != null && comp.getName().equals(name)) {
                    return (JPanel) comp;
                }
                // Alternativamente, comparar pelo tipo
                if (name.equals(TOTP_QRCODE_PANEL) && comp instanceof TotpQrCodePanel) return (JPanel) comp;
                if (name.equals(TOTP_VALIDATION_PANEL) && comp instanceof TotpValidationPanel) return (JPanel) comp;
            }
        }
        return null;
    }

    // Getters para os serviços, se necessário pelos painéis filhos
    public UsuarioServico getUsuarioServico() {
        return usuarioServico;
    }

    public TotpServico getTotpServico() {
        return totpServico;
    }

    public RegistroServico getRegistroServico() {
        return registroServico;
    }

    public void onSetupAdminSubmit(String caminhoCert, String caminhoChave, String fraseSecreta, String senhaPessoal) {
        try {
            Usuario adminCriado = usuarioServico.setupInitialAdmin(
                null,
                null,
                caminhoCert,
                caminhoChave,
                fraseSecreta,
                senhaPessoal,
                "Administrador"
            );

            if (adminCriado != null) {
                this.usuarioEmCadastro = adminCriado;
                showTotpQrCodePanel(adminCriado, senhaPessoal); 
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao configurar o administrador. Verifique os logs.", "Erro no Setup", JOptionPane.ERROR_MESSAGE);
                showScreen(SETUP_ADMIN_PANEL);
                setupAdminPanel.limparCampos();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao configurar administrador: " + e.getMessage(), "Erro de Configuração", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            showScreen(SETUP_ADMIN_PANEL);
            if (setupAdminPanel != null) {
                 setupAdminPanel.limparCampos();
            }
        }
    }

    // Novo método de callback para o ValidateAdminPassphrasePanel
    public void onAdminPassphraseValidated() {
        showScreen(EMAIL_VERIFICATION_PANEL);
    }

    public void onAdminSetupComplete() {
        // Log já feito pelo SetupAdminPanel
        System.out.println("[MainFrame] Admin setup completo (onAdminSetupComplete). Navegando para EMAIL_VERIFICATION_PANEL.");
        showScreen(EMAIL_VERIFICATION_PANEL);
    }

    public void onUserLoginRequired() {
         System.out.println("[MainFrame] User login required. Navegando para EMAIL_VERIFICATION_PANEL.");
        showScreen(EMAIL_VERIFICATION_PANEL);
    }
    
    // Novo método logout
    public void logout() {
        this.usuarioEmLogin = null;
        showScreen(EMAIL_VERIFICATION_PANEL);
    }

    // Novo método placeholder
    public void showUserRegistrationPanel(Usuario adminLogado) {
        if (userRegistrationAdminPanel != null) {
            userRegistrationAdminPanel.prepareForm(adminLogado);
            registroServico.registrarEventoDoUsuario(
                        LogEventosMIDs.TELA_CADASTRO_APRESENTADA,
                        adminLogado.getId(),
                        "email_usuario", adminLogado.getEmail(),
                        "grupo_usuario", adminLogado.getGrupo()
                    );
            showScreen(USER_REGISTRATION_ADMIN_PANEL);
        } else {
            // Fallback ou log de erro se o painel não for inicializado
            JOptionPane.showMessageDialog(this,
                "Erro: Painel de Cadastro de Usuário (Admin) não está pronto.",
                "Erro de Interface", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Usuario getUsuarioLogado() {
        return usuarioEmLogin;
    }

    // Adicionado para consistência, embora logout() já limpe
    public void setUsuarioLogado(Usuario usuario) {
        this.usuarioEmLogin = usuario;
    }

    // Método para exibir o painel de Logout/Exit
    public void showLogoutExitPanel(Usuario usuario) {
        if (logoutExitPanel != null) {
            setUsuarioLogado(usuario); // Garante que o usuário está setado para o painel de saída
            logoutExitPanel.prepareForm(usuario);
            showScreen(LOGOUT_EXIT_PANEL);
        } else {
            JOptionPane.showMessageDialog(this, "Erro: Painel de Saída não está pronto.", "Erro de Interface", JOptionPane.ERROR_MESSAGE);
            // Fallback, talvez ir para o login se o painel de saída falhar
            if (usuario != null && "Administrador".equals(usuario.getGrupo())) {
                 showScreen(ADMIN_MAIN_PANEL);
            } else if (usuario != null) {
                 showScreen(USER_MAIN_PANEL);
            } else {
                showScreen(EMAIL_VERIFICATION_PANEL);
            }
        }
    }
    
    public void showTotpValidationPanel(String email, String senhaPessoalOriginal) {
        TotpValidationPanel tvp = (TotpValidationPanel) getPanelByName(TOTP_VALIDATION_PANEL);
        if (tvp != null) {
            // tvp.preparePanel(email, senhaPessoalOriginal); // Linha removida - causa do erro
            // O email para log é setado em showScreen.
            // A senhaPessoalOriginal (this.senhaEmLogin) já está disponível no MainFrame para uso em onTotpValidated.
            showScreen(TOTP_VALIDATION_PANEL);
        } else {
            JOptionPane.showMessageDialog(this, "Erro ao carregar tela de validação TOTP.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showConsultarArquivosSecretosPanel(Usuario usuario, int totalConsultas) {
        consultarArquivosSecretosPanel.setUsuarioLogado(usuario, totalConsultas);
        consultarArquivosSecretosPanel.limparTabela();
        consultarArquivosSecretosPanel.limparCamposDeEntrada();
        showScreen(CONSULTAR_ARQUIVOS_SECRETOS_PANEL);
    }

    // Novo método para exibir o painel de consulta de arquivos secretos para o admin
    public void showConsultarArquivosSecretosPanel() {
        try {
            // Recupera o KID do usuário logado
            Integer kid = usuarioEmLogin.getKid();
            if (kid == null) {
                JOptionPane.showMessageDialog(this, "Usuário não possui chaveiro associado.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Busca o chaveiro
            Chaveiro chaveiro = usuarioServico.buscarChaveiroPorKid(kid).orElseThrow(() -> new Exception("Chaveiro não encontrado para o usuário."));
            // Carrega o certificado
            X509Certificate certificado = CertificateUtil.loadCertificateFromPEMString(chaveiro.getCertificadoPem());
            // Não decriptar a chave privada aqui, pois será feito no painel com a frase secreta informada
            // Injeta o serviço de usuário e o usuário logado
            consultarArquivosSecretosPanel.setUsuarioServico(usuarioServico);
            consultarArquivosSecretosPanel.setUsuarioLogado(usuarioEmLogin, usuarioEmLogin.getTotalAcessos());
            consultarArquivosSecretosPanel.limparTabela();
            consultarArquivosSecretosPanel.limparCamposDeEntrada();
            // Exibe o painel
            showScreen(CONSULTAR_ARQUIVOS_SECRETOS_PANEL);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao preparar painel de consulta de arquivos secretos: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public void navigateToPasswordPanel(Usuario usuario) {
        this.usuarioEmLogin = usuario; // Armazena o usuário validado na primeira etapa
        passwordPanel.prepareForUser(usuario);

        registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_INICIADA, usuario.getId(), "email", usuario.getEmail());
        showScreen(PASSWORD_PANEL);
    }

    public void navigateToTotpValidation(Usuario usuario, String senhaPlanaAutenticada) {
        this.usuarioEmLogin = usuario;
        this.senhaEmLogin = senhaPlanaAutenticada;
        TotpValidationPanel tvp = (TotpValidationPanel) getPanelByName(TOTP_VALIDATION_PANEL);
        if (tvp != null) {
            tvp.prepareForValidation(usuario, senhaPlanaAutenticada);
        }
        registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_INICIADA, usuario.getId(), "email", usuario.getEmail());
        showScreen(TOTP_VALIDATION_PANEL);
    }

    // Getter para AdminMainPanel
    public AdminMainPanel getAdminMainPanel() {
        return this.adminMainPanel;
    }

    // Getter para UserMainPanel
    public UserMainPanel getUserMainPanel() {
        return this.userMainPanel;
    }

    // Método para limpar dados sensíveis de login
    public void clearSensitiveLoginData() {
        this.usuarioEmLogin = null;
        this.senhaEmLogin = null;
        // // registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_DADOS_SESSAO_LIMPOS_GUI, "motivo", "Retorno da tela TOTP ou logout explícito");
        System.out.println("[MainFrame] Dados sensíveis da sessão de login (usuário/senha plana) foram limpos.");
    }
} 