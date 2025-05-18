package br.com.cofredigital;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.ui.gui.MainFrame;
import javax.swing.SwingUtilities;
import br.com.cofredigital.persistencia.DatabaseManager;

public class CofreDigitalApp {

    public static void main(String[] args) {
        // 0. Inicializar o banco de dados e criar tabelas se não existirem
        try {
            Class.forName("org.sqlite.JDBC"); // Garante que o driver JDBC do SQLite seja carregado
            DatabaseManager.inicializarBanco();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite JDBC não encontrado. Verifique as dependências do Maven.");
            e.printStackTrace();
            // Considerar encerrar a aplicação aqui se o driver não for encontrado, pois o DB é essencial.
            return; // Sai da aplicação se não puder carregar o driver
        } catch (RuntimeException e) {
            System.err.println("Falha crítica ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            // Encerra a aplicação se o banco não puder ser inicializado.
            return; 
        }

        // 1. Inicializar os serviços necessários
        TotpServico totpServico = new TotpServico();
        UsuarioServico usuarioServico = new UsuarioServico(totpServico);

        // 2. Garantir que a UI seja criada e atualizada no Event Dispatch Thread (EDT)
        // Isso é uma boa prática para aplicações Swing para evitar problemas de concorrência.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // 3. Criar e exibir o MainFrame
                MainFrame mainFrame = new MainFrame(usuarioServico, totpServico);
                mainFrame.setVisible(true);
            }
        });
    }

    // TODO Futuro: Mover a inicialização dos serviços para um local mais apropriado
    //              (talvez injeção de dependência se o projeto crescer).
} 