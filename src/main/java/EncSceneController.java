import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

//The controller of the encryption/decryption/main scene and the corresponding fxml
public class EncSceneController {

    private Runnable onNext = () -> {}; //For scene switching
    private ChangeListener<String> listener; //Stores the id change listener that forces the id label to be able to accept numbers only
    private List<File> files; //Stores a list of the files chosen to encrypt/decrypt
	
    @FXML
    private TextField id; //A text field for the user to enter his id in order to log in

    @FXML
    private Label logInIndicator; //Tells the user whether he is logged in

    @FXML
    private ImageView image; //Stores the image view for the image in the ui

    private char[] password; //An array to store the plain-text password received

    //This function is called on the start of the program automatically
    public void initialize() {
    	//Force id field to be numeric only
    	listener = (observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                id.setText(newValue.replaceAll("[^\\d]", "")); //Replaces any non-numeric character with an empty string immediately
            }
        };
        id.textProperty().addListener(listener); //Adds the listener to the text field
        image.setImage(new Image("key.png")); //Loads the image for the ui
    }

    //For scene switching
    public void setOnNext(Runnable onNext) {
        this.onNext = onNext ;
    }

    @FXML //This function is called when the Enter password button is pressed and creates a swing password field to get the password from because JavaFX's password field can only return String which cannot be deleted from memory
    void enterPassword(ActionEvent event) {
        //Creates frame and password field
        final JFrame frame = new JFrame("Enter your password");
        frame.setLocationRelativeTo(null); //Centers frame on screen
        JLabel lblPassword = new JLabel("Password:");
        final JPasswordField pfPassword = new JPasswordField(20);
        lblPassword.setLabelFor(pfPassword);

        //Adds a listener to the password field so that whenever a key is typed, the password will be updated and so when the Enter key is pressed the window will close
        pfPassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    password = pfPassword.getPassword();
                    JComponent comp = (JComponent) e.getSource();
                    Window win = SwingUtilities.getWindowAncestor(comp);
                    win.dispose();
                }
                password = pfPassword.getPassword();
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

    @FXML //This function is called when the Select files button is pressed, and it opens a file chooser for the user to choose the files they want to encrypt/decrypt and saves them in the file list
    void selectFiles(ActionEvent event) {
        //Set the initial directory of the file chooser to the user folder if it exists, if not, sets it to the program's folder - \Program files\Encrypt
        File initDir = new File(Utility.getUserPath() + File.separator + id.getText());
        if(!Files.exists(Path.of(initDir.getAbsolutePath()))) {
            initDir = new File(Utility.getDirPath());
        }
        FileChooser fileChooser = new FileChooser(); //Creates the file chooser
        fileChooser.setInitialDirectory(initDir);
        fileChooser.setTitle("Select Files");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*")); //Enables the user to choose any file type
        files = fileChooser.showOpenMultipleDialog(((Node)event.getSource()).getScene().getWindow()); //Shows the file chooser dialog, enables the user to choose multiple files and stores them in the files list
    }

    @FXML //This function is called when the Decrypt button is pressed and calls the encDec() function in Crypt to decrypt the files with the given password and user id
    void Decrypt(@NotNull ActionEvent event) {
        Crypt.encDec(id.getText(), password, false, files);
    }

    @FXML //This function is called when the Encrypt button is pressed and calls the encDec() function in Crypt to encrypt the files with the given password and user id
    void Encrypt(@NotNull ActionEvent event) {
        Crypt.encDec(id.getText(), password, true, files);
    }

//    @FXML //File dragging doesn't work with administrator privileges which are need in order to write files in the Program files folder
//    void handleDragDropped(DragEvent event) {
//    	files = event.getDragboard().getFiles();
//    }
//
//    @FXML
//    void handleDragOver(DragEvent event) {
//    	if(event.getDragboard().hasFiles())
//    		event.acceptTransferModes(TransferMode.ANY);
//    }

    @FXML //This function is called when the Log in button is pressed. It checks the user and passwords validity, and checks them against the database to find a match and sets the login boolean in Utility class to true if successful
    void logIn(ActionEvent event) {
        //Checks if the user entered a password and id, if not, tells them and returns;
        if (id.getText().isBlank() || password == null || password.length == 0){
            Utility.alert("Must enter username and password in order to log in", Alert.AlertType.ERROR);
            return;
        }
        String[] res;
        if((res = Utility.getPassNSalt(Integer.parseInt(id.getText()))) == null) //Gets the hashed password and the salt from the database
            return;
        if(res[0] == null || res[1] == null)
            return;
        Charset charset = StandardCharsets.UTF_16BE;
        //Generates the hash based on the entered password and loaded salt from the database
        byte[] key = Crypt.keyGen(password, charset.encode(res[1]).array());

        //Checks if the hash created is equal to the hash stored in the database, if so - logs in
        //Uses the MessageDigest.isEqual to compare because it is safer and isn't vulnerable to timing based attacks
        if(!MessageDigest.isEqual(key, charset.encode(res[0]).array())){
            System.out.println(key+"\n");
            System.out.println(charset.encode(res[0]).array());
            Utility.alert("Password and username aren't matching or doesn't exist!", Alert.AlertType.ERROR);
            return;
        }
        System.out.println(key+"\n");
        System.out.println(charset.encode(res[0]).array());

        //Deletes the key from memory
        for (byte b: key) {
            b = 0;
        }

        //Indicates to user that the log in was successful
        logInIndicator.setText("Logged in!");
        Utility.setLoggedIn(true);
        Utility.alert("Logged in!", Alert.AlertType.CONFIRMATION);
    }

    @FXML //This function is called when the Log out button is pressed. It delets the stored password from memory, sets the login boolean in the Utility class to false and tells the user the log out was successful
    void logOut(ActionEvent event) {
        //Deletes password from memory
        if(password != null) {
            for (char c : password) {
                c = '\0';
            }
        }
        password = null;
        //Indicates to user log out was successful
        logInIndicator.setText("Not logged in!");
        id.setText("");
        Utility.setLoggedIn(false); //Sets the boolean to false so encryption/decryption will be rejected
        Utility.alert("Logged out!", Alert.AlertType.CONFIRMATION);
    }

    @FXML //This function is called when the Create account button is pressed and switches to the other scene
    void switchToCreateAccountScene(ActionEvent event) {
        //Clears the id field
    	id.setText("");
        //If user is logged in - logs out before switching
        if(Utility.isLoggedIn())
            logOut(event);
        else {
            //Deletes the password from memory
            if(password != null) {
                for (char c : password) {
                    c = '\0';
                }
            }
        }
        //Switches scene
    	onNext.run();
    }

    @FXML //This function is called when the Select folder button is pressed. It opens a directory chooser for the user to choose the directory they want to save the encryption/decryption results and saves the path in the config.txt file
    void setFolder(ActionEvent event) {
        //In order to not mix encrypted and decrypted files in to two folders, prevents changing folder after encryption/decryption occurred
        if(Utility.isHasEncDec()) {
           Utility.alert("Set folder before starting encryption/decryption!", Alert.AlertType.ERROR);
           return;
        }
        //Sets the initial directory to the folder in which the user folders are saved
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(Crypt.loadPath()));
        File dir = dc.showDialog(((Node)event.getSource()).getScene().getWindow()); //Shows the directory chooser to the user
        //Saves the new directory path in to the config.txt file
        if(dir == null || dir.getAbsolutePath().isBlank()) {
            return;
        }
        Crypt.savePath(dir.getAbsolutePath());
    }
}