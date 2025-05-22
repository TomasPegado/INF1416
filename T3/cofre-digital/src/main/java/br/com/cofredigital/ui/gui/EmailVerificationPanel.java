package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.util.exception.UsuarioNaoEncontradoException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class EmailVerificationPanel extends JPanel {
    private final JTextField emailField;
    private final JButton okButton;
    private final JButton limparButton;
    private final JLabel statusLabel;

    private final UsuarioServico usuarioServico;
    private final MainFrame mainFrame;
    private final RegistroServico registroServico;

    public EmailVerificationPanel(UsuarioServico usuarioServico, RegistroServico registroServico, MainFrame mainFrame) {
        this.usuarioServico = usuarioServico;
        this.mainFrame = mainFrame;
        this.registroServico = registroServico;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Email Label e Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        emailField = new JTextField(25);
        add(emailField, gbc);

        // Status Label
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        add(statusLabel, gbc);

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        okButton = new JButton("OK");
        limparButton = new JButton("Limpar");
        buttonPanel.add(okButton);
        buttonPanel.add(limparButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        // Action Listeners
        limparButton.addActionListener((ActionEvent e) -> {
            emailField.setText("");
            statusLabel.setText(" ");
            emailField.requestFocusInWindow();
        });

        okButton.addActionListener((ActionEvent e) -> {
            String email = emailField.getText().trim();
            statusLabel.setText(" ");

            if (email.isEmpty()) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Email não pode estar vazio.");
                return;
            }

            try {
                statusLabel.setText("Verificando email...");
                statusLabel.setForeground(Color.BLACK);
                Usuario usuarioValidado = usuarioServico.validarIdentificacaoUsuario(email);
                statusLabel.setForeground(new Color(0,128,0));
                statusLabel.setText("Email verificado. Redirecionando para senha...");
                mainFrame.navigateToPasswordPanel(usuarioValidado);
            } catch (UsuarioNaoEncontradoException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Email não cadastrado.");
            } catch (IllegalStateException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(ex.getMessage());
            } catch (IllegalArgumentException ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(ex.getMessage());
            } catch (RuntimeException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof SQLException) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Erro de banco de dados. Tente novamente.");
                    System.err.println("SQLException (via RuntimeException) em EmailVerificationPanel: " + cause.getMessage());
                    cause.printStackTrace();
                } else {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Erro inesperado ao verificar email (runtime).");
                    System.err.println("RuntimeException em EmailVerificationPanel: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Erro inesperado ao verificar email.");
                System.err.println("Exception em EmailVerificationPanel: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    public void resetPanel() {
        emailField.setText("");
        statusLabel.setText(" ");
        statusLabel.setForeground(Color.RED);
        SwingUtilities.invokeLater(() -> emailField.requestFocusInWindow());
    }
}
