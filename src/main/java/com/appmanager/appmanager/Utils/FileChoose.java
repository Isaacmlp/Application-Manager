package com.appmanager.appmanager.Utils;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileChoose {

    // Seleccionar archivo y moverlo a carpeta destino
    public String showFileChooser(Node owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");

        Window window = owner.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);

        if (file != null) {
            return moveFile(file);
        }
        return null;
    }

    // Guardar archivo en ruta elegida
    public String chooseSavePath(Node owner, String nombreArchivo) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo");
        fileChooser.setInitialFileName(nombreArchivo + ".pdf");

        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);

        Window window = owner.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(window);

        if (selectedFile != null) {
            System.out.println("El usuario seleccionó: " + selectedFile.getAbsolutePath());
            return selectedFile.getAbsolutePath();
        } else {
            showError("Error al guardar el archivo", "No se seleccionó ningún destino");
            return null;
        }
    }

    // Mover archivo a carpeta Insumos
    private String moveFile(File file) {
        String destinoDir = System.getProperty("user.home") + "/Insumos";
        Path destinoPath = Path.of(destinoDir, file.getName());

        try {
            Files.createDirectories(Path.of(destinoDir));
            Files.copy(file.toPath(), destinoPath, StandardCopyOption.REPLACE_EXISTING);
            return destinoPath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al mover archivo", e.getMessage());
            return null;
        }
    }

    // Mostrar alertas de error
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}