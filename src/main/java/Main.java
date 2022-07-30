import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

//Main class, starts the program and initializes the scene controllers
public class Main extends Application{ 
	
	public void start(@NotNull Stage stage) throws Exception{
        //Load the fxml files of the scenes
        FXMLLoader loader1 = new FXMLLoader(MFXDemoResourcesLoader.loadURL("EncryptScene.fxml"));
        Parent view1 = loader1.load();
//      FXMLLoader loader1 = new FXMLLoader(getClass().getResource("EncryptScene.fxml"));
//      Parent view1 = loader1.load();
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("CreateAccountScene.fxml"));
        Parent view2 = loader2.load();

        //Loading the corresponding controllers of each fxml file
        EncSceneController controller1 = loader1.getController();
        CreateAccSceneController controller2 = loader2.getController();

        //Create a scene
        Scene scene = new Scene(view1);

        //Sets the functions to switch between the two scenes
        controller1.setOnNext(() -> scene.setRoot(view2));
        controller2.setOnBack(() -> scene.setRoot(view1));

        //Starts and shows the program
        stage.setTitle("Encrypt");
        stage.setScene(scene);
        stage.show();

        //Sets function to run when pressing the X button to close the program
        stage.setOnCloseRequest(we -> Utility.close());
    }
	
	public static void main(String[] args) {
        Crypt.init(); //Initializes the Crypt class
		launch(args); //Launches the application
    }
}