// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.log.LogEventosMIDs; // Manter este import

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogoutExitPanel extends JPanel {

    private MainFrame mainFrame;
    private Usuario usuarioLogado;

    // Cabeçalho
    private JLabel lblHeaderLoginValue;
    private JLabel lblHeaderGrupoValue;
    private JLabel lblHeaderNomeValue;

    // Corpo 1
    private JLabel lblCorpo1TotalAcessosValue;

    // Corpo 2
    private JLabel lblCorpo2LoginValue;
    private JLabel lblCorpo2GrupoValue;
    private JLabel lblCorpo2NomeValue;
    private JLabel lblCorpo2TotalAcessosUsuarioValue; 

    private JButton btnEncerrarSessao;
    private JButton btnEncerrarSistema;
    private JButton btnVoltarMenuPrincipal;

    public LogoutExitPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Painel do Cabeçalho (Admin Info) ---
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createTitledBorder("Usuário Logado"));
        GridBagConstraints gbcHeader = new GridBagConstraints();
        gbcHeader.insets = new Insets(2, 5, 2, 5);
        gbcHeader.anchor = GridBagConstraints.WEST;

        gbcHeader.gridx = 0; gbcHeader.gridy = 0; headerPanel.add(new JLabel("Login:"), gbcHeader);
        gbcHeader.gridx = 1; lblHeaderLoginValue = new JLabel("N/A"); headerPanel.add(lblHeaderLoginValue, gbcHeader);
        gbcHeader.gridy++;
        gbcHeader.gridx = 0; headerPanel.add(new JLabel("Grupo:"), gbcHeader);
        gbcHeader.gridx = 1; lblHeaderGrupoValue = new JLabel("N/A"); headerPanel.add(lblHeaderGrupoValue, gbcHeader);
        gbcHeader.gridy++;
        gbcHeader.gridx = 0; headerPanel.add(new JLabel("Nome:"), gbcHeader);
        gbcHeader.gridx = 1; lblHeaderNomeValue = new JLabel("N/A"); headerPanel.add(lblHeaderNomeValue, gbcHeader);
        
        add(headerPanel, BorderLayout.NORTH);

        // --- Painel Central (Corpo 1 e Corpo 2) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10,10));

        // --- Corpo 1 ---
        JPanel corpo1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        corpo1Panel.setBorder(BorderFactory.createTitledBorder("Informações da Sessão"));
        corpo1Panel.add(new JLabel("Total de acessos do usuário corrente:"));
        lblCorpo1TotalAcessosValue = new JLabel("0");
        corpo1Panel.add(lblCorpo1TotalAcessosValue);
        centerPanel.add(corpo1Panel, BorderLayout.NORTH);

        // --- Corpo 2 ---
        JPanel corpo2Panel = new JPanel(new GridBagLayout());
        corpo2Panel.setBorder(BorderFactory.createTitledBorder("Opções de Saída"));
        GridBagConstraints gbcCorpo2 = new GridBagConstraints();
        gbcCorpo2.insets = new Insets(5, 5, 5, 5);
        gbcCorpo2.anchor = GridBagConstraints.WEST;
        
        int y = 0;
        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; corpo2Panel.add(new JLabel("Login:"), gbcCorpo2);
        gbcCorpo2.gridx = 1; lblCorpo2LoginValue = new JLabel("N/A"); corpo2Panel.add(lblCorpo2LoginValue, gbcCorpo2);
        y++;
        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; corpo2Panel.add(new JLabel("Grupo:"), gbcCorpo2);
        gbcCorpo2.gridx = 1; lblCorpo2GrupoValue = new JLabel("N/A"); corpo2Panel.add(lblCorpo2GrupoValue, gbcCorpo2);
        y++;
        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; corpo2Panel.add(new JLabel("Nome:"), gbcCorpo2);
        gbcCorpo2.gridx = 1; lblCorpo2NomeValue = new JLabel("N/A"); corpo2Panel.add(lblCorpo2NomeValue, gbcCorpo2);
        y++;
        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; corpo2Panel.add(new JLabel("Total de acessos do usuário:"), gbcCorpo2);
        gbcCorpo2.gridx = 1; lblCorpo2TotalAcessosUsuarioValue = new JLabel("0"); corpo2Panel.add(lblCorpo2TotalAcessosUsuarioValue, gbcCorpo2);
        y++;
        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; gbcCorpo2.gridwidth = 2;
        corpo2Panel.add(new JLabel("Saída do sistema: Pressione o botão Encerrar Sessão ou o botão Encerrar Sistema para confirmar."), gbcCorpo2);
        y++;

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnEncerrarSessao = new JButton("Encerrar Sessão");
        btnEncerrarSistema = new JButton("Encerrar Sistema");
        btnVoltarMenuPrincipal = new JButton("Voltar para o Menu Principal");
        
        buttonsPanel.add(btnEncerrarSessao);
        buttonsPanel.add(btnEncerrarSistema);
        buttonsPanel.add(btnVoltarMenuPrincipal);

        gbcCorpo2.gridx = 0; gbcCorpo2.gridy = y; gbcCorpo2.gridwidth = 2; gbcCorpo2.anchor = GridBagConstraints.CENTER;
        corpo2Panel.add(buttonsPanel, gbcCorpo2);
        
        centerPanel.add(corpo2Panel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Action Listeners
        btnEncerrarSessao.addActionListener(e -> {
            if (usuarioLogado != null && mainFrame.getRegistroServico() != null) {
                java.util.Map<String, String> detalhes = new java.util.HashMap<>();
                detalhes.put("login_name", usuarioLogado.getEmail());
                mainFrame.getRegistroServico().registrarEventoDoUsuario(
                    LogEventosMIDs.BOTAO_ENCERRAR_SESSAO,
                    usuarioLogado.getId(),
                    detalhes
                );
            }
            mainFrame.logout(); // Chama o método logout do MainFrame
        });

        btnEncerrarSistema.addActionListener(e -> {
            // // if (usuarioLogado != null && mainFrame.getRegistroServico() != null) {
            // //      mainFrame.getRegistroServico().registrarEventoDoUsuario(
            // //         LogEventosMIDs.SISTEMA_ENCERRAMENTO_SOLICITADO_USUARIO, 
            // //         usuarioLogado.getId(),
            // //         "email", usuarioLogado.getEmail()
            // //     );
            // // } else if (mainFrame.getRegistroServico() != null) {
            // //      mainFrame.getRegistroServico().registrarEventoDoSistema(LogEventosMIDs.SISTEMA_ENCERRAMENTO_SOLICITADO_USUARIO, "motivo", "Usuário não logado ou não identificado na tela de saída");
            // // }
            System.exit(0);
        });

        btnVoltarMenuPrincipal.addActionListener(e -> {
            if (usuarioLogado != null) {
                if ("Administrador".equalsIgnoreCase(usuarioLogado.getGrupo())) {
                    mainFrame.showScreen(MainFrame.ADMIN_MAIN_PANEL);
                } else {
                    mainFrame.showScreen(MainFrame.USER_MAIN_PANEL); 
                }
            } else {
                mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL); // Fallback to new email screen
            }
        });
    }

    public void prepareForm(Usuario usuario) {
        this.usuarioLogado = usuario;
        if (usuario != null) {
            lblHeaderLoginValue.setText(usuario.getEmail());
            lblHeaderGrupoValue.setText(usuario.getGrupo());
            lblHeaderNomeValue.setText(usuario.getNome());
            lblCorpo1TotalAcessosValue.setText(String.valueOf(usuario.getTotalAcessos()));
            lblCorpo2LoginValue.setText(usuario.getEmail());
            lblCorpo2GrupoValue.setText(usuario.getGrupo());
            lblCorpo2NomeValue.setText(usuario.getNome());
            lblCorpo2TotalAcessosUsuarioValue.setText(String.valueOf(usuario.getTotalAcessos()));
            if (mainFrame.getRegistroServico() != null) {
                java.util.Map<String, String> detalhes = new java.util.HashMap<>();
                detalhes.put("login_name", usuario.getEmail());
                mainFrame.getRegistroServico().registrarEventoDoUsuario(
                    LogEventosMIDs.TELA_SAIDA_APRESENTADA,
                    usuario.getId(),
                    detalhes
                );
            }
        } else {
            String na = "N/A";
            lblHeaderLoginValue.setText(na);
            lblHeaderGrupoValue.setText(na);
            lblHeaderNomeValue.setText(na);
            lblCorpo1TotalAcessosValue.setText("0");
            lblCorpo2LoginValue.setText(na);
            lblCorpo2GrupoValue.setText(na);
            lblCorpo2NomeValue.setText(na);
            lblCorpo2TotalAcessosUsuarioValue.setText("0");
            // //  if (mainFrame.getRegistroServico() != null) {
            // //      mainFrame.getRegistroServico().registrarEventoDoSistema(LogEventosMIDs.TELA_SAIDA_APRESENTADA_GUI, "motivo", "Usuário nulo ao preparar formulário de saída");
            // // }
        }
    }
} 