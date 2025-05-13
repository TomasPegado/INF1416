package br.com.cofredigital.ui.gui;

import javax.swing.*;
import java.awt.*;

public class TotpValidationPanel extends JPanel {
    private final JLabel instrucaoLabel = new JLabel("Digite o código de 6 dígitos do seu app autenticador:");
    private final JTextField codigoField = new JTextField(8);
    private final JButton validarButton = new JButton("Validar");
    private final JButton voltarButton = new JButton("Voltar");
    private final JLabel statusLabel = new JLabel(" ");

    public TotpValidationPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(instrucaoLabel, gbc);

        gbc.gridy++;
        add(codigoField, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(validarButton, gbc);
        gbc.gridx = 1;
        add(voltarButton, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        add(statusLabel, gbc);

        validarButton.addActionListener(e -> onTotpValidated());
        voltarButton.addActionListener(e -> onBack());
    }

    public String getCodigoTotp() {
        return codigoField.getText().trim();
    }

    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    protected void onTotpValidated() {}
    protected void onBack() {}
} 