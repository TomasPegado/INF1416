// Gabriela Soares: 2210347
// Tomás Lenzi: 2220711
package br.com.cofredigital.persistencia.dao;

import br.com.cofredigital.persistencia.modelo.Registro;
import java.sql.SQLException;
import java.util.List; // Opcional, para métodos futuros

public interface RegistroDAO {
    /**
     * Salva um novo registro de evento no banco de dados.
     * O rid do objeto Registro deve ser populado se for gerado pelo banco.
     * @param registro O objeto Registro a ser salvo.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    void salvar(Registro registro) throws SQLException;

    // Métodos opcionais para consulta (podem ser adicionados depois, se necessário)
    List<Registro> listarTodos() throws SQLException;
    /*
    List<Registro> buscarPorUid(long uid) throws SQLException;
    List<Registro> buscarPorMid(int mid) throws SQLException;
    List<Registro> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) throws SQLException;
    */
} 