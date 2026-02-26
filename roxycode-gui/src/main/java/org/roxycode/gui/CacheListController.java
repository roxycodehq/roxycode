package org.roxycode.gui;

import com.google.genai.types.CachedContent;
import io.micronaut.context.annotation.Prototype;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Prototype
public class CacheListController {

    @FXML
    private TableView<CacheRow> cacheTable;

    @FXML
    private TableColumn<CacheRow, String> nameColumn;

    @FXML
    private TableColumn<CacheRow, String> createTimeColumn;

    @FXML
    private TableColumn<CacheRow, String> expireTimeColumn;

    @FXML
    private TableColumn<CacheRow, Number> tokensColumn;

    @Inject
    private GeminiService geminiService;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayName()));
        createTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().createTime()));
        expireTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().expireTime()));
        tokensColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().totalTokens()));

        refresh();
    }

    @FXML
    private void refresh() {
        new Thread(() -> {
            try {
                List<CachedContent> caches = geminiService.listCaches();
                List<CacheRow> rows = caches.stream()
                        .map(c -> new CacheRow(
                                c.displayName().orElse(c.name().orElse("Unnamed")),
                                c.createTime().map(Object::toString).orElse("-"),
                                c.expireTime().map(Object::toString).orElse("-"),
                                c.usageMetadata().flatMap(um -> um.totalTokenCount()).orElse(0)
                        ))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    cacheTable.setItems(FXCollections.observableArrayList(rows));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public record CacheRow(
            String displayName,
            String createTime,
            String expireTime,
            long totalTokens
    ) {}
}
