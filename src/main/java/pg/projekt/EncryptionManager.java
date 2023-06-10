package pg.projekt;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.Scanner;

@Getter
@Setter
public class EncryptionManager {
    private byte[] localKey;
    private byte[] publicKey;
    private byte[] privateKey;

    private byte[] sessionKey;

    private volatile byte[] friendPublicKey;

    public EncryptionManager() {
        friendPublicKey = null;

    }

    public void setLocalKey(byte[] localKey) {
        this.localKey = localKey;
    }

    public HashCode shaHashingToString(String password) {
        return Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8);
    }

    // Checking if hash of entered password equals the hash saved in the file
    public boolean isPasswordCorrect(String hashedPassword, File passwordFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(passwordFile);
        if (scanner.hasNextLine()) {
            String passwordFromFile = scanner.nextLine();
            return hashedPassword.equals(passwordFromFile);
        }
        scanner.close();
        return false;
    }

    public boolean generateRSAkeys(){
        try{
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            // save to file
            FileOutputStream fos = new FileOutputStream("src/main/resources/publicKey/public.key");
            fos.write(pair.getPublic().getEncoded());
            publicKey = pair.getPublic().getEncoded();
            fos.close();


            privateKey = pair.getPrivate().getEncoded();
            // encrypt and save private key
            byte[] encryptedPrivateKey = encryptAES(pair.getPrivate().getEncoded(), localKey);

            FileOutputStream fosPrivate = new FileOutputStream("src/main/resources/privateKey/private.key");
            fosPrivate.write(encryptedPrivateKey);
            fosPrivate.close();

            return true;
        }catch(RuntimeException | NoSuchAlgorithmException | IOException ex){
            System.err.println("Key generation failed");
            return false;
        }

    }

    @SneakyThrows
    public static byte[] encryptAES(byte[] data, byte[] key, byte[] iv, CipherMode mode){
        Cipher cipher = null;

        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

        switch(mode){
            case CBC:
                cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

                cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivspec);

                break;
            case ECB:
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

                cipher.init(Cipher.ENCRYPT_MODE, aesKey);
                break;
        }

        byte[] result = cipher.doFinal(data);

        return  result;
    }

    @SneakyThrows
    public static byte[] encryptAES(byte[] data, byte[] key){


        byte[] iv = new byte[16];
        // IV as 16 first bytes of key
        for(int i =0 ;i < iv.length; i++){
            iv[i]= key[i];
        }

        byte[] result = encryptAES(data, key, iv, CipherMode.ECB);

        return  result;
    }


    @SneakyThrows
    public static byte[] decryptAES(byte[] data, byte[] key, byte[] iv, CipherMode mode){
        Cipher cipher = null;

        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

        switch(mode){
            case CBC:
                cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

                cipher.init(Cipher.DECRYPT_MODE, aesKey, ivspec);

                break;
            case ECB:
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

                cipher.init(Cipher.DECRYPT_MODE, aesKey);
                break;
        }

        byte[] result = cipher.doFinal(data);

        return  result;
    }
    @SneakyThrows
    public static byte[] encryptRSA(byte[] data, byte[] key, boolean isPublic) {
        Cipher cipher = Cipher.getInstance("RSA");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
        Key encryptionKey;
        if (isPublic) {
            keySpec = new X509EncodedKeySpec(key);
            encryptionKey = keyFactory.generatePublic(keySpec);
        }
        else {
            keySpec = new PKCS8EncodedKeySpec(key);
            encryptionKey = keyFactory.generatePrivate(keySpec);
        }

        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] result = cipher.doFinal(data);
        return result;
    }

    @SneakyThrows
    public static byte[] decryptRSA(byte[] data, byte[] key, boolean isPublic) {
        Cipher cipher = Cipher.getInstance("RSA");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec keySpec;
        Key decryptionKey;
        if (isPublic) {
            keySpec = new X509EncodedKeySpec(key);
            decryptionKey = keyFactory.generatePublic(keySpec);
        }
        else {
            keySpec = new PKCS8EncodedKeySpec(key);
            decryptionKey = keyFactory.generatePrivate(keySpec);
        }

        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        byte[] result = cipher.doFinal(data);
        return result;
    }

    public static byte[] generateRandomBytes(int n){
        Random rand = new Random();
        byte[] result = new byte[n];
        rand.nextBytes(result);
        return result;
    }

    public byte[] generateSessionKey(){
        byte[] sessionKey = generateRandomBytes(32);
        this.sessionKey = sessionKey;
        return sessionKey;
    }
}
