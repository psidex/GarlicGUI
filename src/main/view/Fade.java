package main.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class Fade {

    private static Integer transitionDuration = 400;

    public static ParallelTransition createFadeOutLeft(VBox VBoxToFade) {
        // Will become less opaque and translate from the centre towards the left side
        FadeTransition newFadeTrans = new FadeTransition(Duration.millis(transitionDuration), VBoxToFade);
        newFadeTrans.setFromValue(1.0);
        newFadeTrans.setToValue(0.0);
        TranslateTransition newTranslateTrans = new TranslateTransition(Duration.millis(transitionDuration), VBoxToFade);
        newTranslateTrans.setFromX(0);
        newTranslateTrans.setToX(-300);
        ParallelTransition returnPT = new ParallelTransition();
        returnPT.getChildren().addAll(
                newFadeTrans,
                newTranslateTrans
        );
        return returnPT;
    }

    public static ParallelTransition createFadeOutRight(VBox VBoxToFade) {
        FadeTransition newFadeTrans = new FadeTransition(Duration.millis(transitionDuration), VBoxToFade);
        newFadeTrans.setFromValue(1.0);
        newFadeTrans.setToValue(0.0);
        TranslateTransition newTranslateTrans = new TranslateTransition(Duration.millis(transitionDuration), VBoxToFade);
        newTranslateTrans.setFromX(0);
        newTranslateTrans.setToX(300);
        ParallelTransition returnPT = new ParallelTransition();
        returnPT.getChildren().addAll(
                newFadeTrans,
                newTranslateTrans
        );
        return returnPT;
    }

    public static ParallelTransition createFadeInRight(VBox VBoxToFade) {
        // Will become more opaque and translate towards the centre from the right side
        FadeTransition newFadeTrans = new FadeTransition(Duration.millis(transitionDuration), VBoxToFade);
        newFadeTrans.setFromValue(0.0);
        newFadeTrans.setToValue(1.0);
        TranslateTransition newTranslateTrans = new TranslateTransition(Duration.millis(transitionDuration), VBoxToFade);
        newTranslateTrans.setFromX(300);
        newTranslateTrans.setToX(0);
        ParallelTransition returnPT = new ParallelTransition();
        returnPT.getChildren().addAll(
                newFadeTrans,
                newTranslateTrans
        );
        return returnPT;
    }

    public static ParallelTransition createFadeInLeft(VBox VBoxToFade) {
        FadeTransition newFadeTrans = new FadeTransition(Duration.millis(transitionDuration), VBoxToFade);
        newFadeTrans.setFromValue(0.0);
        newFadeTrans.setToValue(1.0);
        TranslateTransition newTranslateTrans = new TranslateTransition(Duration.millis(transitionDuration), VBoxToFade);
        newTranslateTrans.setFromX(-300);
        newTranslateTrans.setToX(0);
        ParallelTransition returnPT = new ParallelTransition();
        returnPT.getChildren().addAll(
                newFadeTrans,
                newTranslateTrans
        );
        return returnPT;
    }

}
