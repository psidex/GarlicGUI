package main.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;

public class mainController {

    @FXML
    VBox setup_vbox;
    @FXML
    ImageView garlic_image;
    @FXML
    TextField sgminer_path_textField, grlc_address_textField, pool_address_textField, pool_pword_textField;
    @FXML
    TextField sgminer_intesity_textField, sgminer_flags_textField;
    @FXML
    Button go_button;

    @FXML
    VBox mining_vbox;
    @FXML
    Label hashrate_avg_Label;
    @FXML
    Label hashrate_5s_Label;

    @FXML
    private void load_miner(ActionEvent event){
        go_button.setDisable(true);
        sgminer_path_textField.setDisable(true);
        grlc_address_textField.setDisable(true);
        pool_address_textField.setDisable(true);
        pool_pword_textField.setDisable(true);
        sgminer_intesity_textField.setDisable(true);
        sgminer_flags_textField.setDisable(true);

        // exe path, pool address, GRLC address, pool password, mining intensity, extra flags
        String sgminer_cmd = "%s/sgminer --gpu-platform 0 --algorithm scrypt-n --nfactor 11 -o %s -u %s -p %s -I %s --api-listen --api-allow W:127.0.0.1 --thread-concurrency 8193 %s";

        // Get all options
        String sgminer_path = sgminer_path_textField.getText().trim();
        String pool_address = pool_address_textField.getText().trim();
        String grlc_address = grlc_address_textField.getText().trim();
        String pool_pword = pool_pword_textField.getText().trim();
        String sgminer_intesity = sgminer_intesity_textField.getText().trim();
        String sgminer_flags = sgminer_flags_textField.getText().trim();

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
        RotateTransition rot = new RotateTransition(Duration.millis(3000), garlic_image);
        rot.setByAngle(360);
        rot.setCycleCount(Animation.INDEFINITE);
        rot.setInterpolator(Interpolator.LINEAR);
        rot.play();

        // Thread for running sgminer.exe (maybe move to class?)
        new Thread(){
            public void run(){
                System.out.println("sgminer_cmd_thread started");

                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", to_execute);
                builder.redirectErrorStream(true);

                try {
                    Process p = builder.start();

                    Runtime.getRuntime().addShutdownHook(new Thread(){
                        public void run() {
                            // Kill CMD process
                            p.destroy();
                            try {
                                // Kill sgminer.exe (the CMD processes child)
                                Runtime.getRuntime().exec("taskkill /f /t /im sgminer.exe");
                            } catch (IOException e) {
                                // Do nothing
                            }
                        }
                    });

                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (true) {
                        // Read lines from output and make sure everything is still working
                        // This is needed for it to work
                        if (r.readLine() == null) break;
                    }
                } catch (IOException e) {
                    new stacktrace_alert().create("IOException", "Error in sgminer.exe thread", "IOException in sgminer.exe thread", e);
                }
            }
        }.start();

        // Thread for running API requests (maybe move to class?)
        new Thread(){
            public void run() {
                System.out.println("sgminer_api_thread started");

                try {
                    miner_api_client miner_api = new miner_api_client();

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

                            Platform.runLater(new Runnable(){
                                public void run(){
                                    hashrate_avg_Label.setText(api_summary_jsonObject.get("KHS av").toString());
                                    hashrate_5s_Label.setText(api_summary_jsonObject.get("KHS 5s").toString());
                                }
                            });
                        }

                        miner_api.stopConnection();
                    }

                } catch (IOException e) {
                    new stacktrace_alert().create("Exception occurred", "Error in sgminer_api_thread", "Exception in sgminer_api_thread", e);
                }
            }
        }.start();
    }

    public void initialize() {
        // Currently doing nothing
    }

}
