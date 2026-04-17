package com.processguard.ui;

import com.processguard.core.AppConfig;
import com.processguard.models.Condition;
import com.processguard.models.CustomRule;
import com.processguard.models.Severity;
import com.processguard.models.RuleAction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * UI dialog for creating, viewing, and managing custom process detection rules.
 */
public class RuleManagerDialog {

    private final Stage stage;
    private final ObservableList<CustomRule> ruleData;
    private final ListView<CustomRule> ruleList;

    // Form fields
    private final TextField nameField;
    private final TextField valueField;
    private final ComboBox<String> fieldBox;
    private final ComboBox<String> opBox;
    private final ComboBox<RuleAction> actionBox;

    /**
     * Constructs the RuleManagerDialog and initializes UI components.
     * @param owner the parent stage that owns this dialog
     */
    public RuleManagerDialog(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Rule Manager - Custom Detection Rules");

        ruleData = FXCollections.observableArrayList();
        ruleList = new ListView<>(ruleData);
        ruleList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ruleList.setCellFactory(lv -> new ListCell<>() {
            /**
             * Renders each rule item in the list view.
             * @param rule the rule item
             * @param empty whether the cell is empty
             */
            @Override
            protected void updateItem(CustomRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) {
                    setText(null);
                } else {
                    String status = rule.isEnabled() ? "✓ Enabled" : "✗ Disabled";
                    setText(rule.getName() + "  (" + status + ")");
                }
            }
        });

        nameField = new TextField();
        nameField.setPromptText("Enter a descriptive rule name");

        valueField = new TextField();
        valueField.setPromptText("Value to match (e.g. chrome, 80)");

        fieldBox = new ComboBox<>(FXCollections.observableArrayList(
                "name", "executablePath", "cpuUsage", "memoryUsageMB"));
        fieldBox.setValue("name");

        opBox = new ComboBox<>(FXCollections.observableArrayList(
                "CONTAINS", "EQUALS", "GREATER_THAN", "LESS_THAN"));
        opBox.setValue("CONTAINS");

        actionBox = new ComboBox<>(FXCollections.observableArrayList(
                RuleAction.values()
        ));
        actionBox.setValue(RuleAction.ALERT_ONLY);

        Button btnAdd = new Button("Add Rule");
        Button btnSample = new Button("Add Sample Rule");
        Button btnDelete = new Button("Delete Selected");
        Button btnToggle = new Button("Enable / Disable");

        btnAdd.setOnAction(e -> addRuleFromFields());
        btnSample.setOnAction(e -> addSampleRule());
        btnDelete.setOnAction(e -> deleteSelectedRule());
        btnToggle.setOnAction(e -> toggleSelectedRule());

        btnDelete.disableProperty().bind(ruleList.getSelectionModel().selectedItemProperty().isNull());
        btnToggle.disableProperty().bind(ruleList.getSelectionModel().selectedItemProperty().isNull());

        VBox formBox = new VBox(8,
                new Label("Create New Rule"),
                new Label("Rule Name:"), nameField,
                new Label("Field:"), fieldBox,
                new Label("Operator:"), opBox,
                new Label("Value:"), valueField,
                new Label("Action:"), actionBox,
                new HBox(10, btnAdd, btnSample)
        );
        formBox.setPadding(new Insets(15));

        VBox listBox = new VBox(8,
                new Label("Existing Rules"),
                ruleList
        );
        listBox.setPadding(new Insets(15));

        Label dragHint = new Label("💡 Tip: Drag a process from the main ProcessGuard window into this dialog to auto-fill the rule");
        dragHint.setStyle("-fx-font-style: italic; -fx-text-fill: #555555;");

        VBox centerArea = new VBox(15, listBox, dragHint, formBox);

        HBox buttonBar = new HBox(10, btnDelete, btnToggle);
        buttonBar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(centerArea);
        root.setBottom(buttonBar);

        root.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        root.setOnDragDropped(e -> {
            var db = e.getDragboard();
            if (db.hasString()) {
                String processName = db.getString();
                nameField.setText("High usage " + processName);
                fieldBox.setValue("name");
                opBox.setValue("CONTAINS");
                valueField.setText(processName);
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        root.setOnDragEntered(e -> root.setStyle("-fx-border-color: #2196F3; -fx-border-width: 3; -fx-border-style: dashed;"));
        root.setOnDragExited(e -> root.setStyle(""));

        stage.setScene(new Scene(root, 700, 620));
        refresh();
    }

    /**
     * Displays the rule manager window.
     */
    public void show() {
        refresh();
        stage.show();
        stage.toFront();
    }

    /**
     * Refreshes the rule list from application config.
     */
    private void refresh() {
        ruleData.setAll(AppConfig.getInstance().getCustomRules());
    }

    /**
     * Adds a rule based on user input fields.
     */
    private void addRuleFromFields() {
        String name = (nameField.getText() == null ? "" : nameField.getText()).trim();
        String valueStr = (valueField.getText() == null ? "" : valueField.getText()).trim();

        if (name.isEmpty() || valueStr.isEmpty()) {
            showAlert("Input Error", "Rule Name and Value are required.");
            return;
        }

        try {
            Condition condition = new Condition(fieldBox.getValue(), opBox.getValue(), valueStr);

            CustomRule rule = new CustomRule(
                    System.currentTimeMillis(),
                    name,
                    "User created rule",
                    true,
                    List.of(condition),
                    "AND",
                    Severity.MEDIUM,
                    "Custom rule triggered",
                    5,
                    actionBox.getValue()
            );

            AppConfig.getInstance().addCustomRule(rule);
            refresh();
            showAlert("Success", "Rule created successfully:\n" + name);

            nameField.clear();
            valueField.clear();

        } catch (Exception ex) {
            showAlert("Error", "Failed to create rule:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Adds a predefined sample rule.
     */
    private void addSampleRule() {
        try {
            CustomRule rule = new CustomRule(
                    System.currentTimeMillis(),
                    "High CPU Chrome",
                    "Detect Chrome using too much CPU",
                    true,
                    List.of(
                            new Condition("name", "CONTAINS", "chrome"),
                            new Condition("cpuUsage", "GREATER_THAN", "50")
                    ),
                    "AND",
                    Severity.HIGH,
                    "Chrome CPU spike detected",
                    10,
                    RuleAction.ALERT_ONLY
            );

            AppConfig.getInstance().addCustomRule(rule);
            refresh();
            showAlert("Success", "Sample rule added.");
        } catch (Exception ex) {
            showAlert("Error", "Failed to add sample rule.");
        }
    }

    /**
     * Deletes the currently selected rule.
     */
    private void deleteSelectedRule() {
        CustomRule selected = ruleList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            List<CustomRule> current = new ArrayList<>(AppConfig.getInstance().getCustomRules());
            current.remove(selected);
            AppConfig.getInstance().setCustomRules(current);
            refresh();
        } catch (Exception ex) {
            showAlert("Error", "Failed to delete rule.");
        }
    }

    /**
     * Toggles enabled/disabled state of selected rule.
     */
    private void toggleSelectedRule() {
        CustomRule selected = ruleList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            selected.setEnabled(!selected.isEnabled());
            AppConfig.getInstance().setCustomRules(new ArrayList<>(AppConfig.getInstance().getCustomRules()));
            refresh();
        } catch (Exception ex) {
            showAlert("Error", "Failed to toggle rule.");
        }
    }

    /**
     * Shows an alert dialog with a message.
     * @param title alert title
     * @param message alert content
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}