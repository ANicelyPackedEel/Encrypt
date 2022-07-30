import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import okhttp3.OkHttpClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

//The controller of the create account scene and the corresponding fxml
public class CreateAccSceneController {

	//For scene switching
    private Runnable onBack = () -> {};

	private char[] password; //An array to store the plain-text password received

    @FXML
    private Label idForUser; //A label that tells the user what is their ID
    
    @FXML
    private Label passStrghIndicator; //A label that tells the user how strong is the current password they typed

	@FXML
	private MFXProgressBar progBar; //A progress bar that indicates the strength of teh password entered

	//For scene switching
	public void setOnBack(Runnable onBack) {
        this.onBack = onBack ;
    }

	//A function that checks and returns whether the entered password is valid - if the password hasn't been pwned, is longer then 8 characters and is strong.
	private boolean isPasswordValid() {
		//Setup the client and the checker to search the site through the api inorder to see whether the password has been pawned.
		//The password isn't sent to the api as plain-text, it is hashed and only the first 5 characters on the hash is sent, the api then
		//sends back a list of all possibly matching password and the client searches them to see if the password is there. This is called k-anonymity.
		PwnedPasswordClient client = new PwnedPasswordClient(new OkHttpClient(), "https://api.pwnedpasswords.com/range", "");
		PwnedPasswordChecker checker = new PwnedPasswordChecker(client);
		if(password.length < 8) {
			Utility.alert("Password length must be 8 characters or longer", Alert.AlertType.ERROR);
			return false;
		} else if(progBar.getProgress() < 0.75) {
			Utility.alert("Password strength muse be \"Strong\" or \"Very strong\"", Alert.AlertType.ERROR);
			return false;
		} else if(checker.check(password)) {
			Utility.alert("Password has been pwned! do not use it!", Alert.AlertType.ERROR);
			return false;
		} else
			return true;
	}

	private void evaluateStrength() {
		Zxcvbn zxcvbn = new Zxcvbn();
		StringBuffer temp = new StringBuffer();
		Strength strength = zxcvbn.measure(temp.append(password));
		temp.delete(0,temp.length());
		String strghIndcator = "";
		int score = strength.getScore();
		switch (score) {
			case 0 -> strghIndcator = "Weak";
			case 1 -> strghIndcator = "Fair";
			case 2 -> strghIndcator = "Good";
			case 3 -> strghIndcator = "Strong";
			case 4 -> strghIndcator = "Very strong";
		}
		passStrghIndicator.setText(strghIndcator);
		double guesses = strength.getGuesses();
		if(guesses < (Math.pow(10,3) + 5))
			score = 0;
		else if(guesses < (Math.pow(10,4) + 5))
			score = 1;
		else if(guesses < (Math.pow(10,5) + 5))
			score = 2;
		else if(guesses < (Math.pow(10,6) + 5))
			score = 3;
		else if(guesses < (Math.pow(10,7) + 5))
			score = 4;
		else if(guesses < (Math.pow(10,8) + 5))
			score = 5;
		else if(guesses < (Math.pow(10,9) + 5))
			score = 6;
		else if(guesses < (Math.pow(10,10) + 5))
			score = 7;
		else
			score = 8;
		progBar.setProgress(score/8.0);
	}

	@FXML //This function is called when the Enter password button is pressed and creates a swing password field to get the password from because JavaFX's password field can only return String which cannot be deleted from memory
	void enterPassword(ActionEvent event) {
		//Creates frame and password field
		final JFrame frame = new JFrame("Enter a password");
		frame.setLocationRelativeTo(null); //Centers frame on screen
		JLabel lblPassword = new JLabel("Password:");
		final JPasswordField pfPassword = new JPasswordField(20);
		lblPassword.setLabelFor(pfPassword);

		//Adds a listener to the password field so that whenever a key is typed, the password will be updated and evaluated for it's strength
		pfPassword.addKeyListener(new KeyAdapter() {
			public void keyPressed(java.awt.event.KeyEvent e) {
				password = pfPassword.getPassword();
				Platform.runLater(() -> evaluateStrength()); //Runs the evaluateStrength() function in a JavaFX thread
				//If enter is typed, saves the password and closes the password field window so the user won't have to press the OK button
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					password = pfPassword.getPassword();
					JComponent comp = (JComponent) e.getSource();
					Window win = SwingUtilities.getWindowAncestor(comp);
					win.dispose();
				}
			}
		});

		//Creates the OK button to save the password and close the password field window
		JButton okBtn = new JButton("Ok");
		//Adds a listener to the button in order to close the password field window and save the password when the button is pressed
		okBtn.addActionListener(e -> {
			password = pfPassword.getPassword();
			JComponent comp = (JComponent) e.getSource();
			Window win = SwingUtilities.getWindowAncestor(comp);
			win.dispose();
		});

		//Sets the panel, layout and other options of the password field window
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		panel.add(lblPassword);
		panel.add(pfPassword);
		panel.add(okBtn);

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setSize(400, 80);
		frame.getContentPane().add(panel);
		frame.setVisible(true);
		frame.toFront();
		frame.repaint();
	}
    
    @FXML //This function is called when the back button is pressed and switches to the main scene
    void switchToEncryptScene(ActionEvent event) {
		//Deletes the plain-text password from memory
		if(password != null) {
			for (char c : password) {
				c = '\0';
			}
		}
		//Clears all indicators so that the user switches to this scene again, those values won't stay the same
		idForUser.setText("");
		passStrghIndicator.setText("");
		progBar.setProgress(0);
    	onBack.run(); //Switches the scene
    }

    @FXML //This function is called when the Create button is pressed, and it creates an account with the entered password, saves it to the database and gives the user an ID
    void createAccount(ActionEvent event) {
		if(!isPasswordValid()) //Don't create an account if the password isn't strong enough, or has been pwned
			return;
		byte[] salt = Crypt.genSalt(); //Generates a salt to use to hash the user's password
		int uid;
		if((uid = Utility.getID()) == -1) //Gets the user id from the database (user id is just the number of users+1)
			return;
		idForUser.setText(String.valueOf(uid)); //Shows the id on the label

		//Inserts the hashed password with the generated salt to the database, along with the salt, another different salt used for creating the key from the password rather then hashing the password inorder to save it,
		//and with an initialization vector used for the AES encryption
		if(!Utility.insert(Crypt.keyGen(password, salt), salt, Crypt.genSalt(), Crypt.genIV()))
			return;
		//Deletes the password from memory
		if(password != null) {
			for (char c : password) {
				c = '\0';
			}
		}
		//Gives feedback to user
		Utility.alert("Account created successfully!", Alert.AlertType.CONFIRMATION);
    }
}
