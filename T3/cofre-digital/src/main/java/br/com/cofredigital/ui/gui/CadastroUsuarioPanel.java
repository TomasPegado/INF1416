// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.log.LogEventosMIDs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CadastroUsuarioPanel extends JPanel {
    private final JTextField nomeField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField senhaField = new JPasswordField(20);
    private final JPasswordField confirmarSenhaField = new JPasswordField(20);

    // Novos campos adicionados
    private final JTextField caminhoCertificadoField = new JTextField(20);
    private final JTextField caminhoChavePrivadaField = new JTextField(20);
    private final JPasswordField fraseSecretaField = new JPasswordField(20);
    private final JComboBox<String> grupoComboBox = new JComboBox<>(new String[]{"Usuário", "Administrador"});

    private final JButton cadastrarButton = new JButton("Cadastrar");
    private final JButton voltarLoginButton = new JButton("Voltar para Login");
    private final JLabel statusLabel = new JLabel(" ");
    private final UsuarioServico usuarioServico;
    private final RegistroServico registroServico;

    public CadastroUsuarioPanel(UsuarioServico usuarioServico, RegistroServico registroServico) {
        this.usuarioServico = usuarioServico;
        this.registroServico = registroServico;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        int linha = 0;

        // Linha Nome
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(nomeField, gbc);
        linha++;

        // Linha Email
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(emailField, gbc);
        linha++;

        // Linha Caminho Certificado
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Caminho Certificado:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(caminhoCertificadoField, gbc);
        linha++;

        // Linha Caminho Chave Privada
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Caminho Chave Privada:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(caminhoChavePrivadaField, gbc);
        linha++;

        // Linha Frase Secreta
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Frase Secreta Chave:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(fraseSecretaField, gbc);
        linha++;
        
        // Linha Grupo
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Grupo:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(grupoComboBox, gbc);
        linha++;

        // Linha Senha
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Senha Pessoal:"), gbc); // Label atualizado
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(senhaField, gbc);
        linha++;

        // Linha Confirmar Senha
        gbc.gridx = 0; gbc.gridy = linha; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Confirmar Senha Pessoal:"), gbc); // Label atualizado
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(confirmarSenhaField, gbc);
        linha++;

        // Botões e Status
        gbc.gridx = 0; gbc.gridy = linha; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(cadastrarButton, gbc);
        linha++;
        gbc.gridy = linha;
        add(voltarLoginButton, gbc);
        linha++;
        gbc.gridy = linha;
        add(statusLabel, gbc);

        cadastrarButton.addActionListener((ActionEvent e) -> {
            String nome = nomeField.getText().trim();
            String email = emailField.getText().trim();
            String senha = new String(senhaField.getPassword());
            String confirmarSenha = new String(confirmarSenhaField.getPassword());
            String caminhoCertificado = getCaminhoCertificado();
            String caminhoChavePrivada = getCaminhoChavePrivada();
            String fraseSecreta = getFraseSecreta();

            if (nome.isEmpty() || email.isEmpty() || 
                (this.isVisible() && !isModoAdminInicial() && (caminhoCertificado.isEmpty() || caminhoChavePrivada.isEmpty() || fraseSecreta.isEmpty())) ||
                senha.isEmpty() || confirmarSenha.isEmpty()) {
                statusLabel.setText("Preencha todos os campos obrigatórios.");
                return;
            }
            if (!senha.equals(confirmarSenha)) {
                statusLabel.setText("As senhas não coincidem.");
                return;
            }
            
            // Validações de formato de senha (8-10 números) devem ser adicionadas aqui
            // e também a verificação de não aceitar sequências de números repetidos.
            // Essas validações são complexas e devem ser tratadas com atenção.

            try {
                // --- NOVO FLUXO DE CADASTRO ---
                // Buscar o GID do grupo selecionado
                String grupoSelecionado = getGrupoSelecionado();
                int gid = 2; // Default para "Usuário"
                if ("Administrador".equalsIgnoreCase(grupoSelecionado)) gid = 1;
                // O adminUid pode ser null ou obtido do contexto, aqui passamos null para simplificar
                UsuarioServico.CadastroUsuarioResult result = usuarioServico.cadastrarNovoUsuario(
                    nome, email, senha, gid, caminhoCertificado, caminhoChavePrivada, fraseSecreta, null
                );
                // Gerar URI otpauth://
                String otpAuthUri = usuarioServico.getTotpServico().gerarUrlQRCode(result.chaveTotpBase32, result.usuario.getEmail());
                // Gerar QR code como Data URI
                // O método gerarImagemQrCodeComoDataUri é privado, então gere o QR code aqui se necessário
                String qrCodeDataUri = null;
                try {
                    Class<?> totpServicoClass = usuarioServico.getTotpServico().getClass();
                    java.lang.reflect.Method m = totpServicoClass.getDeclaredMethod("gerarImagemQrCodeComoDataUri", String.class);
                    m.setAccessible(true);
                    qrCodeDataUri = (String) m.invoke(usuarioServico.getTotpServico(), otpAuthUri);
                } catch (Exception ex) {
                    qrCodeDataUri = null;
                }
                // Exibir modal com chave e QR code
                mostrarModalTotp(result.chaveTotpBase32, otpAuthUri, qrCodeDataUri);
                statusLabel.setText("Usuário cadastrado com sucesso! Configure o TOTP.");
                onCadastroSuccess();
            } catch (Exception ex) {
                statusLabel.setText("Erro durante a tentativa de cadastro: " + ex.getMessage());
                ex.printStackTrace(); // Para debugging
            }
        });

        voltarLoginButton.addActionListener((ActionEvent e) -> {
            onGoToLogin();
        });
    }

    public String getEmail() {
        return emailField.getText().trim();
    }

    public String getSenha() {
        return new String(senhaField.getPassword());
    }

    // Getters para os novos campos
    public String getCaminhoCertificado() {
        return caminhoCertificadoField.getText().trim();
    }

    public String getCaminhoChavePrivada() {
        return caminhoChavePrivadaField.getText().trim();
    }

    public String getFraseSecreta() {
        // Para JPasswordField, é mais seguro retornar char[] e limpar depois
        // mas String é mais simples para a integração inicial.
        // Considerar mudar para char[] se a segurança for crítica neste ponto.
        return new String(fraseSecretaField.getPassword());
    }

    public String getGrupoSelecionado() {
        return (String) grupoComboBox.getSelectedItem();
    }
    
    public String getNome() { // Getter para o nome, se necessário
        return nomeField.getText().trim();
    }

    /**
     * Configura o painel para o modo de cadastro inicial do administrador.
     * Pré-seleciona e desabilita o campo de grupo para "Administrador".
     * @param isAdminInicial true se for o cadastro inicial do admin, false caso contrário.
     */
    public void setModoAdminInicial(boolean isAdminInicial) {
        if (isAdminInicial) {
            grupoComboBox.setSelectedItem("Administrador");
            grupoComboBox.setEnabled(false);
            // Poderia também desabilitar/pré-preencher outros campos se necessário
            // Ex: nomeField.setEnabled(false); // Se o nome vier sempre do certificado
        } else {
            grupoComboBox.setEnabled(true);
            // Garantir que a seleção padrão seja "Usuário" ou permitir seleção livre
            // grupoComboBox.setSelectedItem("Usuário"); // Opcional
        }
    }

    private boolean isModoAdminInicial() {
        // Helper para verificar se o painel está configurado para cadastro de admin inicial
        // Isso normalmente seria determinado por uma propriedade ou estado gerenciado pelo MainFrame
        return !grupoComboBox.isEnabled() && "Administrador".equals(grupoComboBox.getSelectedItem());
    }

    // Callbacks para serem sobrescritos pelo MainFrame
    protected void onCadastroSuccess() {}
    protected void onGoToLogin() {}

    private void mostrarModalTotp(String chaveTotp, String otpAuthUri, String qrCodeDataUri) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Chave TOTP (para Google Authenticator):"));
        JTextField chaveField = new JTextField(chaveTotp);
        chaveField.setEditable(false);
        panel.add(chaveField);
        panel.add(new JLabel("URI otpauth://:"));
        JTextField uriField = new JTextField(otpAuthUri);
        uriField.setEditable(false);
        panel.add(uriField);
        if (qrCodeDataUri != null) {
            try {
                String base64 = qrCodeDataUri.substring(qrCodeDataUri.indexOf(",") + 1);
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
                ImageIcon icon = new ImageIcon(imageBytes);
                JLabel qrLabel = new JLabel(icon);
                panel.add(new JLabel("QR Code para escanear:"));
                panel.add(qrLabel);
            } catch (Exception e) {
                panel.add(new JLabel("(Falha ao exibir QR code)"));
            }
        }
        JOptionPane.showMessageDialog(this, panel, "Configuração TOTP", JOptionPane.INFORMATION_MESSAGE);
    }
} 