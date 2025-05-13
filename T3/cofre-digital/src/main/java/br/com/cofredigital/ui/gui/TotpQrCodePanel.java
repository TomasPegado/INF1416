package br.com.cofredigital.ui.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TotpQrCodePanel extends JPanel {
    private final JLabel instrucaoLabel = new JLabel("Escaneie o QR Code no seu app autenticador.");
    private final JLabel qrCodeLabel = new JLabel();
    private final JLabel chaveLabel = new JLabel();
    private final JButton continuarButton = new JButton("Continuar");

    public TotpQrCodePanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(instrucaoLabel, gbc);

        gbc.gridy++;
        add(qrCodeLabel, gbc);

        gbc.gridy++;
        add(chaveLabel, gbc);

        gbc.gridy++;
        add(continuarButton, gbc);

        continuarButton.addActionListener(e -> onContinue());
    }

    public void setQrCodeImage(BufferedImage img) {
        qrCodeLabel.setIcon(new ImageIcon(img));
    }

    public void setSecretKey(String key) {
        chaveLabel.setText("Chave secreta: " + key);
    }

    protected void onContinue() {}
} 