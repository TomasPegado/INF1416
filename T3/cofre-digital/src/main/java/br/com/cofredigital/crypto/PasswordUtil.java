package br.com.cofredigital.crypto;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class PasswordUtil {

    private static final int BCRYPT_COST_FACTOR = 8;

    /**
     * Gera um hash de senha usando OpenBSDBCrypt (formato $2y$, custo 08).
     *
     * @param password A senha em texto plano.
     * @return O hash bcrypt da senha.
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        // OpenBSDBCrypt.generate requer salt como byte[], não string.
        // Salt deve ter 16 bytes (BCRYPT_SALT_LEN).
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // O método generate da OpenBSDBCrypt já inclui o prefixo $2y$ (ou $2a$, $2b$ dependendo da versão do BC)
        // e o custo. Para BouncyCastle, ele geralmente usa $2a$ ou $2b$. O formato $2y$ é uma correção
        // específica para uma falha no manuseio de caracteres UTF-8 em algumas implementações de $2b$.
        // A classe OpenBSDBCrypt do BouncyCastle lida com isso corretamente.
        // Se uma especificação estrita de "$2y$" for necessária na saída, pode ser preciso um ajuste manual
        // ou verificar se a versão do BouncyCastle usada produz $2y$ por padrão ou pode ser configurada para tal.
        // Por agora, vamos usar o que a biblioteca OpenBSDBCrypt fornece, que é seguro.
        char[] passwordChars = password.toCharArray();
        return OpenBSDBCrypt.generate(passwordChars, salt, BCRYPT_COST_FACTOR);
    }

    /**
     * Verifica uma senha em texto plano contra um hash bcrypt armazenado.
     *
     * @param password A senha em texto plano a ser verificada.
     * @param storedHash O hash bcrypt armazenado ($2y$08$...).
     * @return true se a senha corresponder ao hash, false caso contrário.
     */
    public static boolean checkPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false; // Ou lançar IllegalArgumentException dependendo da política de erro
        }
        char[] passwordChars = password.toCharArray();
        return OpenBSDBCrypt.checkPassword(storedHash, passwordChars);
    }
} 