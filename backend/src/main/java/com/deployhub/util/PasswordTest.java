package com.deployhub.util;

public class PasswordTest {
    public static void main(String[] args) {
        String password = "Admin@9000";
        String encrypted = PasswordUtil.encrypt(password);
        System.out.println(encrypted);
    }
}
