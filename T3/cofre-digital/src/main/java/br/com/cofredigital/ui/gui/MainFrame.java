package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;

import javax.swing.JFrame;
import javax.swing.JPanel;
// Import javax.swing.SwingUtilities; // Não diretamente necessário aqui, mas útil para updates de UI
import java.awt.CardLayout;

public class MainFrame extends JFrame {

    private final UsuarioServico usuarioServico;
    private final TotpServico totpServico;

    private CardLayout cardLayout;
    private JPanel mainPanel; // Painel que conterá outros painéis (telas)

    // Constantes para os nomes dos painéis (telas)
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String ADMIN_REGISTRATION_PANEL = "AdminRegistrationPanel";
    public static final String MAIN_MENU_PANEL = "MainMenuPanel";
    // Adicionar outras constantes conforme necessário (ex: VALIDATE_ADMIN_PASSPHRASE_PANEL)

    public MainFrame(UsuarioServico usuarioServico, TotpServico totpServico) {
        this.usuarioServico = usuarioServico;
        this.totpServico = totpServico;

        setTitle("Cofre Digital");
        setSize(800, 600); // Tamanho inicial, pode ser ajustado
        setLocationRelativeTo(null); // Centralizar na tela
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Encerrar aplicação ao fechar

        initComponents();
        determineInitialScreen();
    }

    private void initComponents() {
        // Configura o layout principal para alternar entre painéis (telas)
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // TODO: Criar e adicionar os painéis específicos (telas) ao mainPanel
        // Exemplo (quando AdminRegistrationPanel for criado):
        // AdminRegistrationPanel adminRegPanel = new AdminRegistrationPanel(this, usuarioServico, totpServico);
        // mainPanel.add(adminRegPanel, ADMIN_REGISTRATION_PANEL);

        // Adicionar outros painéis (LoginPanel, MainMenuPanel, etc.)

        // Adiciona o painel principal ao frame
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