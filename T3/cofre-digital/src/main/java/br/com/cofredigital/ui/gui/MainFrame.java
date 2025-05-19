package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.ui.gui.CadastroUsuarioPanel;
import br.com.cofredigital.ui.gui.LoginPanel;
import br.com.cofredigital.ui.gui.TotpQrCodePanel;
import br.com.cofredigital.ui.gui.TotpValidationPanel;
import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.ui.gui.SetupAdminPanel;
import br.com.cofredigital.ui.gui.ValidateAdminPassphrasePanel;
import br.com.cofredigital.util.StringUtil;

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
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String CADASTRO_PANEL = "CadastroUsuarioPanel";
    public static final String TOTP_QRCODE_PANEL = "TotpQrCodePanel";
    public static final String TOTP_VALIDATION_PANEL = "TotpValidationPanel";
    public static final String SETUP_ADMIN_PANEL = "SetupAdminPanel";
    public static final String VALIDATE_ADMIN_PASSPHRASE_PANEL = "ValidateAdminPassphrasePanel";
    // Adicionar outras constantes conforme necessário (ex: VALIDATE_ADMIN_PASSPHRASE_PANEL)

    // Estado temporário para integração do fluxo
    private Usuario usuarioEmCadastro;
    private String senhaEmCadastro;
    private Usuario usuarioEmLogin;
    private String senhaEmLogin; // Adicionado para armazenar a senha original temporariamente

    private SetupAdminPanel setupAdminPanel;
    private ValidateAdminPassphrasePanel validateAdminPassphrasePanel;

    public MainFrame(UsuarioServico usuarioServico, TotpServico totpServico, RegistroServico registroServico) {
        this.usuarioServico = usuarioServico;
        this.totpServico = totpServico;
        this.registroServico = registroServico;

        setTitle("Cofre Digital");
        setSize(500, 400);
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

        // Painel de Login
        LoginPanel loginPanel = new LoginPanel(usuarioServico, registroServico) {
            @Override
            protected void onLoginSuccess(String email, String senhaPlanaVerificada) {
                MainFrame.this.usuarioEmLogin = usuarioServico.buscarPorEmail(email);
                MainFrame.this.senhaEmLogin = senhaPlanaVerificada;
                
                if (MainFrame.this.usuarioEmLogin == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, 
                                                "Erro crítico: Usuário não encontrado após login bem-sucedido.", 
                                                "Erro de Login", JOptionPane.ERROR_MESSAGE);
                    showScreen(LOGIN_PANEL);
                    return;
                }

                showScreen(TOTP_VALIDATION_PANEL);
            }

            @Override
            protected void onGoToCadastro() {
                showScreen(CADASTRO_PANEL);
            }
        };
        loginPanel.setName(LOGIN_PANEL);

        // Painel de Cadastro - passar registroServico
        CadastroUsuarioPanel cadastroPanel = new CadastroUsuarioPanel(usuarioServico, registroServico) {
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
                showScreen(LOGIN_PANEL);
            }
        };
        cadastroPanel.setName(CADASTRO_PANEL);

        // Painel de QR Code TOTP
        TotpQrCodePanel qrCodePanel = new TotpQrCodePanel() {
            @Override
            protected void onContinue() {
                showScreen(LOGIN_PANEL);
            }
        };
        qrCodePanel.setName(TOTP_QRCODE_PANEL);

        // Painel de validação TOTP - passar registroServico
        TotpValidationPanel totpValidationPanel = new TotpValidationPanel(registroServico) {
            @Override
            protected void onTotpValidated() {
                String codigo = getCodigoTotp();
                if (MainFrame.this.usuarioEmLogin == null) {
                    setStatus("Erro interno: Usuário não definido para validação TOTP.");
                    // Log de erro interno na GUI
                    // MainFrame.this.registroServico.registrarEventoDoSistema(MID_ERRO_INTERNO_TOTP_USUARIO_NULLO, "origem", "MainFrame.onTotpValidated");
                    return;
                }
                if (MainFrame.this.senhaEmLogin == null) {
                    setStatus("Erro interno: Senha original não disponível para descriptografar TOTP.");
                    return;
                }
                try {
                    String chaveSecreta = usuarioServico.obterChaveTotpDescriptografada(MainFrame.this.usuarioEmLogin, MainFrame.this.senhaEmLogin);
                    boolean valido = totpServico.validarCodigo(chaveSecreta, codigo);
                    if (valido) {
                        setStatus("TOTP válido! Login completo.");
                        // TODO: Navegar para a tela principal da aplicação pós-login
                        // JOptionPane.showMessageDialog(MainFrame.this, "Login totalmente concluído!");
                        // showScreen(MAIN_APP_PANEL); // Exemplo
                    } else {
                        setStatus("Código TOTP inválido.");
                        // TODO: Implementar lógica de tentativas de TOTP e bloqueio se necessário
                    }
                } catch (Exception ex) {
                    setStatus("Erro ao validar TOTP: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    MainFrame.this.senhaEmLogin = null;
                }
            }

            @Override
            protected void onBack() {
                // Log de voltar já está no painel
                MainFrame.this.usuarioEmLogin = null;
                MainFrame.this.senhaEmLogin = null;
                showScreen(LOGIN_PANEL);
                 // Ao voltar para o LoginPanel, o MainFrame já loga AUTH_ETAPA1_INICIADA quando o LoginPanel é mostrado.
            }
        };
        totpValidationPanel.setName(TOTP_VALIDATION_PANEL);

        // Novo Painel de Setup do Admin
        setupAdminPanel = new SetupAdminPanel(usuarioServico, registroServico, this);
        setupAdminPanel.setName(SETUP_ADMIN_PANEL);

        // Novo Painel de Validação de Frase Secreta do Admin
        validateAdminPassphrasePanel = new ValidateAdminPassphrasePanel(usuarioServico, registroServico, this);
        validateAdminPassphrasePanel.setName(VALIDATE_ADMIN_PASSPHRASE_PANEL);

        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(cadastroPanel, CADASTRO_PANEL);
        mainPanel.add(qrCodePanel, TOTP_QRCODE_PANEL);
        mainPanel.add(totpValidationPanel, TOTP_VALIDATION_PANEL);
        mainPanel.add(setupAdminPanel, SETUP_ADMIN_PANEL);
        mainPanel.add(validateAdminPassphrasePanel, VALIDATE_ADMIN_PASSPHRASE_PANEL);

        add(mainPanel);
    }

    private void determineInitialScreen() {
        if (usuarioServico.isFirstExecution()) {
            registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_PRIMEIRA_EXECUCAO);
            showScreen(SETUP_ADMIN_PANEL);
        } else {
            // Não é primeira execução, solicitar frase secreta do admin
            // O log VALIDATE_ADMIN_PASSPHRASE_TELA_APRESENTADA_GUI é feito no construtor do painel.
            // O log PARTIDA_SISTEMA_OPERACAO_NORMAL será feito após a validação bem sucedida da frase.
            showScreen(VALIDATE_ADMIN_PASSPHRASE_PANEL);
        }
    }

    // Método para trocar a tela visível
    public void showScreen(String panelName) {
        cardLayout.show(mainPanel, panelName);
        
        if (registroServico != null) {
            if (LOGIN_PANEL.equals(panelName)) {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA1_INICIADA); 
            } else if (CADASTRO_PANEL.equals(panelName)) {
                // Para CADASTRO_PANEL, precisamos saber se é um admin operando ou um novo usuário se auto-registrando (se permitido).
                // Se for um admin logado (usuarioEmLogin != null e é admin), logar com UID dele.
                // Por agora, um log genérico.
                Long operadorUid = (usuarioEmLogin != null) ? usuarioEmLogin.getId() : null;
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CAD_TELA_APRESENTADA_GUI, operadorUid, "contexto", "cadastro_usuario");
            } else if (TOTP_VALIDATION_PANEL.equals(panelName)) {
                TotpValidationPanel tvp = (TotpValidationPanel) getPanelByName(TOTP_VALIDATION_PANEL);
                if (tvp != null && usuarioEmLogin != null) {
                    tvp.setCurrentUserEmailForLog(usuarioEmLogin.getEmail());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_TELA_APRESENTADA_GUI, usuarioEmLogin.getId(), "email_usuario", usuarioEmLogin.getEmail());
                } else {
                    // Log de erro se não puder setar o email ou usuarioEmLogin for nulo
                    registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA3_TELA_APRESENTADA_GUI, "erro", "usuario_contexto_ausente_para_log_tela_totp");
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

            TotpQrCodePanel qrCodePanel = (TotpQrCodePanel) getPanelByName(TOTP_QRCODE_PANEL);
            qrCodePanel.setQrCodeImage(qrImage);
            qrCodePanel.setSecretKey(chaveSecreta);

            showScreen(TOTP_QRCODE_PANEL);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar QR Code: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            showScreen(LOGIN_PANEL);
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

    public void onSetupAdminSubmit(String caminhoCert, String caminhoChave, String fraseSecreta, String senhaPessoal) {
        registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_BOTAO_CONFIGURAR_PRESSIONADO_GUI,
                "caminho_cert", caminhoCert,
                "caminho_chave", caminhoChave
        );

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
                 registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN_SUCESSO, "uid_admin", String.valueOf(adminCriado.getId()), "email_admin", adminCriado.getEmail());
                registroServico.registrarEventoDoSistema(LogEventosMIDs.SETUP_ADMIN_SUCESSO_GUI, Map.of("emailAdmin", adminCriado.getEmail(), "uidAdmin", String.valueOf(adminCriado.getId()), "kidChaveiro", String.valueOf(adminCriado.getKid() != null ? adminCriado.getKid() : "N/A")));
                JOptionPane.showMessageDialog(this, "Administrador configurado com sucesso!\nE-mail: " + adminCriado.getEmail() + "\nNome: " + adminCriado.getNome(), "Setup Concluído", JOptionPane.INFORMATION_MESSAGE);
                
                this.usuarioEmCadastro = adminCriado;
                // showTotpQrCodePanel(adminCriado, senhaPessoal); // Comentado temporariamente para focar no fluxo de login pós-setup
                showScreen(LOGIN_PANEL);
            } else {
                registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN_FALHA, "motivo", "setupInitialAdmin retornou nulo");
                JOptionPane.showMessageDialog(this, "Falha ao configurar o administrador. Verifique os logs.", "Erro no Setup", JOptionPane.ERROR_MESSAGE);
                showScreen(SETUP_ADMIN_PANEL);
                setupAdminPanel.limparCampos();
            }

        } catch (Exception e) {
            registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_CADASTRO_ADMIN_FALHA, "erro", e.getMessage());
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
        // Este método é chamado pelo ValidateAdminPassphrasePanel quando a frase é validada com sucesso.
        registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_OPERACAO_NORMAL);
        // Aqui, o sistema está pronto para operar.
        // Como ainda não temos a tela principal da aplicação, vamos para a tela de Login.
        // Idealmente, buscaria o usuário admin e o colocaria no estado 'usuarioEmLogin'
        // para que o fluxo de login possa prosseguir para a etapa de senha pessoal.
        // Por agora, apenas redireciona para o LOGIN_PANEL.
        // (Se o admin acabou de ser configurado, ele precisará logar normalmente).
        showScreen(LOGIN_PANEL);
    }

    public void onAdminSetupComplete() {
        System.out.println("DEBUG: MainFrame.onAdminSetupComplete() FOI CHAMADO INESPERADAMENTE E AGORA ESTÁ INOFENSIVO.");
        // Limpar campos do setupAdminPanel pode ser feito no próprio painel ao concluir, ou aqui.
        if (setupAdminPanel != null) {
            // setupAdminPanel.limparCampos(); // Talvez a limpeza devesse ser aqui se este fosse o callback final.
        }
        // Nenhuma navegação aqui para evitar conflito.
    }

    public void onUserLoginRequired() {
        // ... existing code ...
    }
} 