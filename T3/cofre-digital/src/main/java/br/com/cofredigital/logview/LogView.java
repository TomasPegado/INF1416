package br.com.cofredigital.logview;

import br.com.cofredigital.autenticacao.modelo.Usuario;
import br.com.cofredigital.persistencia.dao.UsuarioDAO;
import br.com.cofredigital.persistencia.dao.UsuarioDAOImpl;
import br.com.cofredigital.persistencia.dao.RegistroDAO;
import br.com.cofredigital.persistencia.dao.RegistroDAOImpl;
import br.com.cofredigital.persistencia.dao.MensagemDAO;
import br.com.cofredigital.persistencia.dao.MensagemDAOImpl;
import br.com.cofredigital.persistencia.modelo.Registro;
import br.com.cofredigital.persistencia.modelo.Mensagem;
import br.com.cofredigital.crypto.PasswordUtil;

import java.util.Scanner;
import java.util.Optional;
import java.util.List;

public class LogView {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== LogView - Visualização de Registros do Sistema ===");
        System.out.print("Email do administrador: ");
        String email = scanner.nextLine().trim();
        System.out.print("Senha do administrador: ");
        String fraseSecreta = scanner.nextLine();

        try {
            UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
            Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorEmail(email);
            if (usuarioOpt.isEmpty()) {
                System.out.println("Usuário não encontrado.");
                return;
            }
            Usuario usuario = usuarioOpt.get();
            // Verificar se é admin (ajustar conforme campo do modelo)
            if (usuario.getGrupo() == null || 
                !(usuario.getGrupo().equalsIgnoreCase("admin") || usuario.getGrupo().equalsIgnoreCase("administrador"))) {
                System.out.println("Acesso negado: apenas administradores podem visualizar os logs.");
                return;
            }
            // Validar frase secreta (usando PasswordUtil)
            if (!PasswordUtil.checkPassword(fraseSecreta, usuario.getSenha())) {
                System.out.println("Frase secreta incorreta.");
                return;
            }
            System.out.println("Autenticação bem-sucedida! Listando registros do sistema...\n");

            RegistroDAO registroDAO = new RegistroDAOImpl();
            MensagemDAO mensagemDAO = new MensagemDAOImpl();
            // Buscar e exibir todos os registros ordenados por data/hora
            List<Registro> registros = registroDAO.listarTodos();
            if (registros.isEmpty()) {
                System.out.println("Nenhum registro encontrado.");
                return;
            }
            System.out.println("==== Registros do Sistema ====");
            for (Registro reg : registros) {
                Optional<Mensagem> mensagemOpt = mensagemDAO.buscarPorId(reg.getMid());
                String mensagemTexto = mensagemOpt.map(Mensagem::getTextoMensagem).orElse("[Mensagem não encontrada para MID " + reg.getMid() + "]");
                // Substituir parâmetros na mensagem, se necessário
                if (mensagemTexto.contains("<login_name>") && reg.getUid() != null) {
                    // Buscar nome do usuário pelo UID
                    Optional<Usuario> userOpt = usuarioDAO.buscarPorId(reg.getUid());
                    String loginName = userOpt.map(Usuario::getNome).orElse("uid=" + reg.getUid());
                    mensagemTexto = mensagemTexto.replace("<login_name>", loginName);
                }
                if (mensagemTexto.contains("<arq_name>") && reg.getDetalhesAdicionais() != null) {
                    mensagemTexto = mensagemTexto.replace("<arq_name>", reg.getDetalhesAdicionais());
                }
                // Exibir data/hora, mensagem, UID, detalhes
                String dataHora = reg.getDataHora() != null ? reg.getDataHora().toString() : "[sem data]";
                System.out.printf("[%s] %s\n", dataHora, mensagemTexto);
            }

        } catch (Exception e) {
            System.err.println("Erro ao acessar o banco de dados: " + e.getMessage());
        }
    }
} 