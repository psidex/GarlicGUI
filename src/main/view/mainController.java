package main.view;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class mainController {

    @FXML
    TextField pool_textField;

    @FXML
    ImageView garlic_image;

    @FXML
    private void load_pool(ActionEvent event) {
        System.out.println(pool_textField.getText());
        // Start rotating symbol to show loading
        RotateTransition rt = new RotateTransition(Duration.millis(3000), garlic_image);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();
    }

    public void initialize() {
        // Currently doing nothing
    }

}
