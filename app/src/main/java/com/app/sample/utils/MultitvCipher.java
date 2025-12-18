package com.app.sample.utils;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MultitvCipher {
    private final String iv = "0543737198408118";
    private final IvParameterSpec ivspec;
    private final SecretKeySpec keyspec;
    private Cipher cipher;
    private final String SecretKey = "89H49I12T20E542N17D4E5A47R184LKL";

    public MultitvCipher() {
        this.ivspec = new IvParameterSpec(this.iv.getBytes());
        this.keyspec = new SecretKeySpec(this.SecretKey.getBytes(), "AES");

        try {
            this.cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException var2) {
            var2.printStackTrace();
        } catch (NoSuchPaddingException var3) {
            var3.printStackTrace();
        }

    }

    public byte[] encryptmyapi(String text) throws Exception {
        if (text != null && text.length() != 0) {

            try {
                this.cipher.init(1, this.keyspec, this.ivspec);
                return this.cipher.doFinal(padString(text).getBytes());
            } catch (Exception var4) {
                throw new Exception("[encrypt] " + var4.getMessage());
            }
        } else {
            throw new Exception("Empty string");
        }
    }

    public byte[] decryptmyapi(String code) throws Exception {
        if (code != null && code.length() != 0) {

            try {
                this.cipher.init(2, this.keyspec, this.ivspec);
                return this.cipher.doFinal(hexToBytes(code));
            } catch (Exception var4) {
                throw new Exception("[decrypt] " + var4.getMessage());
            }
        } else {
            throw new Exception("Empty string");
        }
    }

    public static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        } else {
            StringBuilder str = new StringBuilder();

            for (byte datum : data) {
                if ((datum & 255) < 16) {
                    str.append("0").append(Integer.toHexString(datum & 255));
                } else {
                    str.append(Integer.toHexString(datum & 255));
                }
            }

            return str.toString();
        }
    }

    public static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        } else if (str.length() < 2) {
            return null;
        } else {
            int len = str.length() / 2;
            byte[] buffer = new byte[len];

            for(int i = 0; i < len; ++i) {
                buffer[i] = (byte)Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
            }

            return buffer;
        }
    }

    private static String padString(String source) {
        char paddingChar = ' ';
        int size = 16;
        int x = source.length() % size;
        int padLength = size - x;

        StringBuilder sourceBuilder = new StringBuilder(source);
        for(int i = 0; i < padLength; ++i) {
            sourceBuilder.append(paddingChar);
        }
        source = sourceBuilder.toString();

        return source;
    }
}