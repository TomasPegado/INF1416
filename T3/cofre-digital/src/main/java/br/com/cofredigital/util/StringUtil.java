package br.com.cofredigital.util;

public class StringUtil {

    /**
     * Verifica se alguma das strings fornecidas é nula ou vazia (após trim).
     *
     * @param strings As strings a serem verificadas.
     * @return true se pelo menos uma string for nula ou vazia, false caso contrário.
     */
    public static boolean isAnyEmpty(String... strings) {
        if (strings == null) {
            return true; // Considera nulo como "vazio" neste contexto
        }
        for (String str : strings) {
            if (str == null || str.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se todas as strings fornecidas são nulas ou vazias (após trim).
     *
     * @param strings As strings a serem verificadas.
     * @return true se todas as strings forem nulas ou vazias, false caso contrário.
     */
    public static boolean areAllEmpty(String... strings) {
        if (strings == null) {
            return true; // Ou false, dependendo da interpretação. Aqui, um array nulo não tem strings não vazias.
        }
        for (String str : strings) {
            if (str != null && !str.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
} 