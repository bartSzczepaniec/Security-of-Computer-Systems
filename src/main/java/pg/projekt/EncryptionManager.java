package pg.projekt;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class EncryptionManager {
    private byte[] localKey;
    private byte[] publicKey;
    private byte[] privateKey;

    private byte[] sessionKey;

    public EncryptionManager() {

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
    public byte[] encryptAES(byte[] data, byte[] key){
        byte[] iv = new byte[16];
        // IV as 16 first bytes of key
        for(int i =0 ;i < iv.length; i++){
            iv[i]= key[i];
        }



        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivspec);

        byte[] result = cipher.doFinal(data);

        return  result;
    }


    public void generateSessionKey(){
        Random rand = new Random();
        byte[] sessionKey = new byte[32];
        rand.nextBytes(sessionKey);
        this.sessionKey = sessionKey;
    }
}
