package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import javax.swing.*;
import java.awt.*;

public class UserMainPanel extends JPanel {
    private MainFrame mainFrame;
    private Usuario usuarioLogado;

    private JLabel lblWelcome;
    private JButton btnConsultarPasta; // Placeholder, se necessário adicionar outras opções depois
    private JButton btnSair;
    private JLabel lblLoginValue;
    private JLabel lblGrupoValue;
    private JLabel lblNomeValue;
    private JLabel lblTotalAcessosValue;

    public UserMainPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Painel do Cabeçalho ---
        JPanel headerPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Informações do Usuário"));
        lblLoginValue = new JLabel("N/A");
        lblGrupoValue = new JLabel("Usuário");
        lblNomeValue = new JLabel("N/A");
        headerPanel.add(new JLabel("Login:"));
        headerPanel.add(lblLoginValue);
        headerPanel.add(new JLabel("Grupo:"));
        headerPanel.add(lblGrupoValue);
        headerPanel.add(new JLabel("Nome:"));
        headerPanel.add(lblNomeValue);
        add(headerPanel, BorderLayout.NORTH);

        // --- Painel Central (Corpo 2) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        JPanel body2Panel = new JPanel(new GridLayout(0, 1, 10, 10));
        body2Panel.setBorder(BorderFactory.createTitledBorder("Menu Principal"));

        // Total de acessos
        JPanel acessosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTotalAcessosValue = new JLabel("0");
        acessosPanel.add(new JLabel("Total de acessos do usuário:"));
        acessosPanel.add(lblTotalAcessosValue);
        body2Panel.add(acessosPanel);

        // Botão para consultar pasta (placeholder)
        btnConsultarPasta = new JButton("Consultar pasta de arquivos secretos do usuário");
        btnConsultarPasta.addActionListener(e -> {
            if (mainFrame != null && usuarioLogado != null) {
                mainFrame.showConsultarArquivosSecretosPanel();
            }
        });
        body2Panel.add(btnConsultarPasta);

        // Botão Sair
        btnSair = new JButton("Sair do Sistema");
        btnSair.addActionListener(e -> {
            if (this.mainFrame != null && this.usuarioLogado != null) {
                this.mainFrame.showLogoutExitPanel(this.usuarioLogado);
            }
        });
        body2Panel.add(btnSair);

        centerPanel.add(body2Panel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void preparePanel(Usuario usuario) {
        this.usuarioLogado = usuario;
        if (usuario != null) {
            lblLoginValue.setText(usuario.getEmail());
            lblGrupoValue.setText("Usuário");
            lblNomeValue.setText(usuario.getNome());
            lblTotalAcessosValue.setText(String.valueOf(usuario.getTotalAcessos()));
            // // if (mainFrame.getRegistroServico() != null) {
            // //     mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.USER_MAIN_TELA_APRESENTADA_GUI, usuario.getId(), "user_email", usuario.getEmail());
            // // }
        } else {
            lblLoginValue.setText("N/A");
            lblGrupoValue.setText("Usuário");
            lblNomeValue.setText("N/A");
            lblTotalAcessosValue.setText("0");
            lblWelcome.setText("Painel do Usuário Comum - Usuário não definido.");
        }
    }
} 