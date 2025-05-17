package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.ui.gui.CadastroUsuarioPanel;
import br.com.cofredigital.ui.gui.LoginPanel;
import br.com.cofredigital.ui.gui.TotpQrCodePanel;
import br.com.cofredigital.ui.gui.TotpValidationPanel;
import br.com.cofredigital.autenticacao.modelo.Usuario;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
// Import javax.swing.SwingUtilities; // Não diretamente necessário aqui, mas útil para updates de UI
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;

public class MainFrame extends JFrame {

    private final UsuarioServico usuarioServico;
    private final TotpServico totpServico;

    private CardLayout cardLayout;
    private JPanel mainPanel; // Painel que conterá outros painéis (telas)

    // Constantes para os nomes dos painéis (telas)
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String CADASTRO_PANEL = "CadastroUsuarioPanel";
    public static final String TOTP_QRCODE_PANEL = "TotpQrCodePanel";
    public static final String TOTP_VALIDATION_PANEL = "TotpValidationPanel";
    // Adicionar outras constantes conforme necessário (ex: VALIDATE_ADMIN_PASSPHRASE_PANEL)

    // Estado temporário para integração do fluxo
    private Usuario usuarioEmCadastro;
    private String senhaEmCadastro;
    private Usuario usuarioEmLogin;
    private String senhaEmLogin; // Adicionado para armazenar a senha original temporariamente

    public MainFrame(UsuarioServico usuarioServico, TotpServico totpServico) {
        this.usuarioServico = usuarioServico;
        this.totpServico = totpServico;

        setTitle("Cofre Digital");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
        showScreen(LOGIN_PANEL);
    }

    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Painel de Login
        LoginPanel loginPanel = new LoginPanel(usuarioServico) {
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

        // Painel de Cadastro
        CadastroUsuarioPanel cadastroPanel = new CadastroUsuarioPanel(usuarioServico) {
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
                showScreen(LOGIN_PANEL);
            }
        };

        // Painel de QR Code TOTP
        TotpQrCodePanel qrCodePanel = new TotpQrCodePanel() {
            @Override
            protected void onContinue() {
                showScreen(LOGIN_PANEL);
            }
        };

        // Painel de validação TOTP
        TotpValidationPanel totpValidationPanel = new TotpValidationPanel() {
            @Override
            protected void onTotpValidated() {
                String codigo = getCodigoTotp();
                if (MainFrame.this.usuarioEmLogin == null) {
                    setStatus("Erro interno: Usuário não definido para validação TOTP.");
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
                MainFrame.this.usuarioEmLogin = null;
                MainFrame.this.senhaEmLogin = null;
                showScreen(LOGIN_PANEL);
            }
        };

        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(cadastroPanel, CADASTRO_PANEL);
        mainPanel.add(qrCodePanel, TOTP_QRCODE_PANEL);
        mainPanel.add(totpValidationPanel, TOTP_VALIDATION_PANEL);

        add(mainPanel);
    }

    private void determineInitialScreen() {
        // TODO: Lógica para determinar a tela inicial
        // 1. Verificar se existe um administrador (usando usuarioServico)
        // 2. Se não existir, mostrar ADMIN_REGISTRATION_PANEL
        // 3. Se existir, mostrar um painel para validar a frase secreta (ou o LOGIN_PANEL futuramente)
        
        // Exemplo inicial (provisório, assumindo que não há admin):
        // showScreen(ADMIN_REGISTRATION_PANEL); // Precisa que o painel esteja criado e adicionado
        System.out.println("[MainFrame] TODO: Implementar lógica de tela inicial (checar admin).");
        // Por enquanto, vamos deixar o frame vazio até termos o primeiro painel.
    }

    // Método para trocar a tela visível
    public void showScreen(String panelName) {
        cardLayout.show(mainPanel, panelName);
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
} 