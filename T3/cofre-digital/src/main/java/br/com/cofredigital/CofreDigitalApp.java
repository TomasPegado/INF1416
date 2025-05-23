// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711

package br.com.cofredigital;

import javax.swing.SwingUtilities;
import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.ui.gui.MainFrame;
import br.com.cofredigital.persistencia.DatabaseManager;

public class CofreDigitalApp {
    
    private static RegistroServico registroServico;
    private static UsuarioServico usuarioServico;
    private static TotpServico totpServico;

    public static void main(String[] args) {
        
        System.out.println("Iniciando aplicação...");
        try {
            Class.forName("org.sqlite.JDBC");
            DatabaseManager.inicializarBanco();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite JDBC não encontrado. Verifique as dependências do Maven.");
            e.printStackTrace();
            return; 
        } catch (RuntimeException e) {
            System.err.println("Falha crítica ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return; 
        }
        System.out.println("Banco de dados inicializado.");

        registroServico = new RegistroServico(); 
        totpServico = new TotpServico();
        usuarioServico = new UsuarioServico(totpServico, registroServico);

        // Log de sistema iniciado
        registroServico.registrarEventoDoSistema(br.com.cofredigital.log.LogEventosMIDs.SISTEMA_INICIADO);

        // Log de sistema encerrado via shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (registroServico != null) {
                registroServico.registrarEventoDoSistema(br.com.cofredigital.log.LogEventosMIDs.SISTEMA_ENCERRADO);
            }
        }));

        // // registroServico.registrarEventoDoSistema(br.com.cofredigital.log.LogEventosMIDs.PARTIDA_SISTEMA, null);
        System.out.println("Serviços de log, TOTP e usuário inicializados.");

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(usuarioServico, totpServico, registroServico);
            mainFrame.setVisible(true);
            System.out.println("Interface gráfica (MainFrame) iniciada e visível.");
        });
    }
} 