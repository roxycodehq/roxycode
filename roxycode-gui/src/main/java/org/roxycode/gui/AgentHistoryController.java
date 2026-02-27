package org.roxycode.gui;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.micronaut.context.annotation.Prototype;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import jakarta.inject.Inject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Prototype
public class AgentHistoryController {

    @FXML
    private ListView<Agent> agentListView;

    @FXML
    private Label selectedAgentLabel;

    @FXML
    private TableView<HistoryEntry> historyTable;

    @FXML
    private TableColumn<HistoryEntry, Integer> indexColumn;

    @FXML
    private TableColumn<HistoryEntry, String> roleColumn;

    @FXML
    private TableColumn<HistoryEntry, String> contentColumn;

    @Inject
    private AgentService agentService;
    
    @Inject
    private ProjectService projectService;

    private final ObservableList<HistoryEntry> historyEntries = FXCollections.observableArrayList();

    public record HistoryEntry(int index, String role, String content) {}

    @FXML
    public void initialize() {
        indexColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().index()).asObject());
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role()));
        contentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().content()));

        // Enable text wrapping in cells
        contentColumn.setCellFactory(tc -> {
            TableCell<HistoryEntry, String> cell = new TableCell<>() {
                private final Text text = new Text();
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        text.setText(item);
                        text.wrappingWidthProperty().bind(contentColumn.widthProperty().subtract(10));
                        setGraphic(text);
                    }
                }
            };
            return cell;
        });

        historyTable.setItems(historyEntries);
        
        agentListView.getItems().setAll(agentService.getAgents());
        agentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showHistory(newVal);
            }
        });

        if (!agentListView.getItems().isEmpty()) {
            agentListView.getSelectionModel().select(0);
        }
    }

    public static List<HistoryEntry> generateHistoryEntries(Agent agent) {
        List<HistoryEntry> entries = new ArrayList<>();
        int counter = 1;

        // 1. Add System Prompt
        entries.add(new HistoryEntry(counter++, "SYSTEM", agent.getSystemPrompt()));

        // 2. Add History
        List<Content> history = agent.getHistory();
        String lastLabel = null;
        StringBuilder accumulator = new StringBuilder();

        for (Content content : history) {
            String role = content.role().orElse("unknown").toUpperCase();
            List<Part> parts = content.parts().orElse(List.of());
            
            for (Part part : parts) {
                if (part.text().isPresent()) {
                    boolean isThought = part.thought().orElse(false);
                    String currentLabel = isThought ? "THOUGHT" : role;

                    if (lastLabel != null && !lastLabel.equals(currentLabel)) {
                        if (accumulator.length() > 0) {
                            entries.add(new HistoryEntry(counter++, lastLabel, accumulator.toString()));
                            accumulator.setLength(0);
                        }
                    }

                    lastLabel = currentLabel;
                    accumulator.append(part.text().get());
                } else {
                    // Flush accumulator before tool calls/responses
                    if (accumulator.length() > 0 && lastLabel != null) {
                        entries.add(new HistoryEntry(counter++, lastLabel, accumulator.toString()));
                        accumulator.setLength(0);
                    }
                    lastLabel = null;

                    if (part.functionCall().isPresent()) {
                        String call = "Tool Call: " + part.functionCall().get().name().orElse("?") + " " + part.functionCall().get().args().orElse(Map.of());
                        entries.add(new HistoryEntry(counter++, "TOOL_CALL", call));
                    } else if (part.functionResponse().isPresent()) {
                        String resp = "Tool Response: " + part.functionResponse().get().name().orElse("?") + " " + part.functionResponse().get().response().orElse(Map.of());
                        entries.add(new HistoryEntry(counter++, "TOOL_RESP", resp));
                    }
                }
            }
        }

        // Final flush
        if (accumulator.length() > 0 && lastLabel != null) {
            entries.add(new HistoryEntry(counter++, lastLabel, accumulator.toString()));
        }

        return entries;
    }

    private void showHistory(Agent agent) {
        selectedAgentLabel.setText("History for: " + agent.getName());
        historyEntries.clear();
        historyEntries.addAll(generateHistoryEntries(agent));
    }

        @FXML
    private void handleClearHistory() {
        projectService.clearSession();
        Agent selected = agentListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.getHistory().clear();
            selected.setCurrentTurns(0);
            selected.setHistorySize(0);
            showHistory(selected);
        }
    }
}