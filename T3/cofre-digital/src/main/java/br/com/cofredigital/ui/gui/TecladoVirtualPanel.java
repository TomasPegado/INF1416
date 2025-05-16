package br.com.cofredigital.ui.gui;

import br.com.cofredigital.tecladovirtual.TecladoVirtualLogic;
import br.com.cofredigital.tecladovirtual.TecladoVirtualInputHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

/**
 * Painel Swing para o teclado virtual seguro (layout QWERTY simplificado).
 */
public class TecladoVirtualPanel extends JPanel {
    private final TecladoVirtualInputHandler inputHandler;
    private final JTextField senhaVisualField;
    private final Runnable onSenhaAlterada;
    private final List<List<String>> linhasTeclas;
    private final List<JPanel> linhasPainel = new ArrayList<>();

    public TecladoVirtualPanel(Runnable onSenhaAlterada) {
        this.inputHandler = new TecladoVirtualInputHandler();
        this.onSenhaAlterada = onSenhaAlterada;
        this.senhaVisualField = new JTextField(20);
        senhaVisualField.setEditable(false);
        senhaVisualField.setFocusable(false); // Bloqueia foco para evitar digitação física
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(700, 400)); // Aumenta o espaço do painel
        add(senhaVisualField, BorderLayout.NORTH);
        // Define o layout QWERTY simplificado
        linhasTeclas = Arrays.asList(
            Arrays.asList("q","w","e","r","t","y","u","i","o","p"),
            Arrays.asList("a","s","d","f","g","h","j","k","l"),
            Arrays.asList("z","x","c","v","b","n","m"),
            Arrays.asList("1","2","3","4","5","6","7","8","9","0"),
            Arrays.asList("!","@","#","$","%","&","*","-","_","."),
            Arrays.asList("<","C")
        );
        add(criarTeclado(), BorderLayout.CENTER);
    }

    private JPanel criarTeclado() {
        JPanel tecladoPanel = new JPanel();
        tecladoPanel.setLayout(new BoxLayout(tecladoPanel, BoxLayout.Y_AXIS));
        linhasPainel.clear();
        for (List<String> linha : linhasTeclas) {
            JPanel linhaPanel = new JPanel();
            linhaPanel.setLayout(new GridLayout(1, linha.size(), 8, 2)); // GridLayout para melhor distribuição
            List<String> linhaEmbaralhada = new ArrayList<>(linha);
            Collections.shuffle(linhaEmbaralhada); // Embaralha cada linha separadamente
            for (String tecla : linhaEmbaralhada) {
                JButton btn = new JButton(tecla);
                btn.setPreferredSize(new Dimension(60, 40)); // Botões maiores
                btn.setMinimumSize(new Dimension(50, 35));
                btn.setMaximumSize(new Dimension(80, 50));
                btn.addActionListener(criarListener());
                linhaPanel.add(btn);
            }
            linhasPainel.add(linhaPanel);
            tecladoPanel.add(linhaPanel);
        }
        return tecladoPanel;
    }

    private ActionListener criarListener() {
        return e -> {
            String cmd = e.getActionCommand();
            if (cmd.equals("<")) {
                inputHandler.removerUltimoChar();
            } else if (cmd.equals("C")) {
                inputHandler.limpar();
            } else {
                inputHandler.adicionarChar(cmd.charAt(0));
            }
            atualizarSenhaVisual();
            if (onSenhaAlterada != null) onSenhaAlterada.run();
        };
    }

    private void atualizarSenhaVisual() {
        char[] senha = inputHandler.getSenha();
        char[] masked = new char[senha.length];
        Arrays.fill(masked, '*');
        senhaVisualField.setText(new String(masked));
    }

    public char[] getSenha() {
        return inputHandler.getSenha();
    }

    public void embaralharTeclas() {
        // Embaralha cada linha e atualiza os botões
        for (int i = 0; i < linhasTeclas.size(); i++) {
            List<String> linha = linhasTeclas.get(i);
            List<String> embaralhada = new ArrayList<>(linha);
            Collections.shuffle(embaralhada);
            JPanel linhaPanel = linhasPainel.get(i);
            linhaPanel.removeAll();
            linhaPanel.setLayout(new GridLayout(1, embaralhada.size(), 8, 2)); // Garante GridLayout após embaralhar
            for (String tecla : embaralhada) {
                JButton btn = new JButton(tecla);
                btn.setPreferredSize(new Dimension(60, 40));
                btn.setMinimumSize(new Dimension(50, 35));
                btn.setMaximumSize(new Dimension(80, 50));
                btn.addActionListener(criarListener());
                linhaPanel.add(btn);
            }
            linhaPanel.revalidate();
            linhaPanel.repaint();
        }
    }

    public void limparSenha() {
        inputHandler.limpar();
        atualizarSenhaVisual();
    }
} 