package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PasswordPanel extends JPanel {
    // Constants for display logic, should mirror UsuarioServico configuration
    private static final int MAX_TENTATIVAS_PERMITIDAS = 3;
    private static final int TEMPO_BLOQUEIO_MINUTOS = 2;

    private final JButton entrarButton;
    private final JLabel statusLabel;
    private final TecladoVirtualPanel tecladoVirtualPanel;
    private final UsuarioServico usuarioServico;
    private final MainFrame mainFrame;
    private final RegistroServico registroServico;

    private Usuario usuarioAtual;

    public PasswordPanel(UsuarioServico usuarioServico, RegistroServico registroServico, MainFrame mainFrame) {
        this.usuarioServico = usuarioServico;
        this.mainFrame = mainFrame;
        this.registroServico = registroServico;

        entrarButton = new JButton("Entrar");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        
        this.tecladoVirtualPanel = new TecladoVirtualPanel();
        
        entrarButton.setEnabled(false);

        this.tecladoVirtualPanel.setOnInteractionCallback(() -> {
            int passwordLength = this.tecladoVirtualPanel.getSequenciaDeParesSelecionados().size();
            entrarButton.setEnabled(passwordLength >= 8 && passwordLength <= 10);
        });

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label Senha
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Senha (Teclado Virtual):"), gbc);
        
        // Teclado Virtual
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(this.tecladoVirtualPanel, gbc);

        // Status Label
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(statusLabel, gbc);
        
        // Botão Entrar e Voltar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton voltarButton = new JButton("Voltar");
        buttonPanel.add(voltarButton);
        buttonPanel.add(entrarButton);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);

        voltarButton.addActionListener((ActionEvent e) -> {
            if (usuarioAtual != null) {
                // this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_BOTAO_VOLTAR_PRESSIONADO_GUI, usuarioAtual.getId(), "email", usuarioAtual.getEmail());
            } else {
                // this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_BOTAO_VOLTAR_PRESSIONADO_GUI_SEM_USUARIO);
            }
            mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL);
        });

        // Action Listener para Entrar
        entrarButton.addActionListener((ActionEvent e) -> {
            if (usuarioAtual == null) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Erro: Usuário não definido. Volte para a tela de email.");
                // this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_TENTATIVA_SEM_USUARIO_GUI);
                return;
            }

            List<Character[]> sequenciaPares = this.tecladoVirtualPanel.getSequenciaDeParesSelecionados();
            String email = usuarioAtual.getEmail();
            Long uid = usuarioAtual.getId();

            if (sequenciaPares.size() < 8 || sequenciaPares.size() > 10) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Senha deve ter entre 8 e 10 dígitos.");
                this.tecladoVirtualPanel.limparSenha();
                entrarButton.setEnabled(false);
                // this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_DADOS_INVALIDOS_GUI, uid, "email", email, "motivo", "tamanho_senha_invalido");
                return;
            }

            // this.registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_ETAPA2_TENTATIVA_SENHA_GUI, uid, "email", email);

            try {
                statusLabel.setText("Verificando senha...");
                statusLabel.setForeground(Color.BLACK);
                Optional<String> senhaAutenticadaOpt = usuarioServico.autenticarComTecladoVirtual(usuarioAtual, sequenciaPares);

                if (senhaAutenticadaOpt.isPresent()) {
                    String senhaPlanaAutenticada = senhaAutenticadaOpt.get();
                    statusLabel.setForeground(new Color(0,128,0));
                    statusLabel.setText("Senha verificada! Prosseguindo para TOTP...");
                    mainFrame.navigateToTotpValidation(usuarioAtual, senhaPlanaAutenticada);
                } else {
                    statusLabel.setForeground(Color.RED);
                    Usuario usuarioAposTentativa = usuarioServico.buscarPorEmail(email);
                    
                    if (usuarioAposTentativa == null) {
                         statusLabel.setText("Erro crítico. Tente novamente o login.");
                         mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL);
                         return;
                    }
                    
                    this.usuarioAtual = usuarioAposTentativa;

                    if (usuarioAtual.isAcessoBloqueado()) {
                        // User is now blocked (this was the 3rd fail or they were already blocked coming into this attempt)
                        String mensagemBloqueio = "Conta bloqueada por " + TEMPO_BLOQUEIO_MINUTOS + " minutos devido a tentativas de senha excedidas.";
                        statusLabel.setText(mensagemBloqueio); // Update status label as well, though dialog is primary
                        JOptionPane.showMessageDialog(PasswordPanel.this,
                            mensagemBloqueio,
                            "Acesso Bloqueado",
                            JOptionPane.WARNING_MESSAGE);
                        mainFrame.showScreen(MainFrame.EMAIL_VERIFICATION_PANEL); // Then navigate
                    } else {
                        // User is not blocked, but password was incorrect
                        int tentativasFeitas = usuarioAtual.getTentativasFalhasSenha();
                        int tentativasRestantes = MAX_TENTATIVAS_PERMITIDAS - tentativasFeitas;
                        statusLabel.setText("Senha incorreta. Tentativas restantes: " + Math.max(0, tentativasRestantes));
                    }
                    this.tecladoVirtualPanel.limparSenha();
                    entrarButton.setEnabled(false);
                }
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Erro durante a verificação da senha: " + ex.getMessage());
                ex.printStackTrace();
                this.tecladoVirtualPanel.limparSenha();
                entrarButton.setEnabled(false);
            }
        });
    }

    public void prepareForUser(Usuario usuario) {
        this.usuarioAtual = usuario;
        statusLabel.setText("Usuário: " + usuario.getEmail() + ". Insira sua senha.");
        statusLabel.setForeground(Color.BLACK);
        tecladoVirtualPanel.limparSenha();
        entrarButton.setEnabled(false);
    }

    public void resetPanel() {
        this.usuarioAtual = null;
        statusLabel.setText(" ");
        statusLabel.setForeground(Color.RED);
        tecladoVirtualPanel.limparSenha();
        entrarButton.setEnabled(false);
    }
}
