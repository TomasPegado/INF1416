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

    public UserMainPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblWelcome = new JLabel("Painel do Usuário Comum - Em Desenvolvimento", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 16));
        add(lblWelcome, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(0, 1, 10, 10)); // Para organizar os botões
        centerPanel.setBorder(BorderFactory.createTitledBorder("Menu Principal"));
        
        // Botão Placeholder para futuras funcionalidades
        btnConsultarPasta = new JButton("2 – Consultar pasta de arquivos secretos do usuário");
        btnConsultarPasta.addActionListener(e -> {
            JOptionPane.showMessageDialog(UserMainPanel.this, 
                "Funcionalidade 'Consultar Pasta' ainda não implementada.", 
                "Aviso", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        // centerPanel.add(btnConsultarPasta); // Adicionar se quiser que apareça

        btnSair = new JButton("3 – Sair do Sistema");
        btnSair.addActionListener(e -> {
            if (this.mainFrame != null && this.usuarioLogado != null) {
                this.mainFrame.showLogoutExitPanel(this.usuarioLogado);
            }
        });
        
        centerPanel.add(btnConsultarPasta); // Adicionando para seguir o layout do Admin
        centerPanel.add(btnSair);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void preparePanel(Usuario usuario) {
        this.usuarioLogado = usuario;
        if (usuario != null) {
            lblWelcome.setText("Bem-vindo(a), " + usuario.getNome() + "! (Grupo: " + usuario.getGrupo() + ")");
            // // if (mainFrame.getRegistroServico() != null) {
            // //     mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.USER_MAIN_TELA_APRESENTADA_GUI, usuario.getId(), "user_email", usuario.getEmail());
            // // }
        } else {
            lblWelcome.setText("Painel do Usuário Comum - Usuário não definido.");
        }
    }
} 