package main.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

// Largely from http://code.makery.ch/blog/javafx-dialogs-official/

public class stacktraceAlert {
    public void create(String title, String header, String error_reason, Exception exception_obj) {
        // Allows this to run inside a thread that is not the main fx thread
        Platform.runLater(new Runnable(){
            public void run(){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText(header);
                alert.setContentText(error_reason);

                // Create expandable Exception.
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception_obj.printStackTrace(pw);
                String exceptionText = sw.toString();

                Label label = new Label("The exception stacktrace was:");

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                // Set expandable Exception into the dialog pane.
                alert.getDialogPane().setExpandableContent(expContent);

                alert.showAndWait();

                // Stop app when window closed
                Platform.exit();
                System.exit(0);
            }
        });
    }
}
