package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CadastroUsuarioPanel extends JPanel {
    private final JTextField nomeField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField senhaField = new JPasswordField(20);
    private final JPasswordField confirmarSenhaField = new JPasswordField(20);
    private final JButton cadastrarButton = new JButton("Cadastrar");
    private final JButton voltarLoginButton = new JButton("Voltar para Login");
    private final JLabel statusLabel = new JLabel(" ");

    public CadastroUsuarioPanel(UsuarioServico usuarioServico) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Nome:"), gbc);
        gbc.gridy++;
        add(new JLabel("Email:"), gbc);
        gbc.gridy++;
        add(new JLabel("Senha:"), gbc);
        gbc.gridy++;
        add(new JLabel("Confirmar Senha:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(nomeField, gbc);
        gbc.gridy++;
        add(emailField, gbc);
        gbc.gridy++;
        add(senhaField, gbc);
        gbc.gridy++;
        add(confirmarSenhaField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(cadastrarButton, gbc);
        gbc.gridy++;
        add(voltarLoginButton, gbc);
        gbc.gridy++;
        add(statusLabel, gbc);

        cadastrarButton.addActionListener((ActionEvent e) -> {
            String nome = nomeField.getText().trim();
            String email = emailField.getText().trim();
            String senha = new String(senhaField.getPassword());
            String confirmarSenha = new String(confirmarSenhaField.getPassword());

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                statusLabel.setText("Preencha todos os campos.");
                return;
            }
            if (!senha.equals(confirmarSenha)) {
                statusLabel.setText("As senhas não coincidem.");
                return;
            }
            try {
                Usuario usuario = new Usuario();
                usuario.setNome(nome);
                usuario.setEmail(email);
                usuarioServico.cadastrarUsuario(usuario, senha);
                statusLabel.setText("Usuário cadastrado com sucesso!");
                onCadastroSuccess();
            } catch (Exception ex) {
                statusLabel.setText("Erro: " + ex.getMessage());
            }
        });

        voltarLoginButton.addActionListener((ActionEvent e) -> {
            onGoToLogin();
        });
    }

    // Callbacks para serem sobrescritos pelo MainFrame
    protected void onCadastroSuccess() {}
    protected void onGoToLogin() {}
} 