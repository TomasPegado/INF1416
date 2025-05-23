// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.log.servico.RegistroServico;

import javax.swing.*;
import java.awt.*;

public class ValidateAdminPassphrasePanel extends JPanel {
    private JPasswordField pwdFraseSecretaAdmin;
    private JButton btnValidar;
    private JButton btnSair;

    private final UsuarioServico usuarioServico;
    private final RegistroServico registroServico;
    private final MainFrame mainFrame;

    public ValidateAdminPassphrasePanel(UsuarioServico usuarioServico, RegistroServico registroServico, MainFrame mainFrame) {
        this.usuarioServico = usuarioServico;
        this.registroServico = registroServico;
        this.mainFrame = mainFrame;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setBackground(new Color(240, 240, 240)); // Um cinza claro

        initComponents();
        addListeners();

        
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Validação da Frase Secreta do Administrador", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(lblTitulo, gbc);

        gbc.gridy++;
        JLabel lblInstrucao = new JLabel("Por favor, insira a frase secreta da chave privada do administrador:");
        add(lblInstrucao, gbc);

        pwdFraseSecretaAdmin = new JPasswordField(25);
        gbc.gridy++;
        add(pwdFraseSecretaAdmin, gbc);

        JPanel panelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelBotoes.setOpaque(false); // Tornar o painel de botões transparente
        btnValidar = new JButton("Validar Frase");
        btnValidar.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSair = new JButton("Sair do Sistema");
        btnSair.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSair.setBackground(new Color(220, 50, 50)); // Vermelho para Sair
        btnSair.setForeground(Color.WHITE);

        panelBotoes.add(btnValidar);
        panelBotoes.add(btnSair);
        
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(panelBotoes, gbc);
    }

    private void addListeners() {
        btnValidar.addActionListener(e -> processarValidacao());
        pwdFraseSecretaAdmin.addActionListener(e -> processarValidacao()); // Permitir Enter no campo de senha

        btnSair.addActionListener(e -> {
            
            // Confirmar antes de sair
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja sair do sistema?",
                "Confirmar Saída",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0); // Encerra a aplicação
            }
        });
    }

    private void processarValidacao() {
        String fraseSecreta = new String(pwdFraseSecretaAdmin.getPassword());
        if (fraseSecreta.trim().isEmpty()) {
            
            JOptionPane.showMessageDialog(this, "A frase secreta não pode estar vazia.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            pwdFraseSecretaAdmin.requestFocus();
            return;
        }

        boolean isValid = usuarioServico.validateAdminPassphrase(fraseSecreta);

        if (isValid) {
            
            // Frase secreta validada, MainFrame deve ter armazenado na sessão do UsuarioServico.
            // Limpar campo para segurança
            pwdFraseSecretaAdmin.setText("");
            registroServico.registrarEventoDoSistema(LogEventosMIDs.PARTIDA_SISTEMA_OPERACAO_NORMAL);
            mainFrame.onAdminPassphraseValidated(); // Notifica o MainFrame
        } else {
            
            JOptionPane.showMessageDialog(this, 
                "A frase secreta do administrador é inválida. Verifique e tente novamente.", 
                "Falha na Validação", 
                JOptionPane.ERROR_MESSAGE);
            pwdFraseSecretaAdmin.setText("");
            pwdFraseSecretaAdmin.requestFocus();
        }
    }
    
    public void limparCampo() {
        pwdFraseSecretaAdmin.setText("");
    }

    @Override
    public void requestFocus() { // Sobrescrever para focar no campo de senha quando o painel é mostrado
        super.requestFocus();
        if (pwdFraseSecretaAdmin != null) {
            pwdFraseSecretaAdmin.requestFocusInWindow();
        }
    }
} 