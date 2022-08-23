import javafx.scene.control.Alert;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

//A class that handles the cryptography - key generation, encryption, decryption, salt and IV generation and such
public final class Crypt {

	public static int KEY_SIZE = 128; //The encryption and hashing key length in bits
	public static final int GCM_IV_LENGTH = 12; //The GCM initialization vector length in bytes
	public static final int GCM_TAG_LENGTH = 128; //The GCM authentication tag length in bits
	private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding"; //The algorithm used for encryption

	//Called on the start of the program and creates and initializes files and variables
	public static void init()     {
		//Create Program files\Encrypt directory if not exists
		File create = new File(Utility.getDirPath());
		if (!Files.exists(Paths.get(Utility.getDirPath()))) {
			create.mkdir();
		}

		//Creates the config.txt file if not exists
		create = new File(Utility.getDirPath() + Utility.getDataFile());
		try {
			if(!create.createNewFile())
				loadPath(); //If file already exists, loads the path written in it to userPath
			else
				savePath(Utility.getDirPath()); //If file didn't exist, after it's creation, saves the default path in it
			if(!Files.exists(Path.of(loadPath()))) { //If the path loaded from the file doesn't exist in the system, alerts and saves the default path
				Utility.swingAlert("The path specified in config.txt doesn't exist!");
				savePath(Utility.getDirPath());
			} else
				savePath(Utility.getUserPath()); //If the loaded path does exist in the system, save it to the file and the userPath variable
		} catch (IOException e) {
			Utility.swingAlert("An I/O exception occurred!");
			System.exit(0);
		}

		//Create the table in the database if it doesn't exist already
		Utility.createTable();
	}

	//Saves the new path to the file and stores it in userPath
	public static void savePath(String path) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(Utility.getDirPath() + Utility.getDataFile());
			fw.write(path + "\n"); //Writes the path to the file
			fw.close();
			Utility.setUserPath(path); //Stores it userPath
		} catch (IOException e) {
			Utility.swingAlert("An I/O exception occurred, couldn't open file. Please try again.");
		}
	}

	//loads the current path from the file and stores it in userPath and returns the path loaded
	public static String loadPath() {
		String path = Utility.getUserPath(); //Sets default
		try (BufferedReader br = new BufferedReader(new FileReader(Utility.getDirPath() + Utility.getDataFile()))) {
			path = br.readLine(); //Reads first line only
			br.close();
			if(path == null || path.isBlank()) { //If loaded path isn't valid saves and stores default instead
				path = Utility.getDirPath();
				savePath(path);
			}else
				path = path.trim();
		} catch (IOException e) {
			Utility.swingAlert("Couldn't find a file");
		}
		Utility.setUserPath(path); //Stores the loaded path in userPath
		return path;
	}
	
	@NotNull //Generates a salt for hashing or PBKDF-ing from password. Salt is the same length as the key
	public static byte[] genSalt() {
        byte[] salt = new byte[KEY_SIZE/8];
        Random rand = new SecureRandom();
        rand.nextBytes(salt);
        return salt;
	}
	
	@NotNull //Generates a key/hash from the given password and salt using the Argon2id algorithm and returns it.
	public static byte[] keyGen(char[] password, byte[] salt) {
		//Sets the Argon2id to safe parameters
        int iterations = 2;
        int memory = 66536;
        int parallelism = 1;

		//Creates the Argon2id builder with said parameters
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memory)
                .withParallelism(parallelism)
                .withSalt(salt);

		//Creates the Argon2id generator
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] key = new byte[KEY_SIZE/8]; //An array to store the key result
        generator.generateBytes(password, key, 0, KEY_SIZE/8); //Generates the key

		//Deleting password from memory
		for (char c : password) {
			c = '\0';
		}
		return key;
	}

	@NotNull //Generates an initialization vector for the AES encryption.
	public static byte[] genIV(){
		byte[] IV = new byte[GCM_IV_LENGTH];
		new SecureRandom().nextBytes(IV);
		return IV;
	}

	//Handles and does the encryption and decryption from given password, and user id. Executed on the given list of files.
	public static void encDec(String uid, char[] password, boolean isEnc, List<File> files) {
		//Kicks the user out if they aren't logged in
		if(!Utility.isLoggedIn()){
			Utility.alert("You must log in first!", Alert.AlertType.ERROR);
			return;
		}
		//If the user's id or password haven't been entered, kicks them out
		if (uid.isBlank() || password == null || password.length == 0) {
			if(isEnc)
				Utility.alert("Must enter username and password in order to encrypt", Alert.AlertType.ERROR);
			else
				Utility.alert("Must enter username and password in order to decrypt", Alert.AlertType.ERROR);
			return;
		}
		//If no files where chosen, kicks the user out
		if (files == null || files.isEmpty()) {
			Utility.alert("Couldn't find files", Alert.AlertType.ERROR);
			return;
		}
		//Encrypts/Decrypts each file in the list
		for (File file : files) {
			//Create user folder if not exists
			File userFolder = new File(Utility.getUserPath() + File.separator + uid);
			userFolder.mkdir();

			//Create output file if not exists
			File outFile;
			if(isEnc) //If function should encrypt
				outFile = new File(Utility.getUserPath() + File.separator + uid + File.separator + file.getName() + ".enc" + KEY_SIZE); //Creates the encrypted file
			else { //If function should decrypt
				StringBuffer s = new StringBuffer();
				s.append(Utility.getUserPath() + File.separator + uid + File.separator + file.getName());
				outFile = new File(s.substring(0, s.length() - 4 - 3)); //Creates the decrypted file
				s.delete(0, s.length()); //Deletes the file's name form memory
			}
			try {
				outFile.createNewFile();
			} catch (IOException e) {
				Utility.alert("An I/O exception occurred.", Alert.AlertType.ERROR);
				return;
			}

			byte[] salt = null;
			byte[] IV = null;

			//Retrieves the encryption salt and initialization vector for the given id from the database
			String[] res = Utility.getSaltNIV(Integer.parseInt(uid));
			if(res == null)
				return;
			Charset charset = StandardCharsets.UTF_16BE;
			salt = charset.encode(res[0]).array(); //Stores the salt
			IV = charset.encode(res[1]).array(); //Stores the IV

			//Generates the key
			byte[] encodedKey = keyGen(password, salt);

			//Deleting password from memory
			for (char c : password) {
				c = '\0';
			}

			//Creates the AES key from the generated byte[]
			SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
			try {
				//Sets up cipher
				Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
				GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, IV);
				if(isEnc) //If function should encrypt
					cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
				else //If function should decrypt
					cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

				//Creates file i/o streams to write the file
				FileInputStream is = new FileInputStream(file);
				FileOutputStream os = new FileOutputStream(outFile);

				//Encrypts/Decrypts and writes a buffer each time
				byte[] buffer = new byte[128];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					byte[] out = cipher.update(buffer, 0, bytesRead);
					if (out != null) {
						os.write(out);
					}
				}
				//Deletes the buffer from memory
				for (byte b: buffer) {
					b = 0;
				}
				//Finishes the encryption/decryption
				byte[] out = cipher.doFinal();
				if (out != null) {
					os.write(out);
				}
				//Deletes the final buffer from memory
				if(out != null) {
					for (byte b : out) {
						b = 0;
					}
				}
				is.close();
				os.close();
			} catch (Exception e){
				if(isEnc) { //If function should encrypt
					Utility.alert("Something went wrong: couldn't encrypt!", Alert.AlertType.ERROR);
				}
				else //If function should decrypt
					Utility.alert("Something went wrong: couldn't decrypt!", Alert.AlertType.ERROR);
				return;
			}
		}
		Utility.setHasEncDec(true); //Sets the boolean that indicates the program has encrypted/decrypted at least once in order to prevent folder change
		//Tells the user the operation executed successfully
		Utility.alert("Operation executed successfully!", Alert.AlertType.CONFIRMATION);
		//Clears the file list
		files.clear();
	}
}
