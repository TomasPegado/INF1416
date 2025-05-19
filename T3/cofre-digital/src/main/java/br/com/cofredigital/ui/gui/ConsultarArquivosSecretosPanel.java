package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import java.security.PrivateKey;
import java.security.PublicKey;
import br.com.cofredigital.util.ArquivoProtegidoUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ConsultarArquivosSecretosPanel extends JPanel {
    // Cabeçalho
    private JLabel lblLoginValue;
    private JLabel lblGrupoValue;
    private JLabel lblNomeValue;

    // Corpo 1
    private JLabel lblTotalConsultasValue;

    // Corpo 2
    private JTextField txtCaminhoPasta;
    private JPasswordField txtFraseSecreta;
    private JButton btnListar;
    private JTable tabelaArquivos;
    private DefaultTableModel tabelaModel;
    private JButton btnVoltar;

    private Usuario usuarioLogado;
    private UsuarioServico usuarioServico;
    private PrivateKey chavePrivadaAdmin;
    private PublicKey chavePublicaAdmin;

    public ConsultarArquivosSecretosPanel() {
        setLayout(new BorderLayout(10, 10));

        // --- Painel do Cabeçalho ---
        JPanel headerPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Informações do Usuário"));
        lblLoginValue = new JLabel("N/A");
        lblGrupoValue = new JLabel("N/A");
        lblNomeValue = new JLabel("N/A");
        headerPanel.add(new JLabel("Login:"));
        headerPanel.add(lblLoginValue);
        headerPanel.add(new JLabel("Grupo:"));
        headerPanel.add(lblGrupoValue);
        headerPanel.add(new JLabel("Nome:"));
        headerPanel.add(lblNomeValue);
        add(headerPanel, BorderLayout.NORTH);

        // --- Painel Central (Corpo 1 e Corpo 2) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // --- Painel do Corpo 1 ---
        JPanel body1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        body1Panel.setBorder(BorderFactory.createTitledBorder("Estatísticas"));
        lblTotalConsultasValue = new JLabel("N/A");
        body1Panel.add(new JLabel("Total de consultas do usuário:"));
        body1Panel.add(lblTotalConsultasValue);
        centerPanel.add(body1Panel, BorderLayout.NORTH);

        // --- Painel do Corpo 2 ---
        JPanel body2Panel = new JPanel();
        body2Panel.setLayout(new BoxLayout(body2Panel, BoxLayout.Y_AXIS));
        body2Panel.setBorder(BorderFactory.createTitledBorder("Consultar Pasta de Arquivos Secretos"));

        JPanel linhaCaminho = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linhaCaminho.add(new JLabel("Caminho da pasta:"));
        txtCaminhoPasta = new JTextField(30);
        linhaCaminho.add(txtCaminhoPasta);
        JButton btnSelecionarPasta = new JButton("Selecionar...");
        linhaCaminho.add(btnSelecionarPasta);
        body2Panel.add(linhaCaminho);
        JLabel lblDica = new JLabel("Dica: Dê duplo clique para entrar nas subpastas e clique em 'Selecionar' para escolher.");
        lblDica.setFont(lblDica.getFont().deriveFont(Font.ITALIC, 11f));
        body2Panel.add(lblDica);
        // Listener do botão Selecionar...
        btnSelecionarPasta.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Selecionar Pasta Segura do Cofre Digital");
            chooser.setApproveButtonText("Selecionar");
            chooser.setApproveButtonToolTipText("Selecionar a pasta atual");
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                txtCaminhoPasta.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel linhaFrase = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linhaFrase.add(new JLabel("Frase secreta:"));
        txtFraseSecreta = new JPasswordField(30);
        linhaFrase.add(txtFraseSecreta);
        body2Panel.add(linhaFrase);

        btnListar = new JButton("Listar");
        JPanel linhaBotaoListar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linhaBotaoListar.add(btnListar);
        body2Panel.add(linhaBotaoListar);

        // Listener do botão Listar
        btnListar.addActionListener(e -> {
            String caminhoPasta = getCaminhoPasta();
            String fraseSecreta = getFraseSecreta();
            if (usuarioLogado == null) {
                JOptionPane.showMessageDialog(this, "Usuário não definido.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (caminhoPasta.isEmpty() || fraseSecreta.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha o caminho da pasta e a frase secreta.", "Campos obrigatórios", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (usuarioServico == null) {
                JOptionPane.showMessageDialog(this, "Serviço de usuário não configurado.", "Erro interno", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                // 1. Decriptar a chave privada do usuário logado usando a frase secreta fornecida
                int kid = usuarioLogado.getKid();
                br.com.cofredigital.persistencia.modelo.Chaveiro chaveiroUsuario = usuarioServico.buscarChaveiroPorKid(kid).orElseThrow(() -> new Exception("Chaveiro não encontrado para o usuário."));
                java.security.PrivateKey chavePrivadaUsuario;
                try {
                    chavePrivadaUsuario = br.com.cofredigital.crypto.PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromDERBytes(
                        chaveiroUsuario.getChavePrivadaCriptografada(), fraseSecreta
                    );
                } catch (Exception exPriv) {
                    // Erro ao decriptar a chave privada: provavelmente frase secreta errada
                    JOptionPane.showMessageDialog(this, "Frase secreta incorreta para decriptar a chave privada do usuário.", "Erro de autenticação", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                java.security.cert.X509Certificate certificadoUsuario = br.com.cofredigital.crypto.CertificateUtil.loadCertificateFromPEMString(chaveiroUsuario.getCertificadoPem());

                // 2. Ler arquivos do índice
                String basePath = caminhoPasta;
                byte[] envBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.env");
                byte[] encBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.enc");
                byte[] asdBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.asd");

                // 3. Decriptar envelope digital com a chave privada do usuário logado
                byte[] semente;
                try {
                    semente = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarEnvelope(envBytes, chavePrivadaUsuario);
                } catch (java.security.GeneralSecurityException exEnv) {
                    // Erro ao decriptar o envelope: provavelmente não é o dono
                    JOptionPane.showMessageDialog(this, "Você não tem permissão para acessar este arquivo secreto, pois não é o dono dele.", "Permissão negada", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (Exception exEnv) {
                    // Outros erros ao decriptar o envelope
                    JOptionPane.showMessageDialog(this, "Erro ao decriptar envelope digital: " + exEnv.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 4. Gerar chave AES
                javax.crypto.SecretKey chaveAES = br.com.cofredigital.util.ArquivoProtegidoUtil.gerarChaveAES(semente);
                // 5. Decriptar índice
                byte[] indiceDecriptado = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarArquivoAES(encBytes, chaveAES);

                // 6. Verificar assinatura digital do índice usando o certificado do ADMINISTRADOR
                // Buscar o admin do sistema
                br.com.cofredigital.autenticacao.modelo.Usuario admin = usuarioServico.listarTodos().stream()
                    .filter(u -> u.getGrupo() != null && u.getGrupo().equalsIgnoreCase("Administrador"))
                    .findFirst().orElse(null);
                if (admin == null) {
                    JOptionPane.showMessageDialog(this, "Administrador do sistema não encontrado para verificar assinatura.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int adminKid = admin.getKid();
                br.com.cofredigital.persistencia.modelo.Chaveiro chaveiroAdmin = usuarioServico.buscarChaveiroPorKid(adminKid).orElseThrow(() -> new Exception("Chaveiro do admin não encontrado."));
                java.security.cert.X509Certificate certificadoAdmin = br.com.cofredigital.crypto.CertificateUtil.loadCertificateFromPEMString(chaveiroAdmin.getCertificadoPem());
                boolean assinaturaOk = br.com.cofredigital.util.ArquivoProtegidoUtil.verificarAssinatura(indiceDecriptado, asdBytes, certificadoAdmin.getPublicKey());
                if (!assinaturaOk) {
                    JOptionPane.showMessageDialog(this, "Assinatura digital do índice inválida! Arquivo pode ter sido adulterado.", "Erro de integridade", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 7. Ler linhas e preencher tabela
                limparTabela();
                String conteudo = new String(indiceDecriptado, java.nio.charset.StandardCharsets.UTF_8);
                String[] linhas = conteudo.split("\\n");
                String loginUsuario = usuarioLogado.getEmail();
                String grupoUsuario = usuarioLogado.getGrupo();
                for (String linha : linhas) {
                    String[] partes = linha.trim().split(" ");
                    if (partes.length < 4) continue;
                    String nomeCodigo = partes[0];
                    String nomeArquivo = partes[1];
                    String dono = partes[2];
                    String grupo = partes[3];
                    // Exibir apenas arquivos do usuário ou do grupo
                    if (dono.equalsIgnoreCase(loginUsuario) || grupo.equalsIgnoreCase(grupoUsuario)) {
                        tabelaModel.addRow(new Object[]{nomeCodigo, nomeArquivo, dono, grupo});
                    }
                }
                JOptionPane.showMessageDialog(this, "Consulta realizada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                // Erro genérico não tratado acima
                JOptionPane.showMessageDialog(this, "Erro ao consultar arquivos secretos: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Tabela de arquivos secretos
        String[] colunas = {"Nome Código", "Nome", "Dono", "Grupo"};
        tabelaModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaArquivos = new JTable(tabelaModel);
        JScrollPane scrollTabela = new JScrollPane(tabelaArquivos);
        body2Panel.add(scrollTabela);

        btnVoltar = new JButton("Voltar");
        JPanel linhaBotaoVoltar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        linhaBotaoVoltar.add(btnVoltar);
        body2Panel.add(linhaBotaoVoltar);

        centerPanel.add(body2Panel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Listener do botão Voltar
        btnVoltar.addActionListener(e -> {
            if (usuarioLogado != null && usuarioLogado.getGrupo() != null && usuarioLogado.getGrupo().equalsIgnoreCase("Administrador")) {
                // Voltar para o menu do admin
                java.awt.Component c = this.getParent();
                while (c != null && !(c instanceof javax.swing.JFrame)) {
                    c = c.getParent();
                }
                if (c instanceof MainFrame) {
                    ((MainFrame) c).showScreen(MainFrame.ADMIN_MAIN_PANEL);
                }
            } else if (usuarioLogado != null) {
                // Voltar para o menu do usuário comum
                java.awt.Component c = this.getParent();
                while (c != null && !(c instanceof javax.swing.JFrame)) {
                    c = c.getParent();
                }
                if (c instanceof MainFrame) {
                    ((MainFrame) c).showScreen(MainFrame.USER_MAIN_PANEL);
                }
            }
        });
    }

    // Métodos para atualizar informações do usuário e consultas
    public void setUsuarioLogado(Usuario usuario, int totalConsultas) {
        this.usuarioLogado = usuario;
        lblLoginValue.setText(usuario.getEmail());
        lblGrupoValue.setText(usuario.getGrupo());
        lblNomeValue.setText(usuario.getNome());
        lblTotalConsultasValue.setText(String.valueOf(totalConsultas));
    }

    public String getCaminhoPasta() {
        return txtCaminhoPasta.getText().trim();
    }

    public String getFraseSecreta() {
        return new String(txtFraseSecreta.getPassword());
    }

    public JButton getBtnListar() {
        return btnListar;
    }

    public JButton getBtnVoltar() {
        return btnVoltar;
    }

    public JTable getTabelaArquivos() {
        return tabelaArquivos;
    }

    public DefaultTableModel getTabelaModel() {
        return tabelaModel;
    }

    public void limparTabela() {
        tabelaModel.setRowCount(0);
    }

    public void limparCamposDeEntrada() {
        txtCaminhoPasta.setText("");
        txtFraseSecreta.setText("");
    }

    public void setUsuarioServico(UsuarioServico usuarioServico) {
        this.usuarioServico = usuarioServico;
    }

    public void setAdminKeys(PrivateKey chavePrivada, PublicKey chavePublica) {
        this.chavePrivadaAdmin = chavePrivada;
        this.chavePublicaAdmin = chavePublica;
    }
} 