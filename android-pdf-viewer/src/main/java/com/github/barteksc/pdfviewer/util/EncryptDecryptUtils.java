package com.github.barteksc.pdfviewer.util;

import android.content.Context;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.github.barteksc.pdfviewer.util.Constants.CIPHER_ALGORITHM;
import static com.github.barteksc.pdfviewer.util.Constants.KEY_SPEC_ALGORITHM;
import static com.github.barteksc.pdfviewer.util.Constants.OUTPUT_KEY_LENGTH;


public class EncryptDecryptUtils {

    public static EncryptDecryptUtils instance = null;
    private static PrefUtils prefUtils;

    public static EncryptDecryptUtils getInstance(Context context) {

        if (null == instance)
            instance = new EncryptDecryptUtils();

        if (null == prefUtils)
            prefUtils = PrefUtils.getInstance(context);

        return instance;
    }

    public static byte[] encode(SecretKey yourKey, byte[] fileData)
            throws Exception {
        byte[] data = yourKey.getEncoded();
        SecretKeySpec skeySpec = new SecretKeySpec(data, 0, data.length, KEY_SPEC_ALGORITHM);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        return cipher.doFinal(fileData);
    }

    public static byte[] decode(SecretKey yourKey, byte[] fileData)
            throws Exception {
        byte[] decrypted;
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, yourKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        decrypted = cipher.doFinal(fileData);
        return decrypted;
    }

    public void saveSecretKey(SecretKey secretKey) {
        String encodedKey = Base64.encodeToString(secretKey.getEncoded(), Base64.NO_WRAP);
        prefUtils.saveSecretKey(encodedKey);
    }

    /*
     * Use this method to aut--generate key
     *
     * */
    public SecretKey getSecretKey() {
        String encodedKey = prefUtils.getSecretKey();
        if (null == encodedKey || encodedKey.isEmpty()) {
            SecureRandom secureRandom = new SecureRandom();
            KeyGenerator keyGenerator = null;
            try {
                keyGenerator = KeyGenerator.getInstance(KEY_SPEC_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if (keyGenerator != null) {
                keyGenerator.init(OUTPUT_KEY_LENGTH, secureRandom);
                SecretKey secretKey = keyGenerator.generateKey();
                saveSecretKey(secretKey);
                return secretKey;
            }
        }

        byte[] decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEY_SPEC_ALGORITHM);
    }

    /*
     * Use this method to generate the key using user specified key
     *
     * */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public SecretKey getSecretKey(String userKey) {
        String encodedKey = prefUtils.getSecretKey();
        if (null == encodedKey || encodedKey.isEmpty()) {
            byte[] key = userKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
                key = sha.digest(key);
                key = Arrays.copyOf(key, 16); // use only first 128 bit
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            SecretKeySpec secretKey = new SecretKeySpec(key, KEY_SPEC_ALGORITHM);
            saveSecretKey(secretKey);
            return secretKey;
        }

        byte[] decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEY_SPEC_ALGORITHM);
    }

}
