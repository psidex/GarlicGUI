package main.view;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.util.Duration;

public class mainController {

    private ParallelTransition setup_vboxPT, mining_vboxPT;
    private RotateTransition garlic_imageRT;

    @FXML
    VBox setupVBox;
    @FXML
    ImageView garlicImg;
    @FXML
    RadioButton NvidiaRadioButton, AMDRadioButton;
    @FXML
    TextField GRLCAddressTextField, poolAddressTextField, poolPwordTextField;
    @FXML
    TextField minerPathTextField, minerIntensityTextField, minerFlagsTextField;
    @FXML
    Button goButton;

    @FXML
    VBox miningVBox;
    @FXML
    Label AMDHashrateAvgLabel, AMDHashrate5sLabel, NvidiaHashrateLabel, miningOnLabel, timeElapsedLabel;
    @FXML
    Label acceptedSharesLabel, rejectedSharesLabel;
    @FXML
    HBox AMDHashrateAvgHBox, AMDHashrate5sHBox, NvidiaHashrateHBox;
    @FXML
    Button minerPathButton;

    @FXML
    private void findMinerPath(ActionEvent event) {
        // Grab stage from event
        Node source = (Node) event.getSource();
        Window thisStage = source.getScene().getWindow();
        // Setup and show directory chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("miner executable");
        fileChooser.setInitialDirectory(new File("C:/"));
        File selectedFile = fileChooser.showOpenDialog(thisStage);
        if (selectedFile == null) return;
        // Set chosen dir to textField
        minerPathTextField.setText(selectedFile.toString());
    }

    @FXML
    private void loadMiner(ActionEvent event){

        // Get all options
        String minerPath = minerPathTextField.getText().trim();
        String poolAddress = poolAddressTextField.getText().trim();
        String poolPassword = poolPwordTextField.getText().trim();
        String GRLCAddress = GRLCAddressTextField.getText().trim();
        String minerIntensity = minerIntensityTextField.getText().trim();
        String minerFlags = minerFlagsTextField.getText().trim();

        if (minerPath.isEmpty() || poolAddress.isEmpty() || GRLCAddress.isEmpty() || (!NvidiaRadioButton.isSelected() == !AMDRadioButton.isSelected())) {
            informationAlert.create("Information", "Required settings are empty", "All of: gpu selection, miner path, pool address, and GRLC address have to be filled out");
            return;
        }

        saveSettings();

        // Disable all input
        NvidiaRadioButton.setDisable(true);
        AMDRadioButton.setDisable(true);
        goButton.setDisable(true);
        minerPathTextField.setDisable(true);
        GRLCAddressTextField.setDisable(true);
        poolAddressTextField.setDisable(true);
        poolPwordTextField.setDisable(true);
        minerIntensityTextField.setDisable(true);
        minerFlagsTextField.setDisable(true);
        minerPathButton.setDisable(true);

        // exe path, pool address, GRLC address, mining intensity, pool password, extra flags
        String sgminer_cmd = "%s --algorithm scrypt-n --nfactor 11 -o %s -u %s -I %s -p %s --api-listen --api-allow W:127.0.0.1 %s";
        String ccminer_cmd = "%s --algo=scrypt:10 -o %s -u %s -i %s -p %s -listen -b 127.0.0.1:4028 %s";

        miningOnLabel.setText(poolAddress);

        String cmdToUse = "";
        if (NvidiaRadioButton.isSelected()) {
            cmdToUse = ccminer_cmd;
            miningVBox.getChildren().remove(AMDHashrateAvgHBox);
            miningVBox.getChildren().remove(AMDHashrate5sHBox);
        }
        else if (AMDRadioButton.isSelected()){
            cmdToUse = sgminer_cmd;
            miningVBox.getChildren().remove(NvidiaHashrateHBox);
        }
        // Check for blank radio button has already happened

        // Construct command
        String to_execute = String.format(
                cmdToUse,
                minerPath,
                poolAddress,
                GRLCAddress,
                minerIntensity.isEmpty() ? "12" : minerIntensity,
                poolPassword.isEmpty() ? "x" : poolPassword,
                minerFlags
        );
        System.out.println("Executing: " + to_execute);

        // Start rotating symbol to show loading
        garlic_imageRT.play();

        // Get miner name from path
        String minerExecutable = new File(minerPathTextField.getText()).getName();
        // Thread for running miner executable
        Runnable minerCMDThread = new cmdThread(to_execute, minerExecutable, "minerCMDThread.log");
        new Thread(minerCMDThread).start();

        // Thread for running API requests & updating GUI with results
        new Thread(() -> {
            System.out.println("miner_api_thread started");

            // Keep attempting API connection until successful
            Integer attemptCount = 0;
            try {
                socketObject minerSocket = new socketObject();

                while (true) {
                    try {
                        minerSocket.startConnection("127.0.0.1", 4028);
                    } catch(IOException e) {
                        if (attemptCount == 50) {
                            informationAlert.create(
                                    "Information",
                                    "Attempted miner connection 50 times",
                                    "Are you sure the miner works on your machine normally?"
                            );
                        }
                        attemptCount++;
                        continue;
                    }
                    System.out.println("Connected to API");
                    minerSocket.stopConnection();
                    break;
                }

                // Run transitions
                setup_vboxPT.play();
                mining_vboxPT.play();
                miningVBox.setVisible(true);

                // Get summary results from the API every second and update labels
                // For some reason the API only responds to 1 request, so a new connection has to be made for each api request
                // ToDo: Maybe implement dev api (gpu usage, temp, etc.)
                while (true) {
                    if (AMDRadioButton.isSelected()) amdUpdateInfo(minerSocket);
                    else nvidiaUpdateInfo(minerSocket);
                }

            } catch (IOException e) {
                stacktraceAlert.create("Exception occurred", "Error in miner_api_thread", "Exception in miner_api_thread", e);
            }
        }).start();
    }

    @FXML
    private void stopMiner(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    private void amdUpdateInfo(socketObject miner_api) throws IOException {
        JSONObject api_summary_jsonObject = SGMinerAPI.pingInfo(miner_api);
        // runLater() allows updating GUI from inside another Thread
        Platform.runLater(() -> {
            timeElapsedLabel.setText(api_summary_jsonObject.get("Elapsed").toString() + "s");
            AMDHashrateAvgLabel.setText(api_summary_jsonObject.get("KHS av").toString() + " Kh/s");
            AMDHashrate5sLabel.setText(api_summary_jsonObject.get("KHS 5s").toString() + " Kh/s");
            acceptedSharesLabel.setText(api_summary_jsonObject.get("Accepted").toString());
            rejectedSharesLabel.setText(api_summary_jsonObject.get("Rejected").toString());
        });
    }

    private void nvidiaUpdateInfo(socketObject miner_api) throws IOException {
        Map<String, String> api_summary_map = CCMinerAPI.pingInfo(miner_api);
        Platform.runLater(() -> {
            timeElapsedLabel.setText(api_summary_map.get("Uptime") + "s");
            NvidiaHashrateLabel.setText(api_summary_map.get("KHS") + " Kh/s");
            acceptedSharesLabel.setText(api_summary_map.get("Accepted"));
            rejectedSharesLabel.setText(api_summary_map.get("Rejected"));
        });
    }

    private void saveSettings() {
        // Save current settings to file using settings class
        Map<String, String> settingsObj = new HashMap<>();

        String gpu;
        if (NvidiaRadioButton.isSelected()) gpu = "nvidia";
        else gpu = "amd";

        settingsObj.put("GPUType", gpu);
        settingsObj.put("minerPath", minerPathTextField.getText().trim());
        settingsObj.put("GRLCAddress", GRLCAddressTextField.getText().trim());
        settingsObj.put("poolAddress", poolAddressTextField.getText().trim());
        settingsObj.put("minerIntensity", minerIntensityTextField.getText().trim());
        settingsObj.put("minerFlags", minerFlagsTextField.getText().trim());
        settings.setSettings(settingsObj);
    }

    public void initialize() {
        // Setup transitions
        Integer transitionDuration = 2000;

        FadeTransition setup_vboxFT = new FadeTransition(Duration.millis(transitionDuration), setupVBox);
        setup_vboxFT.setFromValue(1.0);
        setup_vboxFT.setToValue(0.0);
        TranslateTransition setup_vboxTT = new TranslateTransition(Duration.millis(transitionDuration), setupVBox);
        setup_vboxTT.setFromX(0);
        setup_vboxTT.setToX(-300);
        setup_vboxPT = new ParallelTransition();
        setup_vboxPT.getChildren().addAll(
                setup_vboxFT,
                setup_vboxTT
        );

        FadeTransition mining_vboxFT = new FadeTransition(Duration.millis(transitionDuration), miningVBox);
        mining_vboxFT.setFromValue(0.0);
        mining_vboxFT.setToValue(1.0);
        TranslateTransition mining_vboxTT = new TranslateTransition(Duration.millis(transitionDuration), miningVBox);
        mining_vboxTT.setFromX(300);
        mining_vboxTT.setToX(0);
        mining_vboxPT = new ParallelTransition();
        mining_vboxPT.getChildren().addAll(
                mining_vboxFT,
                mining_vboxTT
        );

        garlic_imageRT = new RotateTransition(Duration.millis(3000), garlicImg);
        garlic_imageRT.setByAngle(360);
        garlic_imageRT.setCycleCount(Animation.INDEFINITE);
        garlic_imageRT.setInterpolator(Interpolator.LINEAR);

        // Radio buttons
        ToggleGroup GPUToggleGroup = new ToggleGroup();
        NvidiaRadioButton.setToggleGroup(GPUToggleGroup);
        AMDRadioButton.setToggleGroup(GPUToggleGroup);

        // Load all previous settings from file using settings class
        Map<String, String> settingsObj = settings.getSettings();

        String gpu = settingsObj.get("GPUType");
        if (gpu.equals("nvidia")) GPUToggleGroup.selectToggle(NvidiaRadioButton);
        else GPUToggleGroup.selectToggle(AMDRadioButton);

        minerPathTextField.setText(settingsObj.get("minerPath"));
        GRLCAddressTextField.setText(settingsObj.get("GRLCAddress"));
        poolAddressTextField.setText(settingsObj.get("poolAddress"));
        minerIntensityTextField.setText(settingsObj.get("minerIntensity"));
        minerFlagsTextField.setText(settingsObj.get("minerFlags"));
    }

}
