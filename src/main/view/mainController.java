package main.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;

public class mainController {

    @FXML
    public void exitApplication(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    ImageView garlic_image;
    @FXML
    TextField sgminer_path_textField, grlc_address_textField, pool_address_textField, pool_pword_textField;
    @FXML
    TextField sgminer_intesity_textField, sgminer_flags_textField;
    @FXML
    Button go_button;

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

        // Run sgminer
        Thread sgminer_cmd_thread = new Thread(){
            public void run(){
                System.out.println("sgminer_cmd_thread started");

                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", to_execute);
                builder.redirectErrorStream(true);

                try {
                    Process p = builder.start();

                    Runtime.getRuntime().addShutdownHook(new Thread(){
                        public void run() {
                            // Kill process
                            p.destroy();
                            try {
                                // Kill sgminer (not always killed by destroy()
                                Runtime.getRuntime().exec("taskkill /f /t /im sgminer.exe");
                            } catch (IOException e) {
                                // Do nothing
                            }
                        }
                    });

                    while (true) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        // System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("sgminer proc failed (?)");
                }
            }
        };
        sgminer_cmd_thread.start();

        // Run API thread
        Thread sgminer_api_thread = new Thread(){
            public void run(){
                try {
                    System.out.println("sgminer_api_thread started");
                    miner_api_client miner_api = new miner_api_client();
                    while ( 1==1 ) {
                        try {
                            miner_api.startConnection("127.0.0.1", 4028);
                        } catch(IOException e) {
                            System.out.println("sgminer_api_thread connection failed: " + e);
                            continue;
                        }
                        System.out.println("Connected");
                        break;
                    }
                    String resp = miner_api.sendMessage("{\"command\": \"summary\"}");
                    System.out.println(resp);
                    miner_api.stopConnection();
                } catch (IOException e) {
                    System.out.println("Caught IOException in sgminer_api_thread: " + e);
                }
            }
        };
        sgminer_api_thread.start();
    }

    public void initialize() {
        // Currently doing nothing
    }

}
