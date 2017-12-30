import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

public class ServerController extends Application implements Initializable {

    @FXML public Label errorLabel;
    @FXML public Button startButton, stopButton;
    @FXML public TextArea resultArea;
    @FXML public TextField portField;
    private TankServer server;
    private int portNum;

    @Override
    public void initialize(URL location, ResourceBundle resources){
        errorLabel.setVisible(false);
        stopButton.setDisable(true);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("ServerGUI.fxml"));
        Scene server = new Scene(root, 523, 328);
        stage.setTitle("Server");
        stage.setScene(server);
        stage.show();
    }

    @FXML public void startClicked(){
        try{
            portNum = Integer.parseInt(portField.getText());
            if (portNum < 1024 || portNum > 49151)
                errorLabel.setVisible(true);
            else
                errorLabel.setVisible(false);
        }
        catch(Exception e){
            errorLabel.setVisible(true);
        }

        if(!errorLabel.isVisible()){
            server = new TankServer(portNum, this);
            stopButton.setDisable(false);
            startButton.setDisable(true);
        }
    }

    @FXML public void stopClicked(){
        if (server.getClientSize() > 0){
            server.endServer();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            resultArea.appendText("------------------------------------\n");
        }
        else{
            resultArea.appendText("Must have a client connected to stop server.\n");
        }
        //server = new TankServer(portNum, this);
    }

    public void addMessage(String m){
        resultArea.appendText(m + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}