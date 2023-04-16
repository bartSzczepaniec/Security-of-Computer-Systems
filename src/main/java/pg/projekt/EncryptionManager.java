package pg.projekt;

import com.google.common.hash.Hashing;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class EncryptionManager {
    private String localKey;

    public EncryptionManager() {

    }

    public void setLocalKey(String localKey) {
        this.localKey = localKey;
    }

    public String shaHashingToString(String password) {
        return Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
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
}
