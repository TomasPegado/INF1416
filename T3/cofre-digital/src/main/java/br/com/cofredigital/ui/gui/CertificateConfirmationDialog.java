// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;

public class CertificateConfirmationDialog extends JDialog {

    private boolean confirmado = false;
    private final RegistroServico registroServico;
    private final Long adminUid;

    public CertificateConfirmationDialog(Frame owner, String title, Map<String, String> certificateData, RegistroServico registroServico, Long adminUid) {
        super(owner, title, true); // true para modal
        this.registroServico = registroServico;
        this.adminUid = adminUid;
        initComponents(certificateData);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents(Map<String, String> data) {
        setLayout(new BorderLayout(10, 10));

        JPanel dataPanel = new JPanel(new GridBagLayout());
        dataPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int gridY = 0;
        // Ordem dos campos conforme especificado no roteiro
        String[] fieldOrder = {
            "Sujeito (Friendly Name)", "E-mail", "Versão", "Série", 
            "Validade (Início)", "Validade (Fim)", "Tipo de Assinatura", "Emissor"
        };

        for (String key : fieldOrder) {
            if (data.containsKey(key)) {
                gbc.gridx = 0;
                gbc.gridy = gridY;
                dataPanel.add(new JLabel(key + ":"), gbc);

                gbc.gridx = 1;
                gbc.gridy = gridY;
                JLabel valueLabel = new JLabel("<html>" + data.get(key).replace(",", ",<br>") + "</html>"); // Quebra de linha para DNs longos
                dataPanel.add(valueLabel, gbc);
                gridY++;
            }
        }
        
        // Adicionar outros campos que possam estar no map mas não na ordem prioritária
        for (Map.Entry<String, String> entry : data.entrySet()) {
            boolean alreadyAdded = false;
            for (String orderedKey : fieldOrder) {
                if (orderedKey.equals(entry.getKey())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                gbc.gridx = 0;
                gbc.gridy = gridY;
                dataPanel.add(new JLabel(entry.getKey() + ":"), gbc);

                gbc.gridx = 1;
                gbc.gridy = gridY;
                JLabel valueLabel = new JLabel("<html>" + entry.getValue().replace(",", ",<br>") + "</html>");
                dataPanel.add(valueLabel, gbc);
                gridY++;
            }
        }

        JScrollPane scrollPane = new JScrollPane(dataPanel);
        scrollPane.setPreferredSize(new Dimension(450, 300)); // Ajustar tamanho conforme necessário

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnConfirmar = new JButton("Confirmar Dados e Prosseguir");
        JButton btnCancelar = new JButton("Cancelar Cadastro");

        btnConfirmar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmado = true;
                if (registroServico != null) {
                    String email = data.getOrDefault("E-mail", "(desconhecido)");
                    registroServico.registrarEventoDoUsuario(
                        LogEventosMIDs.CAD_CONFIRMACAO_DADOS_ACEITA,
                        adminUid,
                        "login_name", email
                    );
                }
                dispose();
            }
        });

        btnCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmado = false;
                if (registroServico != null) {
                    String email = data.getOrDefault("E-mail", "(desconhecido)");
                    registroServico.registrarEventoDoUsuario(
                        LogEventosMIDs.CAD_CONFIRMACAO_DADOS_REJEITADA,
                        adminUid,
                        "login_name", email
                    );
                }
                dispose();
            }
        });

        buttonPanel.add(btnConfirmar);
        buttonPanel.add(btnCancelar);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmado() {
        return confirmado;
    }
} 