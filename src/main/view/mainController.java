package main.view;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;

public class mainController {

    @FXML
    VBox setup_vbox;
    @FXML
    ImageView garlic_image;
    @FXML
    TextField grlc_address_textField, pool_address_textField, pool_pword_textField;
    @FXML
    TextField sgminer_path_textField, sgminer_intensity_textField, sgminer_flags_textField;
    @FXML
    Button go_button;

    @FXML
    VBox mining_vbox;
    @FXML
    Label hashrate_avg_Label, hashrate_5s_Label, mining_on_Label, time_elapsed_Label;
    @FXML
    Label accepted_shares_Label, rejected_shares_Label;
    @FXML
    Button sgminer_path_button;

    @FXML
    private void findSGMinerPath(ActionEvent event) {
        // Grab stage from event
        Node source = (Node) event.getSource();
        Window thisStage = source.getScene().getWindow();
        // Setup and show directory chooser
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("SWGMiner.exe location");
        File defaultDirectory = new File("C:/");
        chooser.setInitialDirectory(defaultDirectory);
        File selectedDirectory = chooser.showDialog(thisStage);
        // Set chosen dir to textField
        sgminer_path_textField.setText(selectedDirectory.toString());
    }

    @FXML
    private void loadMiner(ActionEvent event){
        go_button.setDisable(true);
        sgminer_path_textField.setDisable(true);
        grlc_address_textField.setDisable(true);
        pool_address_textField.setDisable(true);
        pool_pword_textField.setDisable(true);
        sgminer_intensity_textField.setDisable(true);
        sgminer_flags_textField.setDisable(true);
        sgminer_path_button.setDisable(true);

        // exe path, pool address, GRLC address, pool password, mining intensity, extra flags
        String sgminer_cmd = "%s\\sgminer --algorithm scrypt-n --nfactor 11 -o %s -u %s -p %s -I %s --api-listen --api-allow W:127.0.0.1 --thread-concurrency 8193 %s";

        // Get all options
        String sgminer_path = sgminer_path_textField.getText().trim();
        String pool_address = pool_address_textField.getText().trim();
        String grlc_address = grlc_address_textField.getText().trim();
        String pool_pword = pool_pword_textField.getText().trim();
        String sgminer_intesity = sgminer_intensity_textField.getText().trim();
        String sgminer_flags = sgminer_flags_textField.getText().trim();

        // Will be seen when frame changes to active mining (TODO: find a better way of doing default values (or don't use them at all)
        mining_on_Label.setText(pool_address.isEmpty() ? "stratum+tcp://freshgarlicblocks.net:3032" : pool_address);

        // Construct command
        String to_execute = String.format(
                sgminer_cmd,
                sgminer_path.isEmpty() ? "C:\\Users\\Simon\\Desktop\\sgminer" : sgminer_path,
                pool_address.isEmpty() ? "stratum+tcp://freshgarlicblocks.net:3032" : pool_address,
                grlc_address.isEmpty() ? "GJbKUzCbAezNZuQJkahqptvT2CpYywMSFj" : grlc_address,
                pool_pword.isEmpty() ? "x" : pool_pword,
                sgminer_intesity.isEmpty() ? "12" : sgminer_intesity,
                sgminer_flags
        );
        System.out.println("Executing: " + to_execute);

        // Start rotating symbol to show loading
        RotateTransition garlic_image_rot = new RotateTransition(Duration.millis(3000), garlic_image);
        garlic_image_rot.setByAngle(360);
        garlic_image_rot.setCycleCount(Animation.INDEFINITE);
        garlic_image_rot.setInterpolator(Interpolator.LINEAR);
        garlic_image_rot.play();

        // Thread for running sgminer.exe
        Runnable sgminerCMDThread = new cmdThread(to_execute);
        new Thread(sgminerCMDThread).start();

        // Thread for running API requests & updating GUI with results
        new Thread(() -> {
            System.out.println("sgminer_api_thread started");

            try {
                socketObject miner_api = new socketObject();

                while (true) {
                    try {
                        miner_api.startConnection("127.0.0.1", 4028);
                    } catch(IOException e) {
                        // TODO: Add a limit to the amount of times it can attempt connection before considering it has failed
                        continue;
                    }
                    System.out.println("Connected to API");
                    miner_api.stopConnection();
                    break;
                }

                setup_vbox.setVisible(false);
                mining_vbox.setVisible(true);

                // Get summary results from the API every second and update labels
                // For some reason the API only responds to 1 request, so a new connection has to be made for each api request
                while (true) {
                    miner_api.startConnection("127.0.0.1", 4028);

                    // TODO: check for memory leak - defining this stuff again and again might break things?
                    String resp = miner_api.sendMessage("{\"command\": \"summary\"}");
                    JSONObject api_return = new JSONObject(resp);
                    JSONArray api_summary_array = (JSONArray) api_return.get("SUMMARY");
                    Iterator api_summary_itr = api_summary_array.iterator();

                    // Should only over iterate once
                    while (api_summary_itr.hasNext()) {
                        Object slide = api_summary_itr.next();
                        JSONObject api_summary_jsonObject = (JSONObject) slide;

                        // runLater() allows updating GUI from inside another Thread
                        Platform.runLater(() -> {
                            time_elapsed_Label.setText(api_summary_jsonObject.get("Elapsed").toString() + "s");
                            hashrate_avg_Label.setText(api_summary_jsonObject.get("KHS av").toString() + " Kh/s");
                            hashrate_5s_Label.setText(api_summary_jsonObject.get("KHS 5s").toString() + " Kh/s");
                            accepted_shares_Label.setText(api_summary_jsonObject.get("Accepted").toString());
                            rejected_shares_Label.setText(api_summary_jsonObject.get("Rejected").toString());
                        });
                    }

                    miner_api.stopConnection();
                }

            } catch (IOException e) {
                new stacktraceAlert().create("Exception occurred", "Error in sgminer_api_thread", "Exception in sgminer_api_thread", e);
            }
        }).start();
    }

    @FXML
    private void stopMiner(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    public void initialize() {
        // Currently doing nothing
    }

}
