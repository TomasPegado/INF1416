// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.ui.gui;

// Não usaremos mais TecladoVirtualInputHandler diretamente aqui para montar a senha
// import br.com.cofredigital.tecladovirtual.TecladoVirtualInputHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Painel Swing para o teclado virtual numérico de 5 botões.
 * Cada botão exibe um par de dígitos distintos (0-9), e os 10 dígitos são
 * distribuídos aleatoriamente entre os botões.
 * A cada clique, o PAR de dígitos do botão clicado é registrado, e os pares nos botões são reembaralhados.
 * A validação da senha ocorrerá externamente, verificando se o dígito real da senha
 * estava contido no par do botão clicado para aquela posição.
 */
public class TecladoVirtualPanel extends JPanel {
    // private final TecladoVirtualInputHandler inputHandler; // Removido
    private final JTextField senhaVisualField;
    private final JPanel botoesDeParesPanel;
    private final JButton[] botoesDePares = new JButton[5];
    private final JButton botaoApagar;
    private final JButton botaoLimpar;

    private final List<Character> todosOsDigitos =
        Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

    // Armazena os pares ATUALMENTE exibidos nos botões.
    // Cada elemento é Character[2], ex: {'1','2'}
    private final List<Character[]> paresAtualmenteNosBotoes = new ArrayList<>(5);

    // Armazena a sequência de PARES dos botões que foram CLICADOS pelo usuário.
    // Esta lista é o que será usado para a validação externa.
    private final List<Character[]> sequenciaDeParesSelecionados = new ArrayList<>();

    private final Random random = new Random();
    private Runnable onInteractionCallback; // Removido do construtor, agora é um campo com setter

    public TecladoVirtualPanel() { // Callback removido do construtor
        this.senhaVisualField = new JTextField(10); // Tamanho fixo para até 10 asteriscos
        senhaVisualField.setEditable(false);
        senhaVisualField.setHorizontalAlignment(JTextField.CENTER);
        senhaVisualField.setFont(new Font("Monospaced", Font.BOLD, 20)); // Fonte maior para '*'

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel visualizacaoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        visualizacaoPanel.add(new JLabel("Senha:"));
        visualizacaoPanel.add(senhaVisualField);
        add(visualizacaoPanel, BorderLayout.NORTH);

        botoesDeParesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        criarBotoesDePares();
        add(botoesDeParesPanel, BorderLayout.CENTER);

        JPanel painelControle = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botaoApagar = new JButton("<");
        botaoApagar.setToolTipText("Apagar último dígito");
        botaoApagar.setFont(new Font("SansSerif", Font.BOLD, 16));
        botaoApagar.setPreferredSize(new Dimension(70, 50));
        botaoApagar.addActionListener(e -> {
            if (!sequenciaDeParesSelecionados.isEmpty()) {
                sequenciaDeParesSelecionados.remove(sequenciaDeParesSelecionados.size() - 1);
                atualizarSenhaVisual();
                if (this.onInteractionCallback != null) { // Verifica se o callback foi definido
                    this.onInteractionCallback.run();
                }
            }
        });
        painelControle.add(botaoApagar);

        botaoLimpar = new JButton("C");
        botaoLimpar.setToolTipText("Limpar toda a senha");
        botaoLimpar.setFont(new Font("SansSerif", Font.BOLD, 16));
        botaoLimpar.setPreferredSize(new Dimension(70, 50));
        botaoLimpar.addActionListener(e -> {
            limparSequenciaDePares();
            embaralharEDisporParesNosBotoes(); // Reembaralha ao limpar
            if (this.onInteractionCallback != null) { // Verifica se o callback foi definido
                this.onInteractionCallback.run();
            }
        });
        painelControle.add(botaoLimpar);
        add(painelControle, BorderLayout.SOUTH);

        embaralharEDisporParesNosBotoes();
        atualizarSenhaVisual();
    }

    public void setOnInteractionCallback(Runnable callback) {
        this.onInteractionCallback = callback;
    }

    private void criarBotoesDePares() {
        botoesDeParesPanel.removeAll();
        for (int i = 0; i < 5; i++) {
            JButton btn = new JButton();
            btn.setFont(new Font("SansSerif", Font.BOLD, 18));
            btn.setPreferredSize(new Dimension(90, 70));
            // O ActionListener agora usará o índice para pegar o par de `paresAtualmenteNosBotoes`
            btn.addActionListener(criarListenerBotaoDePar(i));
            botoesDePares[i] = btn;
            botoesDeParesPanel.add(btn);
        }
        botoesDeParesPanel.revalidate();
        botoesDeParesPanel.repaint();
    }

    private void embaralharEDisporParesNosBotoes() {
        List<Character> digitosEmbaralhados = new ArrayList<>(todosOsDigitos);
        Collections.shuffle(digitosEmbaralhados);

        paresAtualmenteNosBotoes.clear();
        for (int i = 0; i < 10; i += 2) { // Forma 5 pares
            paresAtualmenteNosBotoes.add(new Character[]{
                digitosEmbaralhados.get(i),
                digitosEmbaralhados.get(i+1)
            });
        }

        // Embaralha a ordem dos pares que vão para os botões
        Collections.shuffle(paresAtualmenteNosBotoes);

        for (int i = 0; i < 5; i++) {
            Character[] par = paresAtualmenteNosBotoes.get(i);
            // Formato "D1 / D2" para clareza visual
            String textoBotao = String.valueOf(par[0]) + " / " + String.valueOf(par[1]);
            botoesDePares[i].setText(textoBotao);
            // O ActionCommand não é mais usado diretamente para formar a senha,
            // o listener usará o índice do botão.
        }
    }

    private ActionListener criarListenerBotaoDePar(int indiceBotao) {
        return e -> {
            // Pega o par de dígitos que ESTAVA no botão clicado
            if (indiceBotao < paresAtualmenteNosBotoes.size()) {
                Character[] parSelecionado = paresAtualmenteNosBotoes.get(indiceBotao);

                // Adiciona o par à sequência de pares selecionados
                // Limita o tamanho da senha (número de cliques)
                if (sequenciaDeParesSelecionados.size() < 10) { // Senha de até 10 dígitos
                    sequenciaDeParesSelecionados.add(parSelecionado);
                    atualizarSenhaVisual();
                }
                // Sempre reembaralha após um clique válido que adicionou um par
                embaralharEDisporParesNosBotoes();
                if (this.onInteractionCallback != null) { // Verifica se o callback foi definido
                    this.onInteractionCallback.run();
                }
            }
        };
    }

    private void atualizarSenhaVisual() {
        int numDigitos = sequenciaDeParesSelecionados.size();
        char[] masked = new char[numDigitos];
        Arrays.fill(masked, '*');
        senhaVisualField.setText(new String(masked));
        // O tamanho do campo já é fixo (10), então não precisa ajustar colunas
    }

    /**
     * Retorna a lista de pares de dígitos correspondentes aos botões
     * que o usuário clicou em sequência.
     * Cada item da lista é um Character[2].
     * @return A lista de pares de dígitos selecionados.
     */
    public List<Character[]> getSequenciaDeParesSelecionados() {
        return new ArrayList<>(sequenciaDeParesSelecionados); // Retorna uma cópia para evitar modificação externa
    }

    /**
     * Limpa a sequência de pares selecionados e atualiza a visualização.
     * Chamado pelo LoginPanel ou internamente pelo botão Limpar.
     */
    public void limparSenha() { // Renomeado para manter consistência se LoginPanel chamar
        limparSequenciaDePares();
        // Reembaralhar é responsabilidade do chamador ou do evento de limpar, se necessário
    }

    private void limparSequenciaDePares() {
        sequenciaDeParesSelecionados.clear();
        atualizarSenhaVisual();
    }
} 