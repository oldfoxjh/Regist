package kr.co.enord.dji.utils;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES256Util {
    //키값 32바이트: AES256(24: AES192, 16: AES128)
    public static String secretKey = "Enord.Util.Crypto";
    public static String ivString = "Crypto";

    //AES256 암호화
    public static String aesEncode(String str) {
        try {
            byte[] textBytes = str.getBytes("UTF-8");
            String ivSha = sha256ToString(ivString).substring(0,16);
            String keySha = sha256ToString(secretKey).substring(0,32);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivSha.getBytes("UTF-8"));
            SecretKeySpec newKey = new SecretKeySpec(keySha.getBytes("UTF-8"), "AES");
            Cipher cipher = null;
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);

            return Base64.encodeToString(cipher.doFinal(textBytes), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    //AES256 복호화
    public static String aesDecode(String str) {
        try {
            byte[] textBytes = Base64.decode(str, Base64.NO_WRAP);
            String ivSha = sha256ToString(ivString).substring(0,16);
            String keySha = sha256ToString(secretKey);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivSha.getBytes("UTF-8"));
            SecretKeySpec newKey = new SecretKeySpec(keySha.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);

            return new String(cipher.doFinal(textBytes), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String sha256ToString(String input) {
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes());
            byte[] bytes = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            result = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
}