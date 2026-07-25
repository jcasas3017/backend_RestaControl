package com.utp.restacontrol.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarPassword {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "123456";

        String hash = encoder.encode(password);

        System.out.println("Hash BCrypt:");
        System.out.println(hash);

        System.out.println(
                "Validación: " + encoder.matches(password, hash)
        );
    }
}
