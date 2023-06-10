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
/**
 * An encryption engine for storing and managing keys. It also provides methods for data encryption
 */
public class EncryptionManager {
    /**
     * Local key - hashed password also used for encryption of RSA private key
     */
    private byte[] localKey;
    /**
     * Public key for RSA encryption (PKI)
     */
    private byte[] publicKey;
    /**
     * Private key for RSA encryption (PKI)
     */
    private byte[] privateKey;
    /**
     * IV for private key encryption
     */
    private byte[] privateKeyIV;

    /**
     * Randomly generated session key for encrypting messages
     */
    private byte[] sessionKey;
    /**
     * Public key of a friend for
     */
    private volatile byte[] friendPublicKey;

    public EncryptionManager() {
        friendPublicKey = null;

    }


    /**
     * Method for hashing a password
     * @param password - a passowrd to be hashed
     * @return a hash of password in String format
     */
    public HashCode shaHashingToString(String password) {
        return Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8);
    }

    /**
     * A method for checking if provided password is correct
     * @param hashedPassword - hashed password
     * @param passwordFile - a path to file with the hashed password
     * @return - true if given password was correct and false if not
     * @throws FileNotFoundException
     */
    public boolean isPasswordCorrect(String hashedPassword, File passwordFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(passwordFile);
        if (scanner.hasNextLine()) {
            String passwordFromFile = scanner.nextLine();
            return hashedPassword.equals(passwordFromFile);
        }
        scanner.close();
        return false;
    }

    /**
     * A method to generate a pair of RSA keys (PKI)
     * @return - returns the result of the operation (false if creation was unsuccessful)
     */
    public boolean generateRSAkeys(){
        try{
            // generate a pair of keys
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            // Public key - save to file
            FileOutputStream fos = new FileOutputStream("src/main/resources/publicKey/public.key");
            publicKey = pair.getPublic().getEncoded();
            fos.write(publicKey);
            fos.close();

            // Private key - encrypt with local key and save to file
            privateKey = pair.getPrivate().getEncoded();
            byte[] encryptedPrivateKey = encryptAES(pair.getPrivate().getEncoded(), localKey);
            FileOutputStream fosPrivate = new FileOutputStream("src/main/resources/privateKey/private.key");
            fosPrivate.write(encryptedPrivateKey);
            fosPrivate.close();

            return true;
        }catch(RuntimeException | NoSuchAlgorithmException | IOException ex){
            System.err.println("Key generation failed");
            // Uninitialise the keys to avoid confusion
            publicKey = null;
            privateKey = null;
            return false;
        }

    }

    /**
     * Method for realizoing AES encryption
     * @param data - the data to be encrypted
     * @param key - the key to be used during encryption
     * @param iv - the initialization vector to be used during encryption (does not matter in ECB mode)
     * @param mode - mode (currently ECB or CBC
     * @return - returns a byte array - encrypted data
     */
    @SneakyThrows
    public static byte[] encryptAES(byte[] data, byte[] key, byte[] iv, CipherMode mode){

        Cipher cipher = null;
        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

        switch(mode){
            case CBC: // operations for CBC mode
                cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                IvParameterSpec ivspec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivspec);
                break;

            case ECB: // operations for ECB mode
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, aesKey);

                break;
        }

        // do final encryption
        byte[] result = cipher.doFinal(data);
        // return encrypted data
        return  result;
    }

    /**
     * AES encryption - used for Private Key encryption
     * @param data - data to be encrypted
     * @param key - key to encrypt the data with
     * @return - encrypted data
     */
    @SneakyThrows
    public byte[] encryptAES(byte[] data, byte[] key){

        // generate the iv and store in memory
        byte[] iv = generateRandomBytes(16);
        privateKeyIV = iv;
        // CBC mode as required
        byte[] result = encryptAES(data, key, iv, CipherMode.CBC);
        return  result;
    }


    /**
     * A method for AES decryption
     * @param data - encrypted data to be decrypted
     * @param key - key for decryption
     * @param iv - iv for decryption
     * @param mode - AES mode
     * @return - a byte array of decrypted data
     */
    @SneakyThrows
    public static byte[] decryptAES(byte[] data, byte[] key, byte[] iv, CipherMode mode){
        Cipher cipher = null;
        SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

        switch(mode){
            case CBC: // operations for CBC mode
                cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                IvParameterSpec ivspec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, aesKey, ivspec);
                break;

            case ECB: // operations for ECB mode
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, aesKey);
                break;

        }

        // decrypt data and return results
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

    /**
     * Random byte array generator
     * @param n - the length of array
     * @return the array of length n with random bytes
     */
    public static byte[] generateRandomBytes(int n){
        Random rand = new Random();
        byte[] result = new byte[n];
        rand.nextBytes(result);
        return result;
    }

    /**
     * Session key generator - also sets the sessionKey
     * @return - return the session key
     */
    public byte[] generateAndSetSessionKey(){
        byte[] sessionKey = generateRandomBytes(32);
        this.sessionKey = sessionKey;
        return sessionKey;
    }
}
