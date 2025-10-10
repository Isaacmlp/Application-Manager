package com.appmanager.appmanager.Aplicacion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import com.appmanager.appmanager.Utils.MetadataExtractor;


public class HelloApplication extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    String Metadata;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println(getClass().getResource("/Setups/"));

        ClassLoader classLoader = MetadataExtractor.class.getClassLoader();
        URL resourcesUrl = classLoader.getResource("Setups");
        assert resourcesUrl != null;
        Map<String, Map<String, String>> result = MetadataExtractor.getExeMetadataFromFolder(resourcesUrl.toString());

        System.out.println("\n=== RESULTADOS OBTENIDOS ===");
        for (Map.Entry<String, Map<String, String>> entry : result.entrySet()) {
            System.out.println("\nArchivo: " + entry.getKey());
            Map<String, String> metadata = entry.getValue();
            System.out.println("  Nombre: " + metadata.get("Name"));
            System.out.println("  Versión: " + metadata.get("FileVersion"));
            System.out.println("  Descripción: " + metadata.get("FileDescription"));
            System.out.println("  Tamaño: " + metadata.get("SizeMB") + " MB");

            if (metadata.containsKey("Error")) {
                System.out.println("  ERROR: " + metadata.get("Error"));
            }
        }

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
