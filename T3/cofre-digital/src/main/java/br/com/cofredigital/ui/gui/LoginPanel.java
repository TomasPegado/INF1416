package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.crypto.PasswordUtil;
import br.com.cofredigital.tecladovirtual.TecladoVirtualAuthInput;
import java.util.Arrays;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {
    private final JTextField emailField = new JTextField(20);
    private final JButton loginButton = new JButton("Entrar");
    private final JButton cadastroButton = new JButton("Cadastrar-se");
    private final JLabel statusLabel = new JLabel(" ");
    private final TecladoVirtualPanel tecladoVirtualPanel;

    public LoginPanel(UsuarioServico usuarioServico) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);
        gbc.gridy++;
        add(new JLabel("Senha (Teclado Virtual):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);
        gbc.gridy++;
        tecladoVirtualPanel = new TecladoVirtualPanel(
            Arrays.asList('0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'),
            null
        );
        add(tecladoVirtualPanel, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);
        gbc.gridy++;
        add(cadastroButton, gbc);
        gbc.gridy++;
        add(statusLabel, gbc);

        loginButton.addActionListener((ActionEvent e) -> {
            String email = emailField.getText().trim();
            char[] senha = tecladoVirtualPanel.getSenha();

            if (email.isEmpty() || senha.length == 0) {
                statusLabel.setText("Preencha todos os campos.");
                return;
            }
            try {
                if (!(usuarioServico instanceof TecladoVirtualAuthInput)) {
                    statusLabel.setText("Backend não suporta autenticação por teclado virtual.");
                    return;
                }
                boolean autenticado = ((TecladoVirtualAuthInput) usuarioServico)
                    .autenticarComTecladoVirtual(email, senha);
                if (autenticado) {
                    statusLabel.setText("Login realizado com sucesso!");
                    onLoginSuccess();
                } else {
                    statusLabel.setText("Senha incorreta.");
                }
            } catch (Exception ex) {
                statusLabel.setText("Erro: " + ex.getMessage());
            } finally {
                Arrays.fill(senha, '\0');
                tecladoVirtualPanel.limparSenha();
            }
        });

        cadastroButton.addActionListener((ActionEvent e) -> {
            onGoToCadastro();
        });
    }

    protected void onLoginSuccess() {}
    protected void onGoToCadastro() {}

    public String getEmail() {
        return emailField.getText().trim();
    }

    public String getSenha() {
        return new String(tecladoVirtualPanel.getSenha());
    }

} 