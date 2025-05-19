package br.com.cofredigital.ui.gui;

import br.com.cofredigital.autenticacao.modelo.Usuario;

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
        body2Panel.add(linhaCaminho);

        JPanel linhaFrase = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linhaFrase.add(new JLabel("Frase secreta:"));
        txtFraseSecreta = new JPasswordField(30);
        linhaFrase.add(txtFraseSecreta);
        body2Panel.add(linhaFrase);

        btnListar = new JButton("Listar");
        JPanel linhaBotaoListar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linhaBotaoListar.add(btnListar);
        body2Panel.add(linhaBotaoListar);

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
} 