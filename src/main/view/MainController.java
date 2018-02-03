package main.view;

import org.json.JSONObject;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.util.Duration;

public class MainController {

    private PrintWriter logWriter;
    private ParallelTransition setupVBBoxPTInLeft, setupVBBoxPTOutLeft;
    private ParallelTransition miningVBoxPTInRight;
    private ParallelTransition loggingConfigVBoxPTInRight, loggingConfigVBoxPTOutRight;
    private RotateTransition garlicImageRT;
    private boolean minerLoggingChecked, GarlicGUILoggingChecked;

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
    Hyperlink helpLink;
    @FXML
    Button goButton, configLogsButton;

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
    VBox loggingConfigVBox;
    @FXML
    CheckBox GarlicGUILoggingCheckBox, minerLoggingCheckBox;

    public void initialize() {
        // Focus on the only visible VBox
        setupVBox.toFront();

        // Setup transitions
        setupVBBoxPTInLeft = Fade.createFadeInLeft(setupVBox);
        setupVBBoxPTOutLeft = Fade.createFadeOutLeft(setupVBox);
        miningVBoxPTInRight = Fade.createFadeInRight(miningVBox);
        loggingConfigVBoxPTInRight = Fade.createFadeInRight(loggingConfigVBox);
        loggingConfigVBoxPTOutRight = Fade.createFadeOutRight(loggingConfigVBox);

        garlicImageRT = new RotateTransition(Duration.millis(3000), garlicImg);
        garlicImageRT.setByAngle(360);
        garlicImageRT.setCycleCount(Animation.INDEFINITE);
        garlicImageRT.setInterpolator(Interpolator.LINEAR);

        // Radio buttons
        ToggleGroup GPUToggleGroup = new ToggleGroup();
        NvidiaRadioButton.setToggleGroup(GPUToggleGroup);
        AMDRadioButton.setToggleGroup(GPUToggleGroup);

        // Load all previous Settings from file using Settings class
        Map<String, String> settingsObj = Settings.getSettings();
        logText("Serialized Settings map loaded");

        String gpu = settingsObj.get("GPUType");
        if (gpu.equals("nvidia")) GPUToggleGroup.selectToggle(NvidiaRadioButton);
        else GPUToggleGroup.selectToggle(AMDRadioButton);

        minerPathTextField.setText(settingsObj.get("minerPath"));
        GRLCAddressTextField.setText(settingsObj.get("GRLCAddress"));
        poolAddressTextField.setText(settingsObj.get("poolAddress"));
        minerIntensityTextField.setText(settingsObj.get("minerIntensity"));
        minerFlagsTextField.setText(settingsObj.get("minerFlags"));

        setupLogging();
    }

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
    private void loadMiner(){
        // Get all options
        String minerPath = minerPathTextField.getText().trim();
        String poolAddress = poolAddressTextField.getText().trim();
        String poolPassword = poolPwordTextField.getText().trim();
        String GRLCAddress = GRLCAddressTextField.getText().trim();
        String minerIntensity = minerIntensityTextField.getText().trim();
        String minerFlags = minerFlagsTextField.getText().trim();

        if (minerPath.isEmpty() || poolAddress.isEmpty() || GRLCAddress.isEmpty() || (!NvidiaRadioButton.isSelected() == !AMDRadioButton.isSelected())) {
            InformationAlert.create("Information", "Required Settings are empty", "All of: gpu selection, miner path, pool address, and GRLC address have to be filled out");
            return;
        }

        saveSettings();
        logText("Serialized Settings map saved");

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
        configLogsButton.setDisable(true);

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
        logText("Executing: " + to_execute);

        // Start rotating symbol to show loading
        garlicImageRT.play();

        // Get miner name from path
        String minerExecutable = new File(minerPathTextField.getText()).getName();
        // Thread for running miner executable
        Runnable minerCMDThread = new CMDThread(to_execute, minerExecutable, "minerCMDThread.log", minerLoggingChecked);
        new Thread(minerCMDThread).start();
        logText("minerCMDThread started");

        // Thread for running API requests & updating GUI with results
        new Thread(() -> {
            logText("miner API thread started");

            // Keep attempting API connection until successful
            Integer attemptCount = 0;
            try {
                SocketObject minerSocket = new SocketObject();

                while (true) {
                    try {
                        minerSocket.startConnection("127.0.0.1", 4028);
                    } catch(IOException e) {
                        if (attemptCount == 50) {
                            InformationAlert.create(
                                    "Information",
                                    "Attempted miner connection 50 times",
                                    "Are you sure the miner works on your machine normally?"
                            );
                        }
                        attemptCount++;
                        continue;
                    }
                    logText("Connected to API");
                    minerSocket.stopConnection();
                    break;
                }

                // Run transitions
                Platform.runLater(() -> {
                    setupVBBoxPTOutLeft.play();
                    miningVBoxPTInRight.play();
                    miningVBox.toFront();
                });

                // Get summary results from the API every second and update labels
                // ToDo: Implement dev api (gpu usage, temp, etc.)
                while (true) {
                    if (AMDRadioButton.isSelected()) amdUpdateInfo(minerSocket);
                    else nvidiaUpdateInfo(minerSocket);
                }

            } catch (IOException e) {
                StacktraceAlert.create("Exception occurred", "Error in miner_api_thread", "Exception in miner_api_thread", e);
            }
        }).start();
    }
    @FXML
    private void stopMiner() {
        logText("stopMiner() called, exiting");
        Platform.exit();
        System.exit(0);
    }

    private void amdUpdateInfo(SocketObject miner_api) throws IOException {
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
    private void nvidiaUpdateInfo(SocketObject miner_api) throws IOException {
        Map<String, String> api_summary_map = CCMinerAPI.pingInfo(miner_api);
        Platform.runLater(() -> {
            timeElapsedLabel.setText(api_summary_map.get("Uptime") + "s");
            NvidiaHashrateLabel.setText(api_summary_map.get("KHS") + " Kh/s");
            acceptedSharesLabel.setText(api_summary_map.get("Accepted"));
            rejectedSharesLabel.setText(api_summary_map.get("Rejected"));
        });
    }

    private void saveSettings() {
        // Save current Settings to file using Settings class
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
        settingsObj.put("GarlicGUILogging", String.valueOf(GarlicGUILoggingCheckBox.isSelected()));
        settingsObj.put("minerLogging", String.valueOf(minerLoggingCheckBox.isSelected()));
        Settings.setSettings(settingsObj);
    }

    private void logText(String text) {
        if (GarlicGUILoggingChecked) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            logWriter.println("[" + dateFormat.format(new Date()) + "] " + text);
        }
    }
    private void setupLogging() {
        // Pull fresh settings from file and setup logging variables
        Map<String, String> settingsObj = Settings.getSettings();
        GarlicGUILoggingChecked = Boolean.parseBoolean(settingsObj.get("GarlicGUILogging"));
        minerLoggingChecked = Boolean.parseBoolean(settingsObj.get("minerLogging"));
        GarlicGUILoggingCheckBox.setSelected(GarlicGUILoggingChecked);
        minerLoggingCheckBox.setSelected(minerLoggingChecked);

        if (GarlicGUILoggingChecked) {
            // Setup logging to file
            try {
                logWriter = new PrintWriter("GarlicGUI.log", "UTF-8");
            } catch (IOException e) {
                StacktraceAlert.create(
                        "Log file error",
                        "Cannot create new PrintWriter to GarlicGUI.log",
                        "MainController.initialize threw IOException",
                        e
                );
            }
            // Close log file when program closed
            Runtime.getRuntime().addShutdownHook(new Thread(() -> logWriter.close()));
        }
    }
    @FXML
    private void openLogConfig() {
        setupVBBoxPTOutLeft.play();
        loggingConfigVBoxPTInRight.play();
        loggingConfigVBox.toFront();
    }
    @FXML void backToSetup() {
        // When leaving logging config screen, re-save settings then update all logging vars
        saveSettings();
        setupLogging();
        setupVBBoxPTInLeft.play();
        loggingConfigVBoxPTOutRight.play();
        setupVBox.toFront();
    }

    @FXML
    private void openHelp() {
        try {
            URL url = new URL("https://github.com/thatguywiththatname/GarlicGUI/blob/master/README.md");
            URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);
            Desktop.getDesktop().browse(uri);
        } catch (URISyntaxException | IOException e) {
            StacktraceAlert.create(
                    "Help error",
                    "Cannot open help URL",
                    "openHelp threw URISyntaxException or IOException",
                    e,
                    false
            );
        }
    }

}
