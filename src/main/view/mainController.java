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
    VBox setup_vbox;
    @FXML
    ImageView garlic_image;
    @FXML
    RadioButton nvidia_RadioButton, amd_RadioButton;
    @FXML
    TextField grlc_address_textField, pool_address_textField, pool_pword_textField;
    @FXML
    TextField miner_path_textField, miner_intensity_textField, miner_flags_textField;
    @FXML
    Button go_button;

    @FXML
    VBox mining_vbox;
    @FXML
    Label amd_hashrate_avg_Label, amd_hashrate_5s_Label, nvidia_hashrate_Label, mining_on_Label, time_elapsed_Label;
    @FXML
    Label accepted_shares_Label, rejected_shares_Label;
    @FXML
    HBox amd_hashrate_avg_HBox, amd_hashrate_5s_HBox, nvidia_hashrate_HBox;
    @FXML
    Button miner_path_button;

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
        // Set chosen dir to textField
        miner_path_textField.setText(selectedFile.toString());
    }

    @FXML
    private void loadMiner(ActionEvent event){

        // Get all options
        String minerPath = miner_path_textField.getText().trim();
        String poolAddress = pool_address_textField.getText().trim();
        String poolPassword = pool_pword_textField.getText().trim();
        String GRLCAddress = grlc_address_textField.getText().trim();
        String minerIntensity = miner_intensity_textField.getText().trim();
        String minerFlags = miner_flags_textField.getText().trim();

        if (minerPath.isEmpty() || poolAddress.isEmpty() || GRLCAddress.isEmpty() || (!nvidia_RadioButton.isSelected() == !amd_RadioButton.isSelected())) {
            informationAlert.create("Information", "Required settings are empty", "All of: gpu selection, miner path, pool address, and GRLC address have to be filled out");
            return;
        }

        saveSettings();

        // Disable all input
        nvidia_RadioButton.setDisable(true);
        amd_RadioButton.setDisable(true);
        go_button.setDisable(true);
        miner_path_textField.setDisable(true);
        grlc_address_textField.setDisable(true);
        pool_address_textField.setDisable(true);
        pool_pword_textField.setDisable(true);
        miner_intensity_textField.setDisable(true);
        miner_flags_textField.setDisable(true);
        miner_path_button.setDisable(true);

        // exe path, pool address, GRLC address, mining intensity, pool password, extra flags
        String sgminer_cmd = "%s --algorithm scrypt-n --nfactor 11 -o %s -u %s -I %s -p %s --api-listen --api-allow W:127.0.0.1 %s";
        String ccminer_cmd = "%s --algo=scrypt:10 -o %s -u %s -i %s -p %s -listen -b 127.0.0.1:4028 %s";

        mining_on_Label.setText(poolAddress);

        String cmdToUse = "";
        if (nvidia_RadioButton.isSelected()) {
            cmdToUse = ccminer_cmd;
            mining_vbox.getChildren().remove(amd_hashrate_avg_HBox);
            mining_vbox.getChildren().remove(amd_hashrate_5s_HBox);
        }
        else if (amd_RadioButton.isSelected()){
            cmdToUse = sgminer_cmd;
            mining_vbox.getChildren().remove(nvidia_hashrate_HBox);
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
        String minerExecutable = new File(miner_path_textField.getText()).getName();
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
                mining_vbox.setVisible(true);

                // Get summary results from the API every second and update labels
                // For some reason the API only responds to 1 request, so a new connection has to be made for each api request
                // ToDo: Maybe implement dev api (gpu usage, temp, etc.)
                while (true) {
                    if (amd_RadioButton.isSelected()) amdUpdateInfo(minerSocket);
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
            time_elapsed_Label.setText(api_summary_jsonObject.get("Elapsed").toString() + "s");
            amd_hashrate_avg_Label.setText(api_summary_jsonObject.get("KHS av").toString() + " Kh/s");
            amd_hashrate_5s_Label.setText(api_summary_jsonObject.get("KHS 5s").toString() + " Kh/s");
            accepted_shares_Label.setText(api_summary_jsonObject.get("Accepted").toString());
            rejected_shares_Label.setText(api_summary_jsonObject.get("Rejected").toString());
        });
    }

    private void nvidiaUpdateInfo(socketObject miner_api) throws IOException {
        Map<String, String> api_summary_map = CCMinerAPI.pingInfo(miner_api);
        Platform.runLater(() -> {
            time_elapsed_Label.setText(api_summary_map.get("Uptime") + "s");
            nvidia_hashrate_Label.setText(api_summary_map.get("KHS") + " Kh/s");
            accepted_shares_Label.setText(api_summary_map.get("Accepted"));
            rejected_shares_Label.setText(api_summary_map.get("Rejected"));
        });
    }

    private void saveSettings() {
        // Save current settings to file using settings class
        Map<String, String> settingsObj = new HashMap<>();

        String gpu = "";
        if (nvidia_RadioButton.isSelected()) gpu = "nvidia";
        else if (amd_RadioButton.isSelected()) gpu = "amd";

        settingsObj.put("gpu_type", gpu);
        settingsObj.put("miner_path", miner_path_textField.getText().trim());
        settingsObj.put("grlc_address", grlc_address_textField.getText().trim());
        settingsObj.put("pool_address", pool_address_textField.getText().trim());
        settingsObj.put("miner_intensity", miner_intensity_textField.getText().trim());
        settingsObj.put("miner_flags", miner_flags_textField.getText().trim());
        settings.setSettings(settingsObj);
    }

    public void initialize() {
        // Setup transitions
        Integer transitionDuration = 2000;

        FadeTransition setup_vboxFT = new FadeTransition(Duration.millis(transitionDuration), setup_vbox);
        setup_vboxFT.setFromValue(1.0);
        setup_vboxFT.setToValue(0.0);
        TranslateTransition setup_vboxTT = new TranslateTransition(Duration.millis(transitionDuration), setup_vbox);
        setup_vboxTT.setFromX(0);
        setup_vboxTT.setToX(-300);
        setup_vboxPT = new ParallelTransition();
        setup_vboxPT.getChildren().addAll(
                setup_vboxFT,
                setup_vboxTT
        );

        FadeTransition mining_vboxFT = new FadeTransition(Duration.millis(transitionDuration), mining_vbox);
        mining_vboxFT.setFromValue(0.0);
        mining_vboxFT.setToValue(1.0);
        TranslateTransition mining_vboxTT = new TranslateTransition(Duration.millis(transitionDuration), mining_vbox);
        mining_vboxTT.setFromX(300);
        mining_vboxTT.setToX(0);
        mining_vboxPT = new ParallelTransition();
        mining_vboxPT.getChildren().addAll(
                mining_vboxFT,
                mining_vboxTT
        );

        garlic_imageRT = new RotateTransition(Duration.millis(3000), garlic_image);
        garlic_imageRT.setByAngle(360);
        garlic_imageRT.setCycleCount(Animation.INDEFINITE);
        garlic_imageRT.setInterpolator(Interpolator.LINEAR);

        // Radio buttons
        ToggleGroup GPUToggleGroup = new ToggleGroup();
        nvidia_RadioButton.setToggleGroup(GPUToggleGroup);
        amd_RadioButton.setToggleGroup(GPUToggleGroup);

        // Load all previous settings from file using settings class
        Map<String, String> settingsObj = settings.getSettings();

        String gpu = settingsObj.get("gpu_type");
        if (gpu.equals("nvidia")) GPUToggleGroup.selectToggle(nvidia_RadioButton);
        else if (gpu.equals("amd")) GPUToggleGroup.selectToggle(amd_RadioButton);

        miner_path_textField.setText(settingsObj.get("miner_path"));
        grlc_address_textField.setText(settingsObj.get("grlc_address"));
        pool_address_textField.setText(settingsObj.get("pool_address"));
        miner_intensity_textField.setText(settingsObj.get("miner_intensity"));
        miner_flags_textField.setText(settingsObj.get("miner_flags"));
    }

}
