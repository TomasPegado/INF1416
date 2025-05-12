package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.crypto.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField senhaField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Entrar");
    private final JButton cadastroButton = new JButton("Cadastrar-se");
    private final JLabel statusLabel = new JLabel(" ");

    public LoginPanel(UsuarioServico usuarioServico) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);
        gbc.gridy++;
        add(new JLabel("Senha:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);
        gbc.gridy++;
        add(senhaField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);
        gbc.gridy++;
        add(cadastroButton, gbc);
        gbc.gridy++;
        add(statusLabel, gbc);

        loginButton.addActionListener((ActionEvent e) -> {
            String email = emailField.getText().trim();
            String senha = new String(senhaField.getPassword());

            if (email.isEmpty() || senha.isEmpty()) {
                statusLabel.setText("Preencha todos os campos.");
                return;
            }
            try {
                Usuario usuario = usuarioServico.buscarPorEmail(email);
                if (PasswordUtil.checkPassword(senha, usuario.getSenha())) {
                    statusLabel.setText("Login realizado com sucesso!");
                    onLoginSuccess();
                } else {
                    statusLabel.setText("Senha incorreta.");
                }
            } catch (Exception ex) {
                statusLabel.setText("Erro: " + ex.getMessage());
            }
        });

        cadastroButton.addActionListener((ActionEvent e) -> {
            onGoToCadastro();
        });
    }

    protected void onLoginSuccess() {}
    protected void onGoToCadastro() {}

//     public class PasswordUtil {
//         public static boolean checkPassword(String plain, String hash) {
//         // implementação...
//     }
// }
} 