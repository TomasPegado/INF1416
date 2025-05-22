// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.util.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AcessoBloqueadoException extends Exception {
    public AcessoBloqueadoException(String email, LocalDateTime bloqueadoAte) {
        super(formatarMensagem(email, bloqueadoAte));
    }

    private static String formatarMensagem(String email, LocalDateTime bloqueadoAte) {
        String dataFormatada = "indefinidamente";
        if (bloqueadoAte != null) {
            dataFormatada = bloqueadoAte.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
        return "Acesso bloqueado para o usuário " + email + ". Tente novamente após " + dataFormatada + ".";
    }

    public AcessoBloqueadoException(String message) {
        super(message);
    }
} 