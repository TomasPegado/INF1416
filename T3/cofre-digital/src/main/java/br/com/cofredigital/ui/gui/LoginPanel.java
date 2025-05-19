package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import java.util.List;
import java.util.Optional;
import java.util.Arrays; // Importar explicitamente se PasswordUtil for removido e Arrays não for usado

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;

public class LoginPanel extends JPanel {
    private final JTextField emailField;
    private final JButton loginButton;
    private final JLabel statusLabel;
    private final TecladoVirtualPanel tecladoVirtualPanel;
    private final UsuarioServico usuarioServico;
    private final RegistroServico registroServico;
    // Duplicando a constante para uso na mensagem de feedback, ou torná-la pública em UsuarioServico
    private static final int MAX_TENTATIVAS_SENHA_CONFIG = 3; 

    public LoginPanel(UsuarioServico usuarioServico, RegistroServico registroServico) {
        this.usuarioServico = usuarioServico;
        this.registroServico = registroServico;

        // Inicializar componentes de UI que NÃO dependem do teclado virtual diretamente para o callback
        emailField = new JTextField(20);
        loginButton = new JButton("Entrar");
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
        
        // Linha 3: (Removido botão Cadastro)
        gbc.gridy++;
        // (Nada adicionado aqui)

        // Linha 4: Status Label
        gbc.gridy++;
        add(statusLabel, gbc);

        // Action Listeners
        loginButton.addActionListener((ActionEvent e) -> {
            String email = emailField.getText().trim();
            List<Character[]> sequenciaPares = this.tecladoVirtualPanel.getSequenciaDeParesSelecionados();

            // Log da tentativa de login pela GUI
            // UID é desconhecido neste ponto, então registramos como evento do sistema ou com UID nulo.
            this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_TENTATIVA_LOGIN_GUI, "email_tentativa", email);

            if (email.isEmpty()) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Email não pode estar vazio.");
                this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_DADOS_INVALIDOS_GUI, "email_tentativa", email, "motivo", "email_vazio");
                return;
            }
            
            if (sequenciaPares.size() < 8 || sequenciaPares.size() > 10) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Senha deve ter entre 8 e 10 dígitos.");
                this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_DADOS_INVALIDOS_GUI, "email_tentativa", email, "motivo", "senha_tamanho_invalido");
                return;
            }

            try {
                statusLabel.setText("Verificando...");
                Optional<String> senhaAutenticadaOpt = usuarioServico.autenticarComTecladoVirtual(email, sequenciaPares);

                // Os logs detalhados de sucesso (AUTH_SENHA_OK) ou falha (AUTH_SENHA_ERRO1/2/3, AUTH_LOGIN_BLOQUEADO)
                // já são feitos DENTRO de usuarioServico.autenticarComTecladoVirtual.
                // Aqui, a GUI apenas reage ao resultado.

                if (senhaAutenticadaOpt.isPresent()) {
                    String senhaPlanaAutenticada = senhaAutenticadaOpt.get(); 
                    statusLabel.setForeground(Color.GREEN);
                    statusLabel.setText("Login realizado com sucesso! Prosseguindo...");
                    onLoginSuccess(email, senhaPlanaAutenticada); 
                    tecladoVirtualPanel.limparSenha();
                } else {
                    // A lógica de feedback da GUI permanece, baseada no estado do usuário retornado implicitamente pela falha.
                    Usuario usuario = usuarioServico.buscarPorEmail(email); 
                    statusLabel.setForeground(Color.RED);
                    if (usuario != null && usuario.isAcessoBloqueado()) {
                        statusLabel.setText("Conta bloqueada por 2 minutos. Tente mais tarde.");
                    } else if (usuario == null) {
                        statusLabel.setText("Email não cadastrado."); 
                    } else { 
                        int tentativasRestantes = MAX_TENTATIVAS_SENHA_CONFIG - usuario.getTentativasFalhasSenha();
                        if (usuario.getTentativasFalhasSenha() < MAX_TENTATIVAS_SENHA_CONFIG) {
                            statusLabel.setText("Email ou senha incorreta. Tentativas restantes: " + (tentativasRestantes > 0 ? tentativasRestantes : 0));
                        } else {
                            statusLabel.setText("Email ou senha incorreta. Conta será bloqueada na próxima falha ou já está.");
                        }
                    }
                    this.tecladoVirtualPanel.limparSenha(); 
                }
            } catch (Exception ex) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Erro durante o login: " + ex.getMessage());
                // Log de erro inesperado na GUI durante o login
                // Poderíamos ter um MID_ERRO_INESPERADO_LOGIN_GUI
                this.registroServico.registrarEventoDoSistema(LogEventosMIDs.AUTH_ETAPA2_ENCERRADA, "email_tentativa", email, "resultado", "excecao_gui", "erro", ex.getMessage());
                ex.printStackTrace(); 
                if (this.tecladoVirtualPanel != null) {
                     this.tecladoVirtualPanel.limparSenha();
                }
            }
        });
    }

    // Modificado para passar dados necessários para a próxima etapa (ex: TOTP)
    protected void onLoginSuccess(String email, String senhaPlanaVerificada) {
        Usuario usuario = usuarioServico.buscarPorEmail(email);
        Long uid = (usuario != null) ? usuario.getId() : null;
        registroServico.registrarEventoDoUsuario(LogEventosMIDs.AUTH_SENHA_OK, uid, "email", email, "mensagem", "Login realizado com sucesso na GUI");
        System.out.println("Login bem-sucedido para: " + email + ". Navegar para a próxima tela (TOTP).");
        // A senhaPlanaVerificada pode ser usada aqui ou passada para o MainFrame
        // para ser usada na obtenção da chave TOTP antes de exibir o painel TOTP.
        // Por segurança, se não for usada imediatamente aqui, deve ser gerenciada com cuidado.
    }

    public String getEmail() {
        return emailField.getText().trim();
    }

    public void resetFields() {
        emailField.setText("");
        statusLabel.setText("");
        statusLabel.setForeground(Color.RED);
        if (tecladoVirtualPanel != null) {
            tecladoVirtualPanel.limparSenha();
        }
        loginButton.setEnabled(false);
    }
} 