package br.com.cofredigital;

import br.com.cofredigital.autenticacao.servico.TotpServico;
import br.com.cofredigital.autenticacao.servico.UsuarioServico;
import br.com.cofredigital.log.LogEventosMIDs;
import br.com.cofredigital.log.servico.RegistroServico;
import br.com.cofredigital.ui.gui.MainFrame;
import javax.swing.SwingUtilities;
import br.com.cofredigital.persistencia.DatabaseManager;
import java.util.Map;

public class CofreDigitalApp {

    public static void main(String[] args) {
        RegistroServico registroServico = null;

        try {
            Class.forName("org.sqlite.JDBC");
            DatabaseManager.inicializarBanco();
            
            registroServico = new RegistroServico();
            registroServico.registrarEventoDoSistema(LogEventosMIDs.SISTEMA_INICIADO, (Map<String, String>) null);

        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite JDBC não encontrado. Verifique as dependências do Maven.");
            e.printStackTrace();
            return; 
        } catch (RuntimeException e) {
            System.err.println("Falha crítica ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return; 
        }

        final RegistroServico servicoLogFinal = registroServico; 
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (servicoLogFinal != null) {
                System.out.println("Registrando encerramento do sistema...");
                servicoLogFinal.registrarEventoDoSistema(LogEventosMIDs.SISTEMA_ENCERRADO, (Map<String, String>) null);
            } else {
                System.err.println("Shutdown hook: RegistroServico não inicializado, não foi possível logar encerramento.");
            }
        }));

        TotpServico totpServico = new TotpServico();
        UsuarioServico usuarioServico = new UsuarioServico(totpServico, registroServico);

        final RegistroServico rsParaUI = registroServico;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame mainFrame = new MainFrame(usuarioServico, totpServico);
                mainFrame.setVisible(true);
            }
        });
    }

    // TODO Futuro: Mover a inicialização dos serviços para um local mais apropriado
    //              (talvez injeção de dependência se o projeto crescer).
} 