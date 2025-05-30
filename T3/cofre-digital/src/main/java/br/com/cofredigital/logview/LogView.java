// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711

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
        // Removed authentication prompts and logic
        try {
            UsuarioDAO usuarioDAO = new UsuarioDAOImpl();
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
                // 1. Substituir <login_name> se presente
                if (mensagemTexto.contains("<login_name>") && reg.getUid() != null) {
                    Optional<Usuario> userOpt = usuarioDAO.buscarPorId(reg.getUid());
                    String loginName = userOpt.map(Usuario::getNome).orElse("uid=" + reg.getUid());
                    mensagemTexto = mensagemTexto.replace("<login_name>", loginName);
                }
                // 2. Substituir todos os placeholders presentes em detalhesAdicionais
                if (reg.getDetalhesAdicionais() != null && !reg.getDetalhesAdicionais().isEmpty()) {
                    // Parse simples: chave1='valor1', chave2='valor2'
                    String[] partes = reg.getDetalhesAdicionais().split(", ");
                    for (String parte : partes) {
                        int idx = parte.indexOf("='");
                        if (idx > 0 && parte.endsWith("'")) {
                            String chave = parte.substring(0, idx);
                            String valor = parte.substring(idx + 2, parte.length() - 1).replace("''", "'");
                            String placeholder = "<" + chave + ">";
                            mensagemTexto = mensagemTexto.replace(placeholder, valor);
                        }
                    }
                }
                String dataHora = reg.getDataHora() != null ? reg.getDataHora().toString() : "[sem data]";
                System.out.printf("[%s] %s\n", dataHora, mensagemTexto);
            }

        } catch (Exception e) {
            System.err.println("Erro ao acessar o banco de dados: " + e.getMessage());
        }
    }
} 