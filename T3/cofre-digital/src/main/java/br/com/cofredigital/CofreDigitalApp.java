package br.com.cofredigital;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;

import javax.swing.SwingUtilities; // Para iniciar a GUI na thread correta
import br.com.cofredigital.ui.gui.MainFrame; // Nossa futura janela principal

public class CofreDigitalApp {

    private static UsuarioServico usuarioServico;
    private static TotpServico totpServico;

    public static void main(String[] args) {
        // Inicializar serviços
        totpServico = new TotpServico();
        usuarioServico = new UsuarioServico(totpServico); // UsuarioServico depende de TotpServico

        // Iniciar a Interface Gráfica (GUI) na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Cria e exibe a janela principal, passando os serviços necessários
            MainFrame mainFrame = new MainFrame(usuarioServico, totpServico);
            mainFrame.setVisible(true);
        });
    }

    // TODO Futuro: Mover a inicialização dos serviços para um local mais apropriado
    //              (talvez injeção de dependência se o projeto crescer).
} 