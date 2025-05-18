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
            this.registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_BOTAO_CADASTRAR_PRESSIONADO_GUI);

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
                this.registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, "motivo", "campos_obrigatorios_vazios");
                return;
            }
            if (!senha.equals(confirmarSenha)) {
                statusLabel.setText("As senhas não coincidem.");
                this.registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, "email_tentativa", email, "motivo", "senhas_nao_coincidem");
                return;
            }
            
            // Validações de formato de senha (8-10 números) devem ser adicionadas aqui
            // e também a verificação de não aceitar sequências de números repetidos.
            // Essas validações são complexas e devem ser tratadas com atenção.

            try {
                Usuario usuario = new Usuario();
                // O nome e email devem ser extraídos do certificado digital, conforme o roteiro.
                // Por enquanto, usamos o campo nome, mas isso será ajustado.
                usuario.setNome(nome); 
                usuario.setEmail(email); // O email também virá do certificado.
                usuario.setGrupo(getGrupoSelecionado());

                // A lógica de cadastro real é complexa e será tratada no UsuarioServico:
                // 1. Ler certificado de caminhoCertificado.
                // 2. Extrair nome e email do certificado e atualizar 'usuario'.
                // 3. Ler chave privada de caminhoChavePrivada (que está criptografada).
                // 4. Usar fraseSecreta para descriptografar a chave privada.
                // 5. Validar chave privada com chave pública do certificado.
                // 6. Se tudo ok, prosseguir com o hash da 'senha' (bcrypt), geração TOTP, etc.
                // 7. Chamar um método em UsuarioServico como:
                //    usuarioServico.registrarNovoUsuario(usuario, senha, caminhoCertificado, caminhoChavePrivada, fraseSecreta);
                //    ou um método específico para o admin inicial.

                // Placeholder para a chamada de serviço - SUBSTITUIR PELA LÓGICA REAL
                System.out.println("--- Dados Coletados para Cadastro ---");
                System.out.println("Nome: " + nome);
                System.out.println("Email: " + email);
                System.out.println("Caminho Cert.: " + caminhoCertificado);
                System.out.println("Caminho Chave: " + caminhoChavePrivada);
                System.out.println("Frase Secreta: " + fraseSecreta.replaceAll(".", "*")); // Não logar frase real
                System.out.println("Grupo: " + getGrupoSelecionado());
                System.out.println("Senha Pessoal: " + senha.replaceAll(".", "*"));
                System.out.println("-------------------------------------");
                
                // Simulação de sucesso para manter o fluxo de UI
                // Em uma implementação real, isso dependeria do resultado do UsuarioServico.
                // usuarioServico.cadastrarUsuario(usuario, senha); // Linha antiga comentada

                // Supondo que o UsuarioServico agora precise de mais dados ou tenha um método específico:
                // Exemplo de chamada (precisa ser implementado no UsuarioServico):
                // usuarioServico.processarCadastroCompleto(nome, email, senha, caminhoCertificado, caminhoChavePrivada, fraseSecreta, grupo);

                statusLabel.setText("Dados de cadastro coletados. Processamento pendente."); // Mensagem temporária
                // onCadastroSuccess(); // Manter por enquanto para o fluxo de UI, mas o sucesso real é do serviço
                
                // Para o fluxo de primeira execução do admin, o MainFrame tomará conta da chamada ao UsuarioServico
                // com todos os dados necessários que este painel agora fornece através dos getters.
                // Portanto, aqui apenas sinalizamos o sucesso para o MainFrame prosseguir.
                onCadastroSuccess(); 

            } catch (Exception ex) {
                statusLabel.setText("Erro durante a tentativa de cadastro: " + ex.getMessage());
                ex.printStackTrace(); // Para debugging
            }
        });

        voltarLoginButton.addActionListener((ActionEvent e) -> {
            this.registroServico.registrarEventoDoSistema(LogEventosMIDs.CAD_BOTAO_VOLTAR_LOGIN_PRESSIONADO_GUI);
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
} 