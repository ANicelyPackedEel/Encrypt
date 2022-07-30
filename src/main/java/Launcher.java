//A launcher class simply to launch the program from a class that doesn't extend Application (unlike main) in order to be able to execute the jar file and the exe made out of it on any system
//without the need to add to the JVM arguments the path of the JavaFX modules (and without the need to download them) on the computer running this program.
public class Launcher {
    public static void main(String[] args){
        Main.main(args);
    }
}