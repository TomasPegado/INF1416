package br.com.cofredigital.ui.gui;

import br.com.cofredigital.tecladovirtual.TecladoVirtualLogic;
import br.com.cofredigital.tecladovirtual.TecladoVirtualInputHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * Painel Swing para o teclado virtual seguro.
 */
public class TecladoVirtualPanel extends JPanel {
    private final TecladoVirtualLogic logic;
    private final TecladoVirtualInputHandler inputHandler;
    private final JTextField senhaVisualField;
    private final Runnable onSenhaAlterada;

    public TecladoVirtualPanel(List<Character> teclasBase, Runnable onSenhaAlterada) {
        this.logic = new TecladoVirtualLogic(teclasBase);
        this.inputHandler = new TecladoVirtualInputHandler();
        this.onSenhaAlterada = onSenhaAlterada;
        this.senhaVisualField = new JTextField(20);
        senhaVisualField.setEditable(false);
        senhaVisualField.setFocusable(false); // Bloqueia foco para evitar digitação física
        setLayout(new BorderLayout());
        add(senhaVisualField, BorderLayout.NORTH);
        add(criarTeclado(), BorderLayout.CENTER);
    }

    private JPanel criarTeclado() {
        JPanel panel = new JPanel(new GridLayout(4, 4, 5, 5));
        atualizarTeclas(panel);
        return panel;
    }

    private void atualizarTeclas(JPanel panel) {
        panel.removeAll();
        List<Character> layout = logic.getLayoutAtual();
        ActionListener listener = e -> {
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
        for (char c : layout) {
            JButton btn = new JButton(String.valueOf(c));
            btn.addActionListener(listener);
            panel.add(btn);
        }
        JButton backspace = new JButton("<");
        backspace.addActionListener(listener);
        panel.add(backspace);
        JButton clear = new JButton("C");
        clear.addActionListener(listener);
        panel.add(clear);
        panel.revalidate();
        panel.repaint();
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
        logic.embaralharTeclas();
        atualizarTeclas((JPanel) getComponent(1));
    }

    public void limparSenha() {
        inputHandler.limpar();
        atualizarSenhaVisual();
    }
} 