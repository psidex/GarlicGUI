package main.view;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import main.view.miner_api_client;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class mainController {

    @FXML
    ImageView garlic_image;
    @FXML
    TextField sgminer_path_textField, grlc_address_textField, pool_address_textField, pool_pword_textField;
    @FXML
    TextField sgminer_intesity_textField, sgminer_flags_textField;

    @FXML
    private void load_miner(ActionEvent event) throws IOException {
        // exe path, pool address, GRLC address, pool password, mining intensity, extra flags
        String sgminer_cmd = "%s/sgminer.exe --algorithm scrypt-n --nfactor 11 -o %s -u %s -p %s -I %s --api-listen --api-allow W:0/0 %s";

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
        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt.exec(to_execute);
        } catch(IOException e) {
            // TODO: Handle this better - https://stackoverflow.com/a/10431764
            System.out.println("to_execute threw error:\n" + e);
        }

        // Wait for sgminer to boot up
        try {
            // TODO: Deal with this differently (maybe keep retrying connection instead of waiting)
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            System.out.println("sleep failed?");
        }

        // TODO: Why this no work???
        miner_api_client miner_api = new miner_api_client();
        miner_api.startConnection("127.0.0.1", 4028);
        String resp = miner_api.sendMessage("{\"command\": \"summary\"}");
        System.out.println(resp);
        miner_api.stopConnection();
    }

    public void initialize() {
        // Currently doing nothing
    }

}
