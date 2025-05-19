package br.com.cofredigital.ui.gui;

import javax.swing.*;
import java.awt.*;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;

public class TotpValidationPanel extends JPanel {
    private final JLabel instrucaoLabel = new JLabel("Digite o código de 6 dígitos do seu app autenticador:");
    private final JTextField codigoField = new JTextField(8);
    private final JButton validarButton = new JButton("Validar");
    private final JButton voltarButton = new JButton("Voltar");
    private final JLabel statusLabel = new JLabel(" ");
    private final RegistroServico registroServico;
    private String currentUserEmailForLog;

    public TotpValidationPanel(RegistroServico registroServico) {
        this.registroServico = registroServico;
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

        validarButton.addActionListener(e -> {
            String codigo = getCodigoTotp();
            this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_BOTAO_VALIDAR_PRESSIONADO_GUI, null /* idealmente UID */, "email_usuario", currentUserEmailForLog, "codigo_tentativa", codigo);

            if (codigo.isEmpty() || !codigo.matches("\\d{6}")) {
                setStatus("Código TOTP deve ter 6 dígitos numéricos.");
                this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_CODIGO_INVALIDO_FORMATO_GUI, null /* UID */, "email_usuario", currentUserEmailForLog, "codigo_fornecido", codigo);
                return;
            }
            onTotpValidated();
        });

        voltarButton.addActionListener(e -> {
            this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA3_BOTAO_VOLTAR_PRESSIONADO_GUI, null /* UID */, "email_usuario", currentUserEmailForLog);
            onBack();
        });
    }

    public String getCodigoTotp() {
        return codigoField.getText().trim();
    }

    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    public void setCurrentUserEmailForLog(String email) {
        this.currentUserEmailForLog = email;
    }

    protected void onTotpValidated() {}
    protected void onBack() {}

    public void resetFields() {
        codigoField.setText("");
        statusLabel.setText(" ");
    }
} 