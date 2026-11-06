
package com.example.controlefinanceiro.utils;

import org.mindrot.jbcrypt.BCrypt;

public class SenhaUtils {

    private SenhaUtils() {
        // Impede criar objetos desta classe utilitária.
    }

    public static String gerarHash(String senha) {
        return BCrypt.hashpw(
                senha,
                BCrypt.gensalt(12)
        );
    }

    public static boolean verificarSenha(
            String senhaDigitada,
            String hashArmazenado
    ) {
        if (senhaDigitada == null ||
                hashArmazenado == null) {
            return false;
        }

        return BCrypt.checkpw(
                senhaDigitada,
                hashArmazenado
        );
    }
}
