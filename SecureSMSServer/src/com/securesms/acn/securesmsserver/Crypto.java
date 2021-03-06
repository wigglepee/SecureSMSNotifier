package com.securesms.acn.securesmsserver;


import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Base64;


public class Crypto {

    private static int AES_KEY_LENGTH = 16;
    private static int GCM_TAG_LENGTH = 16;
    public static int GCM_NONCE_LENGTH = 8;

    private KeyGenerator keyGen;
    private SecretKey secretKey;
    private Cipher gcmCipher;
    private byte[] nonce;
    GCMParameterSpec spec;

    public Crypto(String savedKey){
        try{
            if (savedKey != null){
                secretKey = convertBase64ToKey(savedKey);
            }
            else{
                keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(AES_KEY_LENGTH * 8);
                secretKey = keyGen.generateKey();
            }
            gcmCipher = Cipher.getInstance("AES/GCM/NoPadding");
            nonce = new byte[GCM_NONCE_LENGTH];
        }
        catch(Exception e){e.printStackTrace();}
    }

    public void setUpCrypto(String nonceBase64){
        this.nonce = decodeBase64ToByteArray(nonceBase64);
        setNewSpec();
    }

    // Key -> String
    public String convertKeyToBase64(){
        return encodeArrayToBase64(secretKey.getEncoded());
    }


    // String -> Key
    public SecretKey convertBase64ToKey(String keyBase64){
        byte[] encodedKey     = decodeBase64ToByteArray(keyBase64);
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }

    private void setNewSpec(){
        this.spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        try{
            gcmCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        }
        catch(InvalidKeyException | InvalidAlgorithmParameterException e){e.printStackTrace();}
    }

    private void incNonce(){
        ByteBuffer getBuffer = ByteBuffer.wrap(nonce);
        ByteBuffer putBuffer = ByteBuffer.allocate(Long.BYTES);
        long l = getBuffer.getLong();
        l += 1;
        putBuffer.putLong(l);
        nonce = putBuffer.array();
    }

    public String getNonce(){
        return encodeArrayToBase64(nonce);
    }

    private String encodeArrayToBase64(byte[] array){
        return Base64.getMimeEncoder().encodeToString(array);
    }

    private byte[] decodeBase64ToByteArray(String stringBase64){
        return Base64.getMimeDecoder().decode(stringBase64);
    }



    private byte[] decrypt(byte[] ciphertext){
        byte[] plaintext = null;
        try {
            plaintext = gcmCipher.doFinal(ciphertext);
        }
        catch(IllegalBlockSizeException| BadPaddingException e){e.printStackTrace();}

        return plaintext;
    }


    public String decodeAndDecrypt(String ciphertextBase64) {
        try {
            String plaintext = new String(decrypt(decodeBase64ToByteArray(ciphertextBase64)), "UTF-8");
            incNonce();
            setNewSpec();
            return plaintext;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
