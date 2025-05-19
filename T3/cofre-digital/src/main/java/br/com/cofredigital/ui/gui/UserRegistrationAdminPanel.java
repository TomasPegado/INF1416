package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.persistencia.dao.GrupoDAO;
import br.com.cofredigital.persistencia.dao.GrupoDAOImpl;
import br.com.cofredigital.persistencia.modelo.Grupo;
import br.com.cofredigital.log.LogEventosMIDs; // Para logging de GUI

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public class UserRegistrationAdminPanel extends JPanel {

    private MainFrame mainFrame;
    private UsuarioServico usuarioServico;
    private GrupoDAO grupoDAO; // Para buscar GID
    private Usuario adminOperador;

    // Cabeçalho
    private JLabel lblAdminLoginValue;
    private JLabel lblAdminGrupoValue;
    private JLabel lblAdminNomeValue;

    // Corpo 1
    private JLabel lblTotalUsuariosValue;

    // Corpo 2 - Formulário
    private JTextField txtCertPath;
    private JButton btnBrowseCert;
    private JTextField txtKeyPath;
    private JButton btnBrowseKey;
    private JPasswordField pwdPassphrase;
    private JComboBox<String> comboGrupo;
    private JPasswordField pwdSenha;
    private JPasswordField pwdConfirmaSenha;

    private JButton btnCadastrar;
    private JButton btnVoltar;

    public UserRegistrationAdminPanel(MainFrame mainFrame, UsuarioServico usuarioServico) {
        this.mainFrame = mainFrame;
        this.usuarioServico = usuarioServico;
        this.grupoDAO = new GrupoDAOImpl(); // Instanciar DAO
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // --- Painel do Cabeçalho (Admin Info) ---
        JPanel headerPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Administrador Logado"));
        lblAdminLoginValue = new JLabel("N/A");
        lblAdminGrupoValue = new JLabel("N/A");
        lblAdminNomeValue = new JLabel("N/A");
        headerPanel.add(new JLabel("Login:"));
        headerPanel.add(lblAdminLoginValue);
        headerPanel.add(new JLabel("Grupo:"));
        headerPanel.add(lblAdminGrupoValue);
        headerPanel.add(new JLabel("Nome:"));
        headerPanel.add(lblAdminNomeValue);
        add(headerPanel, BorderLayout.NORTH);

        // --- Painel Central (Corpo 1 e Formulário) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // --- Painel do Corpo 1 (Total de Usuários) ---
        JPanel body1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        body1Panel.setBorder(BorderFactory.createTitledBorder("Estatísticas do Sistema"));
        lblTotalUsuariosValue = new JLabel("0");
        body1Panel.add(new JLabel("Total de usuários no sistema:"));
        body1Panel.add(lblTotalUsuariosValue);
        centerPanel.add(body1Panel, BorderLayout.NORTH);

        // --- Painel do Formulário (Corpo 2) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Formulário de Cadastro de Novo Usuário"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int currentGridY = 0;

        // Caminho Certificado
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Caminho do Certificado Digital:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.WEST;
        txtCertPath = new JTextField(15);
        formPanel.add(txtCertPath, gbc);
        gbc.gridx = 2; gbc.gridy = currentGridY; gbc.fill = GridBagConstraints.NONE;
        btnBrowseCert = new JButton("Procurar...");
        formPanel.add(btnBrowseCert, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        currentGridY++;

        // Caminho Chave Privada
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Caminho da Chave Privada:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.WEST;
        txtKeyPath = new JTextField(15);
        formPanel.add(txtKeyPath, gbc);
        gbc.gridx = 2; gbc.gridy = currentGridY; gbc.fill = GridBagConstraints.NONE;
        btnBrowseKey = new JButton("Procurar...");
        formPanel.add(btnBrowseKey, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        currentGridY++;

        // Frase Secreta
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Frase Secreta (chave privada):"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdPassphrase = new JPasswordField(20);
        formPanel.add(pwdPassphrase, gbc);
        gbc.gridwidth = 1;
        currentGridY++;

        // Grupo
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Grupo:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        comboGrupo = new JComboBox<>(new String[]{"Usuário", "Administrador"});
        formPanel.add(comboGrupo, gbc);
        gbc.gridwidth = 1;
        currentGridY++;

        // Senha Pessoal
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Senha Pessoal (8-10 dígitos numéricos):"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdSenha = new JPasswordField(10);
        formPanel.add(pwdSenha, gbc);
        gbc.gridwidth = 1;
        currentGridY++;

        // Confirmação Senha Pessoal
        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Confirmação Senha Pessoal:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentGridY; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdConfirmaSenha = new JPasswordField(10);
        formPanel.add(pwdConfirmaSenha, gbc);
        gbc.gridwidth = 1;
        currentGridY++;

        // Botões
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnCadastrar = new JButton("Cadastrar");
        btnVoltar = new JButton("Voltar para o Menu Principal");
        buttonsPanel.add(btnCadastrar);
        buttonsPanel.add(btnVoltar);

        gbc.gridx = 0; gbc.gridy = currentGridY; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonsPanel, gbc);

        centerPanel.add(formPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Action Listeners
        btnBrowseCert.addActionListener(e -> browseFile(txtCertPath));
        btnBrowseKey.addActionListener(e -> browseFile(txtKeyPath));

        btnCadastrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performCadastro();
            }
        });

        btnVoltar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Log de GUI: Admin voltando do cadastro para o menu principal
                if (adminOperador != null && mainFrame.getRegistroServico() != null) {
                     mainFrame.getRegistroServico().registrarEventoDoUsuario(
                        LogEventosMIDs.CAD_BOTAO_VOLTAR_MENU_PRINCIPAL, 
                        adminOperador.getId(),
                        "admin_email", adminOperador.getEmail()
                    );
                }
                mainFrame.showScreen(MainFrame.ADMIN_MAIN_PANEL);
            }
        });
    }

    private void browseFile(JTextField targetField) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            targetField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void prepareForm(Usuario admin) {
        this.adminOperador = admin;
        if (admin != null) {
            lblAdminLoginValue.setText(admin.getEmail());
            lblAdminGrupoValue.setText(admin.getGrupo());
            lblAdminNomeValue.setText(admin.getNome());
        } else {
            lblAdminLoginValue.setText("N/A");
            lblAdminGrupoValue.setText("N/A");
            lblAdminNomeValue.setText("N/A");
        }

        try {
            lblTotalUsuariosValue.setText(String.valueOf(usuarioServico.listarTodos().size()));
        } catch (Exception e) {
            lblTotalUsuariosValue.setText("Erro");
            JOptionPane.showMessageDialog(this, "Erro ao buscar total de usuários: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
        // Limpar campos do formulário
        txtCertPath.setText("");
        txtKeyPath.setText("");
        pwdPassphrase.setText("");
        comboGrupo.setSelectedIndex(0); // Default para "Usuário"
        pwdSenha.setText("");
        pwdConfirmaSenha.setText("");
        
        // Log de GUI: Tela de cadastro (por admin) apresentada
        if (adminOperador != null && mainFrame.getRegistroServico() != null) {
             mainFrame.getRegistroServico().registrarEventoDoUsuario(
                LogEventosMIDs.TELA_CADASTRO_APRESENTADA, 
                adminOperador.getId(),
                "admin_email", adminOperador.getEmail()
            );
        }
    }

    private void performCadastro() {
        if (adminOperador == null) {
            JOptionPane.showMessageDialog(this, "Erro: Administrador operador não definido.", "Erro Interno", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String certPath = txtCertPath.getText().trim();
        String keyPath = txtKeyPath.getText().trim();
        String passphrase = new String(pwdPassphrase.getPassword());
        String grupoSelecionado = (String) comboGrupo.getSelectedItem();
        String senha = new String(pwdSenha.getPassword());
        String confirmaSenha = new String(pwdConfirmaSenha.getPassword());

        // Log de GUI: Admin pressionou botão cadastrar
         if (mainFrame.getRegistroServico() != null) {
             mainFrame.getRegistroServico().registrarEventoDoUsuario(
                LogEventosMIDs.BOTAO_CADASTRAR_PRESSIONADO, 
                adminOperador.getId(),
                "admin_email", adminOperador.getEmail()
            );
        }

        // Validações
        if (certPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Caminho do Certificado Digital é obrigatório.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, adminOperador.getId(), "motivo", "caminho_certificado_vazio_cadastro_admin");
            return;
        }
        if (keyPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Caminho da Chave Privada é obrigatório.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, adminOperador.getId(), "motivo", "caminho_chave_vazio_cadastro_admin");
            return;
        }
        if (senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Senha pessoal é obrigatória.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, adminOperador.getId(), "motivo", "senha_vazia_cadastro_admin");
            return;
        }
        if (!senha.matches("^\\d{8,10}$")) {
            JOptionPane.showMessageDialog(this, "Senha pessoal deve ter 8, 9 ou 10 dígitos numéricos.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_SENHA_INVALIDA, adminOperador.getId(), "motivo", "senha_formato_invalido_cadastro_admin");
            return;
        }
        if (java.util.stream.IntStream.range(0, senha.length() -1).allMatch(i -> senha.charAt(i) == senha.charAt(i+1)) && senha.length() > 1) {
            JOptionPane.showMessageDialog(this, "Senha pessoal não pode ter todos os dígitos repetidos.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_SENHA_INVALIDA, adminOperador.getId(), "motivo", "senha_digitos_repetidos_cadastro_admin");
            return;
        }
        if (!senha.equals(confirmaSenha)) {
            JOptionPane.showMessageDialog(this, "As senhas não coincidem.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
             mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_SENHA_INVALIDA, adminOperador.getId(), "motivo", "senhas_nao_coincidem_cadastro_admin");
            return;
        }
        if (grupoSelecionado == null) {
             JOptionPane.showMessageDialog(this, "Selecione um grupo para o novo usuário.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
             mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_DADOS_INVALIDOS_GUI, adminOperador.getId(), "motivo", "grupo_nao_selecionado_cadastro_admin");
            return;
        }

        // Carregar certificado e extrair dados para confirmação
        java.security.cert.X509Certificate certificate;
        java.util.Map<String, String> certificateData;
        try {
            certificate = br.com.cofredigital.crypto.CertificateUtil.loadCertificateFromFile(certPath);
            if (certificate == null) { // Deveria ter lançado exceção antes se nulo
                JOptionPane.showMessageDialog(this, "Falha ao carregar certificado (retornou nulo).", "Erro de Certificado", JOptionPane.ERROR_MESSAGE);
                mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, adminOperador.getId(), "caminho", certPath, "motivo", "loadCertificateFromFile_retornou_nulo");
                return;
            }
            certificateData = br.com.cofredigital.crypto.CertificateUtil.extractCertificateDetails(certificate);
            if (certificateData == null || certificateData.get("E-mail") == null || certificateData.get("E-mail").isEmpty()) {
                JOptionPane.showMessageDialog(this, "Não foi possível extrair o E-mail do certificado para confirmação.", "Erro de Certificado", JOptionPane.ERROR_MESSAGE);
                mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_USUARIO_DADOS_INVALIDOS, adminOperador.getId(), "motivo", "email_nao_extraido_do_certificado_para_confirmacao");
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar ou processar o certificado: " + ex.getMessage(), "Erro de Certificado", JOptionPane.ERROR_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_CERTIFICADO_PATH_INVALIDO, adminOperador.getId(), "caminho", certPath, "erro", ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // Exibir diálogo de confirmação
        CertificateConfirmationDialog confirmationDialog = new CertificateConfirmationDialog(mainFrame, "Confirmar Dados do Certificado para Cadastro", certificateData);
        confirmationDialog.setVisible(true);

        if (!confirmationDialog.isConfirmado()) {
            JOptionPane.showMessageDialog(this, "Cadastro cancelado pelo usuário após visualização dos dados do certificado.", "Cadastro Cancelado", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_CONFIRMACAO_DADOS_REJEITADA, adminOperador.getId(), "email_certificado", certificateData.get("E-mail"));
            // Roteiro: "retornar à Tela de Cadastro com o formulário preenchido com os dados fornecidos."
            // Os dados já estão nos campos, então não fazemos nada para limpá-los aqui.
            return;
        }
        
        // Se confirmado, prosseguir com o cadastro
        mainFrame.getRegistroServico().registrarEventoDoUsuario(LogEventosMIDs.CAD_CONFIRMACAO_DADOS_ACEITA, adminOperador.getId(), "email_certificado", certificateData.get("E-mail"));

        // Obter GID do grupo (repetido, mas necessário após confirmação)
        int gid;
        try {
            Optional<Grupo> grupoOpt = grupoDAO.buscarPorNome(grupoSelecionado);
            if (grupoOpt.isPresent()) {
                gid = grupoOpt.get().getGid();
            } else {
                JOptionPane.showMessageDialog(this, "Grupo selecionado ('" + grupoSelecionado + "') não encontrado (pós-confirmação).", "Erro Interno", JOptionPane.ERROR_MESSAGE);
                return; // Não deveria acontecer se validado antes
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar informações do grupo (pós-confirmação): " + ex.getMessage(), "Erro de Banco de Dados", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Nome e Email são passados como null, pois o UsuarioServico.cadastrarNovoUsuario
            // foi modificado para extraí-los do certificado (caminhoCertificado é passado).
            Usuario novoUsuario = usuarioServico.cadastrarNovoUsuario(
                null, // nomeInput - será extraído do cert pelo serviço
                null, // emailInput - será extraído do cert pelo serviço
                senha,
                gid,
                certPath, 
                keyPath,  
                passphrase, 
                adminOperador.getId()
            );

            JOptionPane.showMessageDialog(this, "Usuário '" + novoUsuario.getEmail() + "' cadastrado com sucesso!", "Cadastro Concluído", JOptionPane.INFORMATION_MESSAGE);
            // Roteiro: "Se o cadastro for efetivado, deve-se retornar à Tela de Cadastro com o formulário vazio."
            prepareForm(this.adminOperador); 

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar usuário (após confirmação): " + ex.getMessage(), "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
            // Se falhar aqui, o roteiro diz "retornar à Tela de Cadastro com o formulário preenchido"
            // Os dados já estão nos campos, então não limpamos.
            ex.printStackTrace();
        }
    }
} 