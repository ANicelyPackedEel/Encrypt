import javafx.scene.control.Alert;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.*;

//A class that handles the communication with the database and other shared resources and services
public class Utility {

    private static final String dbName = "DB.db"; //The database's file name
    private static Connection c;

    private static final String dataFile = File.separator + "config.txt"; //The data file name
    private static final String dirPath = System.getenv("ProgramFiles") + File.separator + "Encrypt"; //The program's directory path - Program files\Encrypt
    private static String userPath = System.getenv("ProgramFiles") + File.separator + "Encrypt"; //The folder holding the users' folders' path. Default is Program files\Encrypt
    private static boolean hasEncDec = false; //A boolean that indicates whether the program has encrypted/decrypted already in this session in order to prevent changing folders
    private static boolean loggedIn = false; //A boolean that indicates whether the use is logged in


    //Returns the program's directory
    public static String getDirPath(){
        return dirPath;
    }

    //Returns the folder holding the users' folders' path
    public static String getUserPath(){
        return userPath;
    }

    //Returns the data file name
    public static String getDataFile() {
        return dataFile;
    }

    //Returns the boolean that indicates whether the program has encrypted/decrypted already in this session
    public static boolean isHasEncDec() {
        return hasEncDec;
    }

    //Returns the boolean that indicates whether the use is logged in
    public static boolean isLoggedIn() {
        return loggedIn;
    }

    //Sets the userPath to a new path
    public static void setUserPath(String path){
        userPath = path;
    }

    //Sets boolean that indicates whether the use is logged in to a new value
    public static void setLoggedIn(boolean loggedIn) {
        Utility.loggedIn = loggedIn;
    }

    //Sets the boolean that indicates whether the program has encrypted/decrypted already in this session to a new value
    public static void setHasEncDec(boolean hasEncDec) {
        Utility.hasEncDec = hasEncDec;
    }

    //Used to alert the user of any message needed
    public static void alert(String errMsg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(errMsg);
        alert.showAndWait();
    }

    //Used to alert the user of any message needed from functions that run before JavaFx initializes
    public static void swingAlert(String errMsg) {
        JOptionPane optionPane = new JOptionPane(errMsg, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = optionPane.createDialog("Error:");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);

    }

    //Connects to the database and returns the connection
    public static Connection connect() {
        String url = "jdbc:sqlite:" + dirPath + File.separator + dbName;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            swingAlert("Something went wrong: couldn't connect to database");
        }
        c = conn;
        return conn;
    }

    //Gets a new id to give a user based on the number of users in the database+1 and returns it
    public static int getID() {
        int id = -1;
        try (Connection conn = connect(); //Connects to the db
            Statement stmt = conn.createStatement(); //Creates a statement
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM LOGIN")){ //Runs the query and stores the result
            rs.next();
            id = rs.getInt(1) + 1; //Gets the new id number
        } catch (SQLException e) {
            Utility.alert("Something went wrong: couldn't get id number", Alert.AlertType.ERROR);
        }
        return id;
    }

    @Nullable //Gets the password and hashing salt from the db for the given user id
    public static String[] getPassNSalt(int id) {
        String[] res = null;
        String sql = "SELECT ROWID,PASSWORD,SALT FROM LOGIN WHERE ROWID=?";
        try (Connection conn = connect(); //Connects to the db
            PreparedStatement pstmt = conn.prepareStatement(sql)) { //Creates a statement
            pstmt.setInt(1,id); //Sets the variable in the statement
            ResultSet rs = pstmt.executeQuery(); //Executes the query and stores the result

            //Stores the password and salt from the result into a String[] to return it
            res = new String[2];
            res[0] = rs.getString("PASSWORD");
            res[1] = rs.getString("SALT");
            rs.close();
            return res;
        } catch (Exception e) {
            Utility.alert("Something went wrong: couldn't retrieve data from database", Alert.AlertType.ERROR);
        }
        return res;
    }

    @Nullable //Gets the encryption/decryption salt and the initialization vecto from the db for the given user id
    public static String[] getSaltNIV(int id) {
        String[] res = null;
        String sql = "SELECT ROWID,ENC_SALT,IV FROM LOGIN WHERE ROWID=?";
        try (Connection conn = connect(); //Connects to the db
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //Creates a statement
            pstmt.setInt(1, id); //Sets the variable in the statement
            ResultSet rs = pstmt.executeQuery(); //Executes the query and stores the result

            //Stores the password and salt from the result into a String[] to return it
            res = new String[2];
            res[0] = rs.getString("ENC_SALT");
            res[1] = rs.getString("IV");
            rs.close();
            return res;
        } catch (Exception e) {
            Utility.alert("Something went wrong: couldn't retrieve data from database", Alert.AlertType.ERROR);
        }
        return res;
    }

    //Inserts the given data - the hashed password, hashing salt, encryption/decryption salt and initialization vector into the database
    public static boolean insert(byte[] hashedPassword, byte[] salt, byte[] encSalt, byte[] IV) {
        String sql = "INSERT INTO LOGIN(PASSWORD,SALT,ENC_SALT,IV) VALUES(?,?,?,?)";
        boolean ret = true;
        try (Connection conn = connect(); //Connects to the db
            PreparedStatement pstmt = conn.prepareStatement(sql)) { //Creates a statement
            Charset charset = StandardCharsets.UTF_16BE;
            pstmt.setString(1, charset.decode(ByteBuffer.wrap(hashedPassword)).toString()); //Sets the first variable in the statement

            //Deletes the password from memory
            for (byte b : hashedPassword)
                b = 0;

            //Sets the other variables in the statement
            pstmt.setString(2, charset.decode(ByteBuffer.wrap(salt)).toString());
            pstmt.setString(3, charset.decode(ByteBuffer.wrap(encSalt)).toString());
            pstmt.setString(4, charset.decode(ByteBuffer.wrap(IV)).toString());
            pstmt.executeUpdate(); //Executes the query
        } catch (SQLException e) {
            Utility.alert("Something went wrong: couldn't save your password", Alert.AlertType.ERROR);
            ret = false;
        }

        //Deletes the hashed password, hashing salt, encryption/decryption salt and the initialization vector from memory
        for (byte b: hashedPassword) {
            b = 0;
        }
        for (byte b: salt) {
            b = 0;
        }
        for (byte b: encSalt) {
            b = 0;
        }
        for (byte b: IV) {
            b = 0;
        }
        return ret;
    }

    //Closes the connection to the database when program closes
    public static void close() {
        try {
            c.close();
        } catch (SQLException ignored) {
        }
        System.exit(0);
    }

    //Creates the table to store all the values in the database if it doesn't exist already (it checks automatically) in the db
    public static void createTable() {
        try (Connection conn = connect(); //Connects to the db
            Statement stmt = conn.createStatement()){ //Creates a statement
            //Creates the scheme and table
            String sql = "CREATE TABLE IF NOT EXISTS LOGIN " +
                "(PASSWORD         TEXT    NOT NULL, " +
                "SALT             TEXT    NOT NULL, " +
                "ENC_SALT         TEXT    NOT NULL, " +
                "IV        		  TEXT    NOT NULL)";
            stmt.executeUpdate(sql); //Executes the query
        }catch (Exception e ) {
            Utility.swingAlert("Fatal error: couldn't create table!");
            System.exit(0);
        }
    }
}
