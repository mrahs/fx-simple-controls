package pw.ahs.app.fxsimplecontrols;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.Collection;

public class Dialogs {

    private static final int DEFAULT_DURATION = 200;

    /**
     * Create a new utility stage, centered on owner, with fade in and fade out effects on a timeline of 200 milliseconds.
     *
     * @param owner the dialog owner
     * @param title the dialog title
     * @param root  the dialog scene's root
     * @return a new utility stage with fade in and fade out effect on a timeline of 200 milliseconds.
     */
    public static Stage createUtilityDialog(
            Stage owner,
            String title,
            Parent root
    ) {
        Scene scene = new Scene(root);
        Timeline fadeOut = new Timeline();
        Timeline fadeIn = new Timeline();

        Stage stage = new Stage(StageStyle.UTILITY) {
            {
                fadeOut.setOnFinished(evt -> super.hide());
            }

            @Override
            public void hide() {
                fadeOut.play();
            }
        };
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setOpacity(0);

        fadeOut.setCycleCount(1);
        fadeOut.setAutoReverse(false);
        fadeOut.getKeyFrames().add(new KeyFrame(Duration.millis(DEFAULT_DURATION), new KeyValue(stage.opacityProperty(), 0)));

        fadeIn.setCycleCount(1);
        fadeIn.setAutoReverse(false);
        // a workaround to avoid nudges at start
        fadeIn.setDelay(Duration.millis(100));
        fadeIn.getKeyFrames().add(new KeyFrame(Duration.millis(DEFAULT_DURATION), new KeyValue(stage.opacityProperty(), 1)));

        stage.setOnShown(evt -> {
            if (stage.getOwner() != null) {
                stage.setX(stage.getOwner().getX() + stage.getOwner().getWidth() / 2 - stage.getWidth() / 2);
                stage.setY(stage.getOwner().getY() + stage.getOwner().getHeight() / 2 - stage.getHeight() / 2);
            }
            fadeIn.play();
        });

        return stage;
    }

    /**
     * Create a utility dialog with a progress indicator.
     * If {@code buttonText} is empty, there will be no way for the user to close the stage.
     *
     * @param owner      the dialog owner
     * @param title      the dialog title
     * @param msg        the message
     * @param buttonText the button text
     * @return a utility dialog with a progress indicator
     * @see #createUtilityDialog(javafx.stage.Stage, String, javafx.scene.Parent)
     */
    public static Stage createWaitingDialog(Stage owner, String title, String msg, String buttonText) {
        VBox layout = new VBox();
        Stage stage = createUtilityDialog(owner, title, layout);

        if (!msg.isEmpty())
            layout.getChildren().addAll(new Label(msg));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1);
        progressIndicator.setMinSize(50, 50);

        layout.getChildren().addAll(progressIndicator);

        if (buttonText.isEmpty()) {
            stage.setOnCloseRequest(WindowEvent::consume);
        } else {
            Button button = new Button(buttonText);
            button.setOnAction(actionEvent -> stage.hide());
            button.setCancelButton(true);
            layout.getChildren().addAll(button);

            stage.setOnCloseRequest(windowEvent -> {
                windowEvent.consume();
                stage.hide();
            });
        }

        layout.setSpacing(20);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);
        layout.setMinSize(200, 200);

        return stage;
    }

    /**
     * Show a utility dialog with a message, a content node, and three buttons: yes, no, cancel.
     * If {@code cancelText} is empty, don't show cancel button.
     * If {@code noText} is empty, don't show no button.
     *
     * @param owner         the dialog owner
     * @param title         the dialog title
     * @param content       the content
     * @param yesText       yes button text
     * @param noText        no button text
     * @param cancelText    cancel button text
     * @param buttonToFocus 1 = 1 = yes, 2 = no, 3 = cancel, else = focus content
     * @return one of {@link pw.ahs.app.fxsimplecontrols.Dialogs.Result} values
     */
    public static Result showYesNoDialog(
            Stage owner,
            String title,
            Node content,
            String yesText,
            String noText,
            String cancelText,
            int buttonToFocus
    ) {
        VBox layout = new VBox();
        Stage stage = createUtilityDialog(owner, title, layout);

        SimpleObjectProperty<Result> result = new SimpleObjectProperty<>(Result.CANCEL);

        Button buttonYes = new Button(yesText);
        buttonYes.setOnAction(actionEvent -> {
            result.set(Result.YES);
            stage.hide();
        });
        buttonYes.setDefaultButton(true);

        Button buttonNo = noText.isEmpty() ? null : new Button(noText);
        Button buttonCancel = cancelText.isEmpty() ? null : new Button(cancelText);

        HBox layoutButtons = new HBox(buttonYes);
        layoutButtons.setSpacing(5);
        layoutButtons.setAlignment(Pos.BASELINE_CENTER);

        if (buttonNo != null) {
            buttonNo.setOnAction(actionEvent -> {
                result.set(Result.NO);
                stage.hide();
            });
            layoutButtons.getChildren().add(buttonNo);
        }

        if (buttonCancel == null) {
            if (buttonNo != null) buttonNo.setCancelButton(true);
        } else {
            buttonCancel.setOnAction(actionEvent -> {
                result.set(Result.CANCEL);
                stage.hide();
            });
            buttonCancel.setCancelButton(true);
            layoutButtons.getChildren().add(buttonCancel);
        }


        layout.setSpacing(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(content, layoutButtons);
        layout.setMinHeight(100);
        layout.setMinWidth(300);

        stage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            if (buttonCancel != null) buttonCancel.fire();
            else if (buttonNo != null) buttonNo.fire();
            else stage.hide();
        });

        if (buttonToFocus == 1)
            buttonYes.requestFocus();
        else if (buttonNo != null && buttonToFocus == 2)
            buttonNo.requestFocus();
        else if (buttonCancel != null && buttonToFocus == 3)
            buttonCancel.requestFocus();
        else if (content != null)
            content.requestFocus();

        stage.showAndWait();

        return result.get();
    }

    public static Result showYesNoDialog(
            Stage owner,
            String title,
            String msg,
            String yesText,
            String noText,
            String cancelText,
            int buttonToFocus
    ) {
        return showYesNoDialog(owner, title, new Label(msg), yesText, noText, cancelText, buttonToFocus);
    }

    /**
     * Show a utility dialog with a simple message and a button.
     *
     * @param owner      the dialog owner
     * @param title      the dialog title
     * @param msgText    the message
     * @param buttonText the button text
     * @see #createUtilityDialog(javafx.stage.Stage, String, javafx.scene.Parent)
     * @see #showYesNoDialog(javafx.stage.Stage, String, javafx.scene.Node, String, String, String, int)
     */
    public static void showMessageDialog(
            Stage owner,
            String title,
            String msgText,
            String buttonText
    ) {
        showYesNoDialog(owner, title, new Label(msgText), buttonText, "", "", 1);
    }

    /**
     * Show a utility dialog with combo box for multiple choices.
     *
     * @param owner            the dialog owner
     * @param title            the dialog title
     * @param content          the content
     * @param msg              the message
     * @param buttonInputText  input button text
     * @param buttonCancelText cancel button text
     * @param values           possible values
     * @param initialValue     initial value (may be null)
     * @param converter        string converter (may be null)
     * @param <T>              the type of values
     * @return the selected item
     */
    public static <T> T showInputDialog(
            Stage owner,
            String title,
            Node content,
            String msg,
            String buttonInputText,
            String buttonCancelText,
            Collection<T> values,
            T initialValue,
            StringConverter<T> converter
    ) {
        VBox layout = new VBox();
        Stage stage = createUtilityDialog(owner, title, layout);

        ComboBox<T> items = new ComboBox<>(FXCollections.observableArrayList(values));
        items.getSelectionModel().select(initialValue);
        if (converter != null) items.setConverter(converter);
        items.requestFocus();

        SimpleObjectProperty<T> val = new SimpleObjectProperty<>();

        Button buttonInput = new Button(buttonInputText);
        buttonInput.setOnAction(actionEvent -> {
            val.set(items.getValue());
            stage.hide();
        });
        buttonInput.setDefaultButton(true);

        Button buttonCancel = new Button(buttonCancelText);
        buttonCancel.setOnAction(actionEvent -> {
            val.set(null);
            stage.hide();
        });
        buttonCancel.setCancelButton(true);

        HBox layoutInput = new HBox(new Label(msg), items);
        layoutInput.setSpacing(3);
        layoutInput.setAlignment(Pos.BASELINE_CENTER);

        HBox layoutButtons = new HBox(buttonInput, buttonCancel);
        layoutButtons.setSpacing(5);
        layoutButtons.setAlignment(Pos.BASELINE_CENTER);

        layout.setSpacing(10);
        layout.setAlignment(Pos.CENTER);
        if (content != null) layout.getChildren().add(content);
        layout.getChildren().addAll(layoutInput, layoutButtons);
        layout.setMinHeight(100);
        layout.setMinWidth(300);

        buttonInput.requestFocus();

        stage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            buttonCancel.fire();
        });

        stage.showAndWait();

        return val.get();
    }

    /**
     * @see #showInputDialog(javafx.stage.Stage, String, javafx.scene.Node, String, String, String, java.util.Collection, Object, javafx.util.StringConverter)
     */
    public static <T> T showInputDialog(
            Stage owner,
            String title,
            Node content,
            String msg,
            String buttonInputText,
            String buttonCancelText,
            Collection<T> values,
            T initialValue
    ) {
        return showInputDialog(owner, title, content, msg, buttonInputText, buttonCancelText, values, initialValue, null);
    }

    /**
     * @see #showInputDialog(javafx.stage.Stage, String, javafx.scene.Node, String, String, String, java.util.Collection, Object, javafx.util.StringConverter)
     */
    public static <T> T showInputDialog(
            Stage owner,
            String title,
            String msg,
            String buttonInputText,
            String buttonCancelText,
            Collection<T> values,
            T initialValue,
            StringConverter<T> converter
    ) {
        return showInputDialog(owner, title, null, msg, buttonInputText, buttonCancelText, values, initialValue, converter);
    }

    /**
     * @see #showInputDialog(javafx.stage.Stage, String, javafx.scene.Node, String, String, String, java.util.Collection, Object, javafx.util.StringConverter)
     */
    public static <T> T showInputDialog(
            Stage owner,
            String title,
            String msg,
            String buttonInputText,
            String buttonCancelText,
            Collection<T> values,
            T initialValue
    ) {
        return showInputDialog(owner, title, null, msg, buttonInputText, buttonCancelText, values, initialValue, null);
    }


    /**
     * Show a simple text input dialog.
     *
     * @param owner            the dialog owner
     * @param title            the dialog title
     * @param msgText          the message
     * @param initial          initial text
     * @param buttonInputText  input button text
     * @param buttonCancelText cancel button text
     * @return the string input
     */
    public static String showTextInputDialog(
            Stage owner,
            String title,
            String msgText,
            String initial,
            String buttonInputText,
            String buttonCancelText
    ) {
        VBox layout = new VBox();
        Stage stage = createUtilityDialog(owner, title, layout);

        Text msg = new Text(msgText);
        msg.setTextAlignment(TextAlignment.CENTER);

        TextField textField = new TextField(initial);

        StringProperty val = new SimpleStringProperty("");

        Button buttonInput = new Button(buttonInputText);
        buttonInput.setOnAction(actionEvent -> {
            val.set(textField.getText());
            stage.hide();
        });
        buttonInput.setDefaultButton(true);

        Button buttonCancel = new Button(buttonCancelText);
        buttonCancel.setOnAction(actionEvent -> {
            val.set("");
            stage.hide();
        });
        buttonCancel.setCancelButton(true);

        HBox layoutInput = new HBox(msg, textField);
        layoutInput.setSpacing(3);
        layoutInput.setAlignment(Pos.BASELINE_CENTER);

        HBox layoutButtons = new HBox(buttonInput, buttonCancel);
        layoutButtons.setSpacing(5);
        layoutButtons.setAlignment(Pos.BASELINE_CENTER);

        layout.setSpacing(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(layoutInput, layoutButtons);
        layout.setMinHeight(100);
        layout.setMinWidth(300);

        textField.requestFocus();

        stage.setOnCloseRequest(windowEvent -> {
            windowEvent.consume();
            buttonCancel.fire();
        });

        stage.showAndWait();

        return val.get();
    }

    public static enum Result {
        YES, NO, CANCEL
    }
}
