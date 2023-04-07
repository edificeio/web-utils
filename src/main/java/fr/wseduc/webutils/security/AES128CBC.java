package fr.wseduc.webutils.security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AES128CBC
{
    private static Cipher getCipher(final int opmode, String key, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if(iv == null)
            iv = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        try
        {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(opmode, skeySpec, ivSpec);
            return cipher;
        }
        catch (UnsupportedEncodingException e) { /* Cannot happen */ throw new RuntimeException(e); }
        catch (NoSuchAlgorithmException e) { /* Cannot happen */ throw new RuntimeException(e); }
        catch (NoSuchPaddingException e) { /* Cannot happen */ throw new RuntimeException(e); }
    }

    //-------------------------------- ENCRYPTION

    public static byte[] encryptBytes(String value, String key) throws InvalidKeyException
    {
        try
        {
            return encryptBytes(value, key, null);
        }
        catch (InvalidAlgorithmParameterException e) { /* Cannot happen */ throw new RuntimeException(e); }
    }

    public static byte[] encryptBytes(String value, String key, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        try {
            return getCipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(value.getBytes());
        }
        catch (IllegalBlockSizeException e) { /* Cannot happen */ throw new RuntimeException(e); }
        catch (BadPaddingException e) { /* Canoot happen */ throw new RuntimeException(e); }
    }

    public static String encrypt(String value, String key) throws InvalidKeyException
    {
        return Base64.getEncoder().encodeToString(encryptBytes(value, key));
    }

    public static String encrypt(String value, String key, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        return Base64.getEncoder().encodeToString(encryptBytes(value, key, iv));
    }

    //-------------------------------- DECRYPTION

    public static byte[] decryptBytes(byte[] value, String key) throws InvalidKeyException
    {
        try
        {
            return decryptBytes(value, key, null);
        }
        catch (InvalidAlgorithmParameterException e) { /* Cannot happen */ throw new RuntimeException(e); }
    }

    public static byte[] decryptBytes(byte[] value, String key, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        try {
            return getCipher(Cipher.DECRYPT_MODE, key, iv).doFinal(value);
        }
        catch (IllegalBlockSizeException e) { /* Cannot happen */ throw new RuntimeException(e); }
        catch (BadPaddingException e) { /* Canoot happen */ throw new RuntimeException(e); }
    }

    public static String decrypt(String value, String key) throws InvalidKeyException
    {
        return new String( decryptBytes(Base64.getDecoder().decode(value), key) );
    }

    public static String decrypt(String value, String key, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException
    {
        return new String( decryptBytes(Base64.getDecoder().decode(value), key, iv) );
    }
}
