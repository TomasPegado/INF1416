package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AdminMainPanel extends JPanel {

    private MainFrame mainFrame;
    private UsuarioServico usuarioServico;
    private Usuario adminLogado;

    // Cabeçalho
    private JLabel lblLoginValue;
    private JLabel lblGrupoValue;
    private JLabel lblNomeValue;

    // Corpo 1
    private JLabel lblTotalAcessosValue;

    // Corpo 2 - Menu Principal
    private JButton btnCadastrarUsuario;
    private JButton btnConsultarPasta;
    private JButton btnSair;

    public AdminMainPanel(MainFrame mainFrame, UsuarioServico usuarioServico) {
        this.mainFrame = mainFrame;
        this.usuarioServico = usuarioServico;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // Margens entre as seções

        // --- Painel do Cabeçalho ---
        JPanel headerPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // 0 linhas, 1 coluna, espaçamento vertical
        headerPanel.setBorder(BorderFactory.createTitledBorder("Informações do Administrador"));
        lblLoginValue = new JLabel("N/A");
        lblGrupoValue = new JLabel("N/A");
        lblNomeValue = new JLabel("N/A");
        headerPanel.add(new JLabel("Login:"));
        headerPanel.add(lblLoginValue);
        headerPanel.add(new JLabel("Grupo:"));
        headerPanel.add(lblGrupoValue);
        headerPanel.add(new JLabel("Nome:"));
        headerPanel.add(lblNomeValue);
        add(headerPanel, BorderLayout.NORTH);

        // --- Painel Central (Corpo 1 e Corpo 2) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // --- Painel do Corpo 1 ---
        JPanel body1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        body1Panel.setBorder(BorderFactory.createTitledBorder("Estatísticas"));
        lblTotalAcessosValue = new JLabel("N/A");
        body1Panel.add(new JLabel("Total de acessos do usuário:"));
        body1Panel.add(lblTotalAcessosValue);
        centerPanel.add(body1Panel, BorderLayout.NORTH);

        // --- Painel do Corpo 2 (Menu) ---
        JPanel body2Panel = new JPanel(new GridLayout(0, 1, 10, 10)); // 0 linhas, 1 coluna, espaçamento
        body2Panel.setBorder(BorderFactory.createTitledBorder("Menu Principal"));
        btnCadastrarUsuario = new JButton("1 – Cadastrar um novo usuário");
        btnConsultarPasta = new JButton("2 – Consultar pasta de arquivos secretos do usuário");
        btnSair = new JButton("3 – Sair do Sistema");

        btnCadastrarUsuario.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ação para Cadastrar Usuário
                if (adminLogado != null) {
                    mainFrame.showUserRegistrationPanel(adminLogado);
                } else {
                    JOptionPane.showMessageDialog(AdminMainPanel.this, "Erro: Administrador não logado.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnConsultarPasta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ação para Consultar Pasta (Placeholder)
                JOptionPane.showMessageDialog(AdminMainPanel.this, "Funcionalidade 'Consultar Pasta' ainda não implementada.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnSair.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ação para Sair
                mainFrame.logout(); // Assume que MainFrame tem um método logout()
            }
        });

        body2Panel.add(btnCadastrarUsuario);
        body2Panel.add(btnConsultarPasta);
        body2Panel.add(btnSair);
        centerPanel.add(body2Panel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    public void setAdminLogado(Usuario admin) {
        this.adminLogado = admin;
        if (admin != null) {
            lblLoginValue.setText(admin.getEmail());
            lblGrupoValue.setText(admin.getGrupo());
            lblNomeValue.setText(admin.getNome());
            // Para total de acessos, precisaremos de um getter em Usuario.java
            // Supondo que exista um admin.getTotalAcessos()
             try {
                // A classe Usuario precisa ter o método getTotalAcessos()
                // Se não tiver, precisará ser adicionado.
                // Exemplo: lblTotalAcessosValue.setText(String.valueOf(admin.getTotalAcessos()));
                // Por enquanto, vamos deixar como N/A ou buscar do DAO se o modelo não tiver
                Usuario usuarioCompleto = usuarioServico.buscarPorId(admin.getId());
                if (usuarioCompleto != null) { // O método getTotalAcessos() em um int não será null
                     lblTotalAcessosValue.setText(String.valueOf(usuarioCompleto.getTotalAcessos()));
                } else {
                     lblTotalAcessosValue.setText("Usuário não encontrado");
                }

            } catch (Exception e) {
                lblTotalAcessosValue.setText("Erro ao buscar acessos");
                 System.err.println("Erro ao buscar total de acessos para o admin: " + e.getMessage());
            }
        } else {
            lblLoginValue.setText("N/A");
            lblGrupoValue.setText("N/A");
            lblNomeValue.setText("N/A");
            lblTotalAcessosValue.setText("N/A");
        }
    }
} 