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
    private JTextField txtNovoUsuarioNome;
    private JTextField txtNovoUsuarioEmail;
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

        // Nome do Novo Usuário
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Nome do Novo Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        txtNovoUsuarioNome = new JTextField(20);
        formPanel.add(txtNovoUsuarioNome, gbc);
        gbc.gridwidth = 1; // Reset

        // Email do Novo Usuário
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Email do Novo Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        txtNovoUsuarioEmail = new JTextField(20);
        formPanel.add(txtNovoUsuarioEmail, gbc);
        gbc.gridwidth = 1;

        // Caminho Certificado
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Caminho do Certificado Digital:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        txtCertPath = new JTextField(15);
        formPanel.add(txtCertPath, gbc);
        gbc.gridx = 2; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        btnBrowseCert = new JButton("Procurar...");
        formPanel.add(btnBrowseCert, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Caminho Chave Privada
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Caminho da Chave Privada:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        txtKeyPath = new JTextField(15);
        formPanel.add(txtKeyPath, gbc);
        gbc.gridx = 2; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        btnBrowseKey = new JButton("Procurar...");
        formPanel.add(btnBrowseKey, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Frase Secreta
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Frase Secreta (chave privada):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdPassphrase = new JPasswordField(20);
        formPanel.add(pwdPassphrase, gbc);
        gbc.gridwidth = 1;

        // Grupo
        gbc.gridx = 0; gbc.gridy = 5; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Grupo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        comboGrupo = new JComboBox<>(new String[]{"Usuário", "Administrador"});
        formPanel.add(comboGrupo, gbc);
        gbc.gridwidth = 1;

        // Senha Pessoal
        gbc.gridx = 0; gbc.gridy = 6; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Senha Pessoal (10 chars):"), gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdSenha = new JPasswordField(10);
        formPanel.add(pwdSenha, gbc);
        gbc.gridwidth = 1;

        // Confirmação Senha Pessoal
        gbc.gridx = 0; gbc.gridy = 7; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Confirmação Senha Pessoal:"), gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        pwdConfirmaSenha = new JPasswordField(10);
        formPanel.add(pwdConfirmaSenha, gbc);
        gbc.gridwidth = 1;

        // Botões
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnCadastrar = new JButton("Cadastrar");
        btnVoltar = new JButton("Voltar para o Menu Principal");
        buttonsPanel.add(btnCadastrar);
        buttonsPanel.add(btnVoltar);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
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
                        LogEventosMIDs.CAD_BOTAO_VOLTAR_MENU_PRINCIPAL, // Usar um MID apropriado
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
        txtNovoUsuarioNome.setText("");
        txtNovoUsuarioEmail.setText("");
        txtCertPath.setText("");
        txtKeyPath.setText("");
        pwdPassphrase.setText("");
        comboGrupo.setSelectedIndex(0); // Default para "Usuário"
        pwdSenha.setText("");
        pwdConfirmaSenha.setText("");
        
        // Log de GUI: Tela de cadastro (por admin) apresentada
        if (adminOperador != null && mainFrame.getRegistroServico() != null) {
             mainFrame.getRegistroServico().registrarEventoDoUsuario(
                LogEventosMIDs.TELA_CADASTRO_APRESENTADA, // Usar um MID apropriado para "TELA DE CADASTRO (ADMIN) APRESENTADA"
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

        String nomeNovoUsuario = txtNovoUsuarioNome.getText().trim();
        String emailNovoUsuario = txtNovoUsuarioEmail.getText().trim();
        String certPath = txtCertPath.getText().trim();
        String keyPath = txtKeyPath.getText().trim();
        String passphrase = new String(pwdPassphrase.getPassword());
        String grupoSelecionado = (String) comboGrupo.getSelectedItem();
        String senha = new String(pwdSenha.getPassword());
        String confirmaSenha = new String(pwdConfirmaSenha.getPassword());

        // Log de GUI: Admin pressionou botão cadastrar
         if (mainFrame.getRegistroServico() != null) {
             mainFrame.getRegistroServico().registrarEventoDoUsuario(
                LogEventosMIDs.BOTAO_CADASTRAR_PRESSIONADO, // Usar um MID apropriado
                adminOperador.getId(),
                "admin_email", adminOperador.getEmail(),
                "novo_usuario_email_tentativa", emailNovoUsuario
            );
        }

        // Validações
        if (nomeNovoUsuario.isEmpty() || emailNovoUsuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome e Email do novo usuário são obrigatórios.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            // Log de GUI: Dados inválidos
            return;
        }
        if (senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Senha pessoal é obrigatória.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (senha.length() != 10) {
            JOptionPane.showMessageDialog(this, "Senha pessoal deve ter exatamente 10 caracteres.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            // Log de GUI: Senha inválida (comprimento) - Usar CAD_SENHA_INVALIDA ou mais específico
            return;
        }
        if (!senha.equals(confirmaSenha)) {
            JOptionPane.showMessageDialog(this, "As senhas não coincidem.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            // Log de GUI: Senha inválida (não coincide)
            return;
        }
        if (grupoSelecionado == null) {
             JOptionPane.showMessageDialog(this, "Selecione um grupo para o novo usuário.", "Validação Falhou", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Obter GID do grupo
        int gid;
        try {
            Optional<Grupo> grupoOpt = grupoDAO.buscarPorNome(grupoSelecionado);
            if (grupoOpt.isPresent()) {
                gid = grupoOpt.get().getGid();
            } else {
                JOptionPane.showMessageDialog(this, "Grupo selecionado ('" + grupoSelecionado + "') não encontrado.", "Erro Interno", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar informações do grupo: " + ex.getMessage(), "Erro de Banco de Dados", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Tratar caminhos e frase secreta: se um for fornecido, os outros relacionados a chaves também devem ser
        boolean certKeyProvided = !certPath.isEmpty() || !keyPath.isEmpty(); // Não considera passphrase aqui intencionalmente
        String finalCertPath = certPath.isEmpty() ? null : certPath;
        String finalKeyPath = keyPath.isEmpty() ? null : keyPath;
        // Frase secreta só é relevante se a chave for fornecida.
        // O método cadastrarNovoUsuario no UsuarioServico já tem lógica para lidar com isso.
        // Se frase secreta for vazia e chave fornecida, pode ser chave não criptografada (se o serviço suportar).
        String finalPassphrase = passphrase;


        try {
            Usuario novoUsuario = usuarioServico.cadastrarNovoUsuario(
                nomeNovoUsuario,
                emailNovoUsuario,
                senha,
                gid,
                finalCertPath, // Pode ser null
                finalKeyPath,  // Pode ser null
                finalPassphrase, // Pode ser null ou vazia
                adminOperador.getId()
            );

            JOptionPane.showMessageDialog(this, "Usuário '" + novoUsuario.getEmail() + "' cadastrado com sucesso!", "Cadastro Concluído", JOptionPane.INFORMATION_MESSAGE);
            // Atualizar total de usuários e limpar formulário
            prepareForm(this.adminOperador); 

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar usuário: " + ex.getMessage(), "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
            // Log de backend: já feito pelo UsuarioServico
            ex.printStackTrace();
        }
    }
} 