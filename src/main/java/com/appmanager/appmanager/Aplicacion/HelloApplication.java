package com.appmanager.appmanager.Aplicacion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import com.appmanager.appmanager.Utils.MetadataExtractor;


public class HelloApplication extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println(Objects.requireNonNull(getClass().getResource("/Setups/")).toURI());

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/View/DashboardView.fxml")));
        Parent root = loader.load();

        // Hacer la ventana movable (sin bordes nativos)
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/Style/Style.css")).toExternalForm());

        primaryStage.setTitle("Instalador de Aplicaciones");
        primaryStage.initStyle(StageStyle.TRANSPARENT); // Para diseño personalizado
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/img.png"))));
        primaryStage.setScene(scene);
        primaryStage.show();
    }


}
