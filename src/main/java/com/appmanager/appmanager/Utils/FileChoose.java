package com.appmanager.appmanager.Utils;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
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
        File file = fileChooser.showOpenDialog(window) ;
        File destDir = new File("resources/Setups");
        if (file != null) {
            return copyDirectory(file,destDir);
        }
        return null;
    }

    public String directoryChooser(Node Owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar Carpeta");
        Window window = Owner.getScene().getWindow();
        File selectedDirectory = dc.showDialog(window);
        if (selectedDirectory != null) {
            System.out.println("El usuario seleccionó la carpeta: " + selectedDirectory.getAbsolutePath());
            File destDir = new File("resources/Setups");
            return copyDirectory(selectedDirectory,destDir);
        } else {
        return "Error al seleccionar carpeta";
        }
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

    public String copyDirectory(File sourceDir, File destDir) {
        if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            showError("Error al copiar carpeta", "Directorio origen inválido");
            return null;
        }
        if (destDir == null) {
            showError("Error al copiar carpeta", "Directorio destino inválido");
            return null;
        }

        Path sourcePath = sourceDir.toPath();
        Path destParent = destDir.toPath();
        Path targetRoot = destParent.resolve(sourcePath.getFileName());

        try {
            Files.createDirectories(targetRoot);

            try {
                Files.walk(sourcePath).forEach(source -> {
                    Path relative = sourcePath.relativize(source);
                    Path target = targetRoot.resolve(relative);
                    try {
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException re) {
                Throwable cause = re.getCause();
                showError("Error al copiar carpeta", cause != null ? cause.getMessage() : re.getMessage());
                return null;
            }

            return targetRoot.toString();
        } catch (IOException e) {
            showError("Error al copiar carpeta", e.getMessage());
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