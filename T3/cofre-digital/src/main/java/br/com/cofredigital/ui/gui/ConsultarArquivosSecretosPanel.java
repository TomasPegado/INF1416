// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import java.security.PrivateKey;
import java.security.PublicKey;
import br.com.cofredigital.util.ArquivoProtegidoUtil;
import br.com.cofredigital.persistencia.dao.RegistroDAO;
import br.com.cofredigital.persistencia.dao.RegistroDAOImpl;
import br.com.cofredigital.persistencia.modelo.Registro;
import java.time.LocalDateTime;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import br.com.cofredigital.log.LogEventosMIDs;
import java.util.HashMap;
import java.util.Map;
import br.com.cofredigital.log.servico.RegistroServico;

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
    private RegistroDAO registroDAO = new RegistroDAOImpl();
    private RegistroServico registroServico = new RegistroServico();

    public ConsultarArquivosSecretosPanel() {
        setLayout(new BorderLayout(10, 10));

        // Logar apresentação da tela de consulta de arquivos secretos
        if (usuarioLogado != null) {
            Map<String, String> detalhes = new HashMap<>();
            detalhes.put("login_name", usuarioLogado.getEmail());
            registroServico.registrarEventoDoUsuario(LogEventosMIDs.TELA_CONSULTA_ARQUIVOS_APRESENTADA, usuarioLogado.getId(), detalhes);
        }

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
            String fraseSecretaUsuario = getFraseSecreta();
            Long uid = (usuarioLogado != null && usuarioLogado.getId() != null) ? usuarioLogado.getId() : null;
            if (usuarioLogado == null) {
                JOptionPane.showMessageDialog(this, "Usuário não definido.", "Erro", JOptionPane.ERROR_MESSAGE);
                
                
                return;
            }
            if (caminhoPasta.isEmpty() || fraseSecretaUsuario.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha o caminho da pasta e a frase secreta.", "Campos obrigatórios", JOptionPane.WARNING_MESSAGE);
                // Logar falha geral
                
                return;
            }
            if (usuarioServico == null) {
                JOptionPane.showMessageDialog(this, "Serviço de usuário não configurado.", "Erro interno", JOptionPane.ERROR_MESSAGE);
                // Logar falha geral
                
                return;
            }
            try {
                // 1. Buscar o admin do sistema
                br.com.cofredigital.autenticacao.modelo.Usuario admin = usuarioServico.listarTodos().stream()
                    .filter(u -> u.getGrupo() != null && u.getGrupo().equalsIgnoreCase("Administrador"))
                    .findFirst().orElse(null);
                if (admin == null) {
                    JOptionPane.showMessageDialog(this, "Administrador do sistema não encontrado para verificar assinatura.", "Erro", JOptionPane.ERROR_MESSAGE);
                    
                    return;
                }
                int adminKid = admin.getKid();
                br.com.cofredigital.persistencia.modelo.Chaveiro chaveiroAdmin = usuarioServico.buscarChaveiroPorKid(adminKid).orElseThrow(() -> new Exception("Chaveiro do admin não encontrado."));
                // 2. Obter a frase secreta do admin da sessão
                String fraseSecretaAdmin = usuarioServico.getAdminPassphraseForSession();
                if (fraseSecretaAdmin == null || fraseSecretaAdmin.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "A frase secreta do administrador não está disponível na sessão. Por favor, reinicie o sistema e faça o login do administrador para liberar o acesso.", "Acesso não autorizado", JOptionPane.ERROR_MESSAGE);
                    
                    return;
                }
                // 3. Decriptar a chave privada do admin
                java.security.PrivateKey chavePrivadaAdmin;
                try {
                    chavePrivadaAdmin = br.com.cofredigital.crypto.PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromDERBytes(
                        chaveiroAdmin.getChavePrivadaCriptografada(), fraseSecretaAdmin
                    );
                } catch (Exception exPriv) {
                    JOptionPane.showMessageDialog(this, "Frase secreta incorreta para decriptar a chave privada do administrador.", "Erro de autenticação", JOptionPane.ERROR_MESSAGE);
                    
                    return;
                }
                java.security.cert.X509Certificate certificadoAdmin = br.com.cofredigital.crypto.CertificateUtil.loadCertificateFromPEMString(chaveiroAdmin.getCertificadoPem());

                // 4. Ler arquivos do índice
                String basePath = caminhoPasta;
                byte[] envBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.env");
                byte[] encBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.enc");
                byte[] asdBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(basePath + "/index.asd");

                // 5. Decriptar envelope digital com a chave privada do admin
                byte[] semente;
                try {
                    semente = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarEnvelope(envBytes, chavePrivadaAdmin);
                } catch (java.security.GeneralSecurityException exEnv) {
                    JOptionPane.showMessageDialog(this, "Erro ao decriptar envelope digital do índice com a chave do administrador.", "Permissão negada", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("msg", "Falha na decriptação do envelope digital do índice. Caminho: " + caminhoPasta + ". Erro: " + exEnv.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_INDICE_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                } catch (Exception exEnv) {
                    JOptionPane.showMessageDialog(this, "Erro ao decriptar envelope digital: " + exEnv.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("msg", "Falha na decriptação do envelope digital do índice. Caminho: " + caminhoPasta + ". Erro: " + exEnv.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_INDICE_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                // 6. Gerar chave AES
                javax.crypto.SecretKey chaveAES = br.com.cofredigital.util.ArquivoProtegidoUtil.gerarChaveAES(semente);
                // 7. Decriptar índice
                byte[] indiceDecriptado = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarArquivoAES(encBytes, chaveAES);

                // 8. Verificar assinatura digital do índice usando o certificado do ADMINISTRADOR
                boolean assinaturaOk = br.com.cofredigital.util.ArquivoProtegidoUtil.verificarAssinatura(indiceDecriptado, asdBytes, certificadoAdmin.getPublicKey());
                if (!assinaturaOk) {
                    JOptionPane.showMessageDialog(this, "Assinatura digital do índice inválida! Arquivo pode ter sido adulterado.", "Erro de integridade", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("msg", "Falha na verificação de integridade/autenticidade do índice. Caminho: " + caminhoPasta);
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_INDICE_VERIFICACAO_FALHA, uid, detalhes);
                    return;
                }
                // 9. Ler linhas e preencher tabela
                limparTabela();
                String conteudo = new String(indiceDecriptado, java.nio.charset.StandardCharsets.UTF_8);
                String[] linhas = conteudo.split("\\n");
                String loginUsuario = usuarioLogado.getEmail();
                String grupoUsuario = usuarioLogado.getGrupo();
                int arquivosListados = 0;
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
                        arquivosListados++;
                    }
                }
                JOptionPane.showMessageDialog(this, "Consulta realizada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                // Logar sucesso
                Map<String, String> detalhes = new HashMap<>();
                detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                detalhes.put("msg", "Consulta realizada. Caminho: " + caminhoPasta + ". Arquivos listados: " + arquivosListados);
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_LISTA_ARQUIVOS_INDICE_APRESENTADA, uid, detalhes);
            } catch (Exception ex) {
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

        // Listener de seleção de linha para decriptar arquivo secreto
        tabelaArquivos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int selectedRow = tabelaArquivos.getSelectedRow();
            if (selectedRow == -1) return;
            String nomeCodigo = (String) tabelaModel.getValueAt(selectedRow, 0);
            String nomeArquivo = (String) tabelaModel.getValueAt(selectedRow, 1);
            String dono = (String) tabelaModel.getValueAt(selectedRow, 2);
            String grupo = (String) tabelaModel.getValueAt(selectedRow, 3);
            String loginUsuario = usuarioLogado.getEmail();
            Long uid = (usuarioLogado != null && usuarioLogado.getId() != null) ? usuarioLogado.getId() : null;
            String caminhoPasta = getCaminhoPasta();
            if (!dono.equalsIgnoreCase(loginUsuario)) {
                JOptionPane.showMessageDialog(this, "Você não tem permissão para acessar este arquivo (não é o dono).", "Acesso negado", JOptionPane.WARNING_MESSAGE);
                Map<String, String> detalhes = new HashMap<>();
                detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                detalhes.put("arq_name", nomeArquivo);
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ACESSO_ARQUIVO_NEGADO, uid, detalhes);
                return;
            }
            try {
                // Buscar chaveiro do usuário logado
                java.util.List<br.com.cofredigital.persistencia.modelo.Chaveiro> chaveiros = usuarioServico.listarChaveirosPorUid(usuarioLogado.getId());
                if (chaveiros.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Chaveiro do usuário logado não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                br.com.cofredigital.persistencia.modelo.Chaveiro chaveiroUsuario = chaveiros.get(0); // Assume o primeiro como padrão
                // Carregar chave privada do usuário logado
                String fraseSecretaUsuario = getFraseSecreta();
                java.security.PrivateKey chavePrivadaUsuario = br.com.cofredigital.crypto.PrivateKeyUtil.loadEncryptedPKCS8PrivateKeyFromDERBytes(
                    chaveiroUsuario.getChavePrivadaCriptografada(), fraseSecretaUsuario
                );
                // Carregar certificado do dono (para assinatura)
                java.security.cert.X509Certificate certificadoDono = null;
                java.security.PublicKey chavePublicaDono = null;
                try {
                    certificadoDono = br.com.cofredigital.crypto.CertificateUtil.loadCertificateFromPEMString(chaveiroUsuario.getCertificadoPem());
                    chavePublicaDono = certificadoDono.getPublicKey();
                } catch (Exception certEx) {
                    JOptionPane.showMessageDialog(this, "Erro de autenticidade: não foi possível obter a chave pública do dono do arquivo. O arquivo pode não ser autêntico.\nDetalhe: " + certEx.getMessage(), "Erro de autenticidade", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro de autenticidade ao carregar chave pública do dono para arquivo: " + certEx.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_VERIFICACAO_FALHA, uid, detalhes);
                    return;
                }
                // Ler arquivos do arquivo secreto
                String basePath = caminhoPasta;
                String prefix = basePath + "/" + nomeCodigo;
                byte[] envBytes = null;
                byte[] encBytes = null;
                byte[] asdBytes = null;
                try {
                    envBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(prefix + ".env");
                    encBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(prefix + ".enc");
                    asdBytes = br.com.cofredigital.util.ArquivoProtegidoUtil.lerArquivo(prefix + ".asd");
                } catch (Exception fileEx) {
                    JOptionPane.showMessageDialog(this, "Erro ao ler arquivos secretos: " + fileEx.getMessage(), "Erro de leitura", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao ler arquivos secretos: " + fileEx.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                byte[] semente = null;
                try {
                    semente = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarEnvelope(envBytes, chavePrivadaUsuario);
                } catch (Exception exEnv) {
                    JOptionPane.showMessageDialog(this, "Erro de sigilo: falha ao decriptar o envelope do arquivo. Verifique se a chave privada está correta.\nDetalhe: " + exEnv.getMessage(), "Erro de sigilo", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao decriptar envelope: " + exEnv.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                javax.crypto.SecretKey chaveAES = null;
                try {
                    chaveAES = br.com.cofredigital.util.ArquivoProtegidoUtil.gerarChaveAES(semente);
                } catch (Exception exAesKey) {
                    JOptionPane.showMessageDialog(this, "Erro ao gerar chave AES: " + exAesKey.getMessage(), "Erro de chave AES", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao gerar chave AES: " + exAesKey.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                byte[] conteudoDecriptado = null;
                try {
                    conteudoDecriptado = br.com.cofredigital.util.ArquivoProtegidoUtil.decriptarArquivoAES(encBytes, chaveAES);
                } catch (Exception exAes) {
                    JOptionPane.showMessageDialog(this, "Erro de sigilo: falha ao decriptar o conteúdo do arquivo. Verifique se a chave está correta.\nDetalhe: " + exAes.getMessage(), "Erro de sigilo", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao decriptar conteúdo: " + exAes.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                boolean assinaturaOk = false;
                try {
                    assinaturaOk = br.com.cofredigital.util.ArquivoProtegidoUtil.verificarAssinatura(conteudoDecriptado, asdBytes, chavePublicaDono);
                    if (assinaturaOk) {
                        Map<String, String> detalhesVerificado = new HashMap<>();
                        detalhesVerificado.put("login_name", usuarioLogado.getEmail());
                        detalhesVerificado.put("arq_name", nomeArquivo);
                        registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_VERIFICADO_OK, uid, detalhesVerificado);
                    }
                } catch (Exception sigEx) {
                    JOptionPane.showMessageDialog(this, "Erro de autenticidade: falha ao verificar a assinatura digital. O arquivo pode não ser autêntico.\nDetalhe: " + sigEx.getMessage(), "Erro de autenticidade", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao verificar assinatura: " + sigEx.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_VERIFICACAO_FALHA, uid, detalhes);
                    return;
                }
                if (!assinaturaOk) {
                    JOptionPane.showMessageDialog(this, "Erro de integridade: o conteúdo do arquivo foi alterado ou corrompido. Assinatura digital inválida.", "Erro de integridade", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_VERIFICACAO_FALHA, uid, detalhes);
                    return;
                }
                
                try {
                    java.nio.file.Files.write(java.nio.file.Paths.get(basePath, nomeArquivo), conteudoDecriptado);
                } catch (Exception writeEx) {
                    JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo decriptado: " + writeEx.getMessage(), "Erro ao salvar", JOptionPane.ERROR_MESSAGE);
                    Map<String, String> detalhes = new HashMap<>();
                    detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                    detalhes.put("arq_name", nomeArquivo);
                    detalhes.put("msg", "Erro ao salvar arquivo decriptado: " + writeEx.getMessage());
                    registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Arquivo decriptado com sucesso e salvo como '" + nomeArquivo + "'!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                Map<String, String> detalhesLog = new HashMap<>();
                detalhesLog.put("login_name", usuarioLogado.getEmail());
                detalhesLog.put("arq_name", nomeArquivo);
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTADO_OK, uid, detalhesLog);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro inesperado ao decriptar arquivo secreto: " + ex.getMessage(), "Erro inesperado", JOptionPane.ERROR_MESSAGE);
                Map<String, String> detalhes = new HashMap<>();
                detalhes.put("login_name", usuarioLogado != null ? usuarioLogado.getEmail() : "N/A");
                detalhes.put("arq_name", nomeArquivo);
                detalhes.put("msg", "Erro inesperado ao decriptar arquivo: " + ex.getMessage());
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_ARQUIVO_DECRIPTACAO_FALHA, uid, detalhes);
            }
        });

        btnVoltar = new JButton("Voltar");
        JPanel linhaBotaoVoltar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        linhaBotaoVoltar.add(btnVoltar);
        body2Panel.add(linhaBotaoVoltar);

        centerPanel.add(body2Panel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Listener do botão Voltar
        btnVoltar.addActionListener(e -> {
            if (usuarioLogado != null && usuarioLogado.getGrupo() != null && usuarioLogado.getGrupo().equalsIgnoreCase("Administrador")) {
                // Logar clique no botão voltar para admin
                Map<String, String> detalhes = new HashMap<>();
                detalhes.put("login_name", usuarioLogado.getEmail());
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_BOTAO_VOLTAR_MENU_PRINCIPAL, usuarioLogado.getId(), detalhes);
                // Voltar para o menu do admin
                java.awt.Component c = this.getParent();
                while (c != null && !(c instanceof javax.swing.JFrame)) {
                    c = c.getParent();
                }
                if (c instanceof MainFrame) {
                    ((MainFrame) c).showScreen(MainFrame.ADMIN_MAIN_PANEL);
                }
            } else if (usuarioLogado != null) {
                // Logar clique no botão voltar para usuário comum
                Map<String, String> detalhes = new HashMap<>();
                detalhes.put("login_name", usuarioLogado.getEmail());
                registroServico.registrarEventoDoUsuario(LogEventosMIDs.CONSULTA_BOTAO_VOLTAR_MENU_PRINCIPAL, usuarioLogado.getId(), detalhes);
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