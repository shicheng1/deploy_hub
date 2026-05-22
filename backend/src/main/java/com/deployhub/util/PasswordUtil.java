package com.deployhub.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

public class PasswordUtil {

    private static final byte[] KEY = "DeployHub2024Key".getBytes();

    private static final AES AES = SecureUtil.aes(KEY);

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return AES.encryptHex(plainText);
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return AES.decryptStr(cipherText);
        } catch (Exception e) {
            return cipherText;
        }
    }
}
