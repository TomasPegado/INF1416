package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import java.util.List;
import java.util.Optional;
import java.util.Arrays; // Importar explicitamente se PasswordUtil for removido e Arrays não for usado

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {
    private final JTextField emailField;
    private final JButton loginButton;
    private final JButton cadastroButton;
    private final JLabel statusLabel;
    private final TecladoVirtualPanel tecladoVirtualPanel;
    private final UsuarioServico usuarioServico;
    // Duplicando a constante para uso na mensagem de feedback, ou torná-la pública em UsuarioServico
    private static final int MAX_TENTATIVAS_SENHA_CONFIG = 3; 

    public LoginPanel(UsuarioServico usuarioServico) {
        this.usuarioServico = usuarioServico;

        // Inicializar componentes de UI que NÃO dependem do teclado virtual diretamente para o callback
        emailField = new JTextField(20);
        loginButton = new JButton("Entrar");
        cadastroButton = new JButton("Cadastrar-se");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        loginButton.setEnabled(false); // Botão de login começa desabilitado

        // 1. Inicializa TecladoVirtualPanel
        this.tecladoVirtualPanel = new TecladoVirtualPanel();
        
        // 2. Configura o callback APÓS a inicialização completa do tecladoVirtualPanel
        this.tecladoVirtualPanel.setOnInteractionCallback(() -> {
            // Agora é seguro referenciar this.tecladoVirtualPanel e loginButton
            int passwordLength = this.tecladoVirtualPanel.getSequenciaDeParesSelecionados().size();
            loginButton.setEnabled(passwordLength >= 8 && passwordLength <= 10);
        });

        // Configuração do Layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        
        // Linha 0: Email
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);

        // Linha 1: Teclado Virtual
        gbc.gridx = 0; gbc.gridy++; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Senha (Teclado Virtual):"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(this.tecladoVirtualPanel, gbc);

        // Linha 2: Botão Login
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);
        
        // Linha 3: Botão Cadastro
        gbc.gridy++;
        add(cadastroButton, gbc);

        // Linha 4: Status Label
        gbc.gridy++;
        add(statusLabel, gbc);

        // Action Listeners
        loginButton.addActionListener((ActionEvent e) -> {
            String email = emailField.getText().trim();
            // Acessa tecladoVirtualPanel do LoginPanel
            List<Character[]> sequenciaPares = this.tecladoVirtualPanel.getSequenciaDeParesSelecionados();

            if (email.isEmpty()) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Email não pode estar vazio.");
                return;
            }
            
            if (sequenciaPares.size() < 8 || sequenciaPares.size() > 10) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Senha deve ter entre 8 e 10 dígitos.");
                return;
            }

            try {
                statusLabel.setText("Verificando..."); // Feedback imediato
                Optional<String> senhaAutenticadaOpt = usuarioServico.autenticarComTecladoVirtual(email, sequenciaPares);

                if (senhaAutenticadaOpt.isPresent()) {
                    String senhaPlanaAutenticada = senhaAutenticadaOpt.get(); 
                    statusLabel.setForeground(Color.GREEN);
                    statusLabel.setText("Login realizado com sucesso! Prosseguindo...");
                    
                    onLoginSuccess(email, senhaPlanaAutenticada); // Passa email e senha para a próxima etapa
                    
                    // Limpar campos após sucesso para segurança e nova entrada.
                    // emailField.setText(""); // Opcional: limpar email ou não
                    tecladoVirtualPanel.limparSenha(); // Limpa o teclado virtual

                } else {
                    Usuario usuario = usuarioServico.buscarPorEmail(email); 
                    statusLabel.setForeground(Color.RED);
                    if (usuario != null && usuario.isAcessoBloqueado()) {
                        statusLabel.setText("Conta bloqueada por 2 minutos. Tente mais tarde.");
                    } else if (usuario == null) {
                        statusLabel.setText("Email não cadastrado."); 
                    } else { // Usuário existe, não está bloqueado, mas a senha falhou
                        int tentativasRestantes = MAX_TENTATIVAS_SENHA_CONFIG - usuario.getTentativasFalhasSenha();
                        if (usuario.getTentativasFalhasSenha() < MAX_TENTATIVAS_SENHA_CONFIG) {
                            statusLabel.setText("Email ou senha incorreta. Tentativas restantes: " + (tentativasRestantes > 0 ? tentativasRestantes : 0));
                        } else {
                            // Esta mensagem pode ser redundante se a anterior de bloqueio já foi exibida após o bloqueio efetivo.
                            // No entanto, se o bloqueio ocorreu nesta exata tentativa, o UsuarioServiço já bloqueou.
                            // O próximo login já cairia no if (usuario.isAcessoBloqueado())
                            statusLabel.setText("Email ou senha incorreta. Conta será bloqueada na próxima falha ou já está.");
                        }
                    }
                    this.tecladoVirtualPanel.limparSenha(); 
                }
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Erro durante o login: " + ex.getMessage());
                ex.printStackTrace(); 
                if (this.tecladoVirtualPanel != null) {
                     this.tecladoVirtualPanel.limparSenha();
                }
            }
        });

        cadastroButton.addActionListener((ActionEvent e) -> {
            onGoToCadastro();
        });
    }

    // Modificado para passar dados necessários para a próxima etapa (ex: TOTP)
    protected void onLoginSuccess(String email, String senhaPlanaVerificada) {
        System.out.println("Login bem-sucedido para: " + email + ". Navegar para a próxima tela (TOTP).");
        // A senhaPlanaVerificada pode ser usada aqui ou passada para o MainFrame
        // para ser usada na obtenção da chave TOTP antes de exibir o painel TOTP.
        // Por segurança, se não for usada imediatamente aqui, deve ser gerenciada com cuidado.
    }
    
    protected void onGoToCadastro() {
        System.out.println("Ir para a tela de cadastro.");
    }

    public String getEmail() {
        return emailField.getText().trim();
    }
} 