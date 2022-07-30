import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

//Code taken from https://github.com/nbaars/pwnedpasswords4j/blob/main/pwnedpasswords4j-client/src/main/java/com/github/nbaars/pwnedpasswords4j/client/Hex.java
//in order to change that the password won't be saved as String in memory
public class PwnedPasswordChecker {

    private final PwnedPasswordClient client;

    public PwnedPasswordChecker(PwnedPasswordClient client) {
        this.client = client;
    }

    public CompletableFuture<Boolean> asyncCheck(char[] password) {
        Hex hashedPassword = hashPassword(password);
        //Deletes the password from memory
        if(password != null) {
            for (char c : password) {
                c = '\0';
            }
        }
        return client.fetchHashesAsync(hashedPassword).thenApplyAsync(x -> x.contains(hashedPassword));
    }

    public boolean check(char[] password) {
        try {
            return asyncCheck(password).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            //Deletes the password from memory
            if(password != null) {
                for (char c : password) {
                    c = '\0';
                }
            }
        }
    }

    @NotNull
    private Hex hashPassword(@NotNull char[] password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] temp = new byte[password.length];
            for (int i = 0; i < password.length; i++) {
                temp[i] = (byte)password[i];
            }

            //Deletes the password from memory
            if(password != null) {
                for (char c : password) {
                    c = '\0';
                }
            }
            byte[] digest = messageDigest.digest(temp);
            for (int i = 0; i < temp.length; i++) {
                temp[i] = 0;
            }
            //Deletes the password in the byte array representation from memory
            if(temp != null) {
                for (byte b : temp) {
                    b = 0;
                }
            }
            return Hex.from(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}