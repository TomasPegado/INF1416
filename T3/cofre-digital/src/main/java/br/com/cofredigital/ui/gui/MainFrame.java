package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.ui.gui.CadastroUsuarioPanel;
import br.com.cofredigital.ui.gui.LoginPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
// Import javax.swing.SwingUtilities; // Não diretamente necessário aqui, mas útil para updates de UI
import java.awt.CardLayout;

public class MainFrame extends JFrame {

    private final UsuarioServico usuarioServico;
    private final TotpServico totpServico;

    private CardLayout cardLayout;
    private JPanel mainPanel; // Painel que conterá outros painéis (telas)

    // Constantes para os nomes dos painéis (telas)
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String CADASTRO_PANEL = "CadastroUsuarioPanel";
    public static final String ADMIN_REGISTRATION_PANEL = "AdminRegistrationPanel";
    public static final String MAIN_MENU_PANEL = "MainMenuPanel";
    // Adicionar outras constantes conforme necessário (ex: VALIDATE_ADMIN_PASSPHRASE_PANEL)

    public MainFrame(UsuarioServico usuarioServico, TotpServico totpServico) {
        this.usuarioServico = usuarioServico;
        this.totpServico = totpServico;

        setTitle("Cofre Digital");
        setSize(500, 350);
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
            protected void onLoginSuccess() {
                // Aqui você pode trocar para o painel principal do sistema, dashboard, etc.
                JOptionPane.showMessageDialog(MainFrame.this, "Login realizado com sucesso!");
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
                JOptionPane.showMessageDialog(MainFrame.this, "Cadastro realizado! Faça login.");
                showScreen(LOGIN_PANEL);
            }

            @Override
            protected void onGoToLogin() {
                showScreen(LOGIN_PANEL);
            }
        };

        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(cadastroPanel, CADASTRO_PANEL);

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

    // Getters para os serviços, se necessário pelos painéis filhos
    public UsuarioServico getUsuarioServico() {
        return usuarioServico;
    }

    public TotpServico getTotpServico() {
        return totpServico;
    }
} 