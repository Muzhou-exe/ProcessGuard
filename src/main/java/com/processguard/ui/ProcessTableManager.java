package com.processguard.ui;

import com.processguard.core.AppConfig;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Status;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessTableManager {

    private final TableView<ProcessInfo> processTable = new TableView<>();
    private final ObservableList<ProcessInfo> masterData = FXCollections.observableArrayList();

    private AlertSidebarManager alertSidebarManager;
    private StatusBarManager statusBarManager;

    private Long selectedPid = null;

    public ProcessTableManager() {
        initializeTable();
    }

    private void initializeTable() {
        SortedList<ProcessInfo> sortedData = new SortedList<>(masterData);
        sortedData.comparatorProperty().bind(processTable.comparatorProperty());
        processTable.setItems(sortedData);

        setupColumns();
        setupRowFactory();
    }

    private void setupColumns() {
        String[] colNames = {"PID", "Process Name", "Executable Path", "CPU %", "Memory MB",
                "Status", "Parent PID", "Start Time", "Captured At"};
        String[] propertyNames = {"pid", "name", "executablePath", "cpuUsage", "memoryUsageMB",
                "status", "parentPid", "startTime", "capturedAt"};

        Map<String, TableColumn<ProcessInfo, ?>> columnMap = new HashMap<>();

        for (int i = 0; i < colNames.length; i++) {
            String prop = propertyNames[i];
            String name = colNames[i];

            TableColumn<ProcessInfo, ?> col;
            if ("cpuUsage".equals(prop)) {
                col = createCpuColumn(name);
            } else if ("memoryUsageMB".equals(prop)) {
                col = createMemoryColumn(name);
            } else {
                col = new TableColumn<>(name);
                col.setCellValueFactory(new PropertyValueFactory<>(prop));
            }
            columnMap.put(prop, col);
        }

        processTable.getColumns().addAll(columnMap.values());

        TableColumn<ProcessInfo, Double> cpuCol =
                (TableColumn<ProcessInfo, Double>) columnMap.get("cpuUsage");
        if (cpuCol != null) {
            cpuCol.setSortType(TableColumn.SortType.DESCENDING);
            processTable.getSortOrder().add(cpuCol);
            processTable.sort();
        }
    }

    public void connectSidebar(AlertSidebarManager sidebar) {
        this.alertSidebarManager = sidebar;
        sidebar.setMasterData(masterData);
    }

    private TableColumn<ProcessInfo, Double> createCpuColumn(String name) {
        TableColumn<ProcessInfo, Double> col = new TableColumn<>(name);
        col.setCellValueFactory(new PropertyValueFactory<>("cpuUsage"));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.1f %%", item));
            }
        });
        col.setComparator(Double::compare);
        return col;
    }

    private TableColumn<ProcessInfo, Long> createMemoryColumn(String name) {
        TableColumn<ProcessInfo, Long> col = new TableColumn<>(name);
        col.setCellValueFactory(new PropertyValueFactory<>("memoryUsageMB"));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item + " MB");
            }
        });
        col.setComparator(Long::compare);
        return col;
    }

    private void setupRowFactory() {
        processTable.setRowFactory(tv -> {
            TableRow<ProcessInfo> row = new TableRow<>();

            ContextMenu contextMenu = createContextMenu(row);
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) return;

                ProcessInfo p = row.getItem();
                if (p == null) return;

                selectedPid = p.getPid();

                if (alertSidebarManager != null) {
                    alertSidebarManager.selectProcess(p);
                }
            });

            row.itemProperty().addListener((obs, oldItem, item) -> applyRowHighlighting(row, item));

            return row;
        });
    }

    private ContextMenu createContextMenu(TableRow<ProcessInfo> row) {
        ContextMenu menu = new ContextMenu();
        MenuItem flagItem    = new MenuItem("Flag Process");
        MenuItem killItem    = new MenuItem("Kill Process");
        MenuItem copyPidItem = new MenuItem("Copy PID");
        MenuItem detailsItem = new MenuItem("View Details");

        final Long[] capturedPid = { null };

        menu.setOnShowing(e -> {
            ProcessInfo item = row.getItem();
            if (item == null) {
                menu.hide();
                return;
            }

            capturedPid[0] = item.getPid();
            flagItem.setText(item.isFlagged() ? "Unflag Process" : "Flag Process");
        });

        flagItem.setOnAction(e -> {
            if (capturedPid[0] == null) return;

            masterData.stream()
                    .filter(p -> p.getPid() == capturedPid[0])
                    .findFirst()
                    .ifPresent(p -> {
                        if (p.isFlagged()) {
                            p.unflag();
                            if (alertSidebarManager != null) alertSidebarManager.selectProcess(p);
                        } else {
                            processTable.getSelectionModel().select(p);
                            flagSelectedProcess();
                        }
                    });

            processTable.refresh();
        });

        killItem.setOnAction(e -> {
            if (capturedPid[0] == null) return;

            masterData.stream()
                    .filter(p -> p.getPid() == capturedPid[0])
                    .findFirst()
                    .ifPresent(p -> {
                        if (alertSidebarManager != null) {
                            alertSidebarManager.killProcess(p);
                        }
                    });
        });

        copyPidItem.setOnAction(e -> {
            if (capturedPid[0] == null) return;

            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(capturedPid[0]));
            Clipboard.getSystemClipboard().setContent(content);
        });

        detailsItem.setOnAction(e -> {
            if (capturedPid[0] == null) return;

            masterData.stream()
                    .filter(p -> p.getPid() == capturedPid[0])
                    .findFirst()
                    .ifPresent(p -> {
                        if (alertSidebarManager != null) {
                            alertSidebarManager.selectProcess(p);
                        }
                    });
        });

        row.setOnDragDetected(e -> {
            if (row.isEmpty()) return;
            ProcessInfo p = row.getItem();
            if (p == null) return;

            Dragboard db = row.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(p.getName());
            content.put(new DataFormat("process/pid"), p.getPid());
            content.put(new DataFormat("process/cpu"), p.getCpuUsage());
            content.put(new DataFormat("process/mem"), p.getMemoryUsageMB());
            content.put(new DataFormat("process/name"), p.getName());
            db.setContent(content);
            e.consume();
        });

        menu.getItems().addAll(killItem, copyPidItem, detailsItem, flagItem);
        return menu;
    }

    private void copyPidToClipboard(ProcessInfo p) {
        if (p == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(String.valueOf(p.getPid()));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void applyRowHighlighting(TableRow<ProcessInfo> row, ProcessInfo item) {
        if (item != null && item.isFlagged()) {
            row.setTooltip(new Tooltip("🚩 " + item.getFlagReason()));
            row.setStyle("-fx-background-color: rgba(0, 120, 255, 0.35);");
            return;
        }

        if (item == null) {
            row.setStyle("");
            return;
        }

        double cpuThreshold = AppConfig.getInstance().getCpuThreshold();
        double memThreshold = AppConfig.getInstance().getMemoryThreshold();

        Status effectiveStatus = item.getStatus();
        if (effectiveStatus == Status.NORMAL &&
                (item.getCpuUsage() > cpuThreshold || item.getMemoryUsageMB() > memThreshold)) {
            effectiveStatus = Status.SUSPICIOUS;
        }

        switch (effectiveStatus) {
            case BLOCKED -> row.setStyle("-fx-background-color: rgba(255,0,0,0.3);");
            case SUSPICIOUS -> row.setStyle("-fx-background-color: rgba(255,255,0,0.3);");
            default -> row.setStyle("");
        }
    }

    public void flagSelectedProcess() {
        ProcessInfo selected = processTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            System.out.println("No process selected");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Flag Process");
        dialog.setHeaderText("Enter reason for flagging process " + selected.getName());

        dialog.showAndWait().ifPresent(reason -> {

            long pid = selected.getPid();

            masterData.stream()
                    .filter(p -> p.getPid() == pid)
                    .findFirst()
                    .ifPresent(p -> {
                        p.flag(reason);
                        if (alertSidebarManager != null) {
                            alertSidebarManager.selectProcess(p);
                        }
                    });

            processTable.refresh();
        });
    }

    public TableView<ProcessInfo> getTable() {
        return processTable;
    }

    public void updateTable(List<ProcessInfo> snapshot) {
        Map<Long, ProcessInfo> existing = new HashMap<>();
        for (ProcessInfo p : masterData) {
            existing.put(p.getPid(), p);
        }

        for (ProcessInfo incoming : snapshot) {
            ProcessInfo old = existing.get(incoming.getPid());

            if (old != null) {

                if (old.isFlagged()) {
                    incoming.flag(old.getFlagReason());
                }

                // replace old object safely
                int index = masterData.indexOf(old);
                if (index >= 0) {
                    masterData.set(index, incoming);
                }

            } else {
                masterData.add(incoming);
            }
        }

        // remove processes that disappeared
        masterData.removeIf(p ->
                snapshot.stream().noneMatch(n -> n.getPid() == p.getPid())
        );

        if (selectedPid != null) {
            final Long pidToReselect = selectedPid;
            processTable.getItems().stream()
                    .filter(p -> p.getPid() == pidToReselect)
                    .findFirst()
                    .ifPresent(p -> processTable.getSelectionModel().select(p));
        }
    }

    public void addProcesses(List<ProcessInfo> newProcesses) {
        masterData.addAll(newProcesses);
    }

    public void removeProcesses(List<ProcessInfo> exitedProcesses) {
        masterData.removeAll(exitedProcesses);
    }

    public List<ProcessInfo> getCurrentData() {
        return List.copyOf(masterData);
    }

    public void setAlertSidebarManager(AlertSidebarManager manager) {
        this.alertSidebarManager = manager;
    }

    public void setStatusBarManager(StatusBarManager manager) {
        this.statusBarManager = manager;
    }
}