package com.appmanager.appmanager.Utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Maintenance {

    // Ruta dinámica al archivo .env en el mismo directorio del .jar
    private static final String ENV_FILE = System.getProperty("user.dir") + File.separator + ".env";

    // Carpeta setups al mismo nivel que el .jar
    private static final String SETUPS_DIR = System.getProperty("user.dir") + File.separator + "setups";

    // Verificar si el archivo .env existe, si no, crearlo
    private static void ensureEnvFileExists() throws IOException {
        Path path = Paths.get(ENV_FILE);
        if (!Files.exists(path)) {
            Files.createFile(path);
            System.out.println("Archivo .env creado en: " + ENV_FILE);
        }
    }
    Button cancelButton = new Button("Cancelar");
    private Runnable onCopySuccess;

    public void setOnCopySuccess(Runnable onCopySuccess) {
        this.onCopySuccess = onCopySuccess;
    }


    // Método genérico para actualizar una clave en el .env
    private static void updateEnvKey(String key, String newValue) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines;

        // Si el archivo está vacío o no existe, crear lista vacía
        if (Files.exists(path) && Files.size(path) > 0) {
            lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            lines = new ArrayList<>();
        }

        boolean updated = false;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + "=")) {
                lines.set(i, key + "=" + newValue);
                updated = true;
                break;
            }
        }

        if (!updated) {
            lines.add(key + "=" + newValue);
        }

        Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("Actualizado " + key + "=" + newValue);
    }

    // 👉 Método para agregar un Proxy
    public static void addProxy(String proxy) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                // Verificar si el proxy ya existe
                List<String> proxies = new ArrayList<>(Arrays.asList(current.split(";")));
                if (proxies.contains(proxy)) {
                    System.out.println("El proxy ya existe: " + proxy);
                    return;
                }
                String updated = current.isEmpty() ? proxy : current + ";" + proxy;
                lines.set(i, "PROXYS=" + updated);
                found = true;
                break;
            }
        }

        if (!found) {
            lines.add("PROXYS=" + proxy);
        }

        Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("Proxy agregado: " + proxy);
    }

    // 👉 Método para agregar un DNS
    public static void addDNS(String dns) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                // Verificar si el DNS ya existe
                List<String> dnsList = new ArrayList<>(Arrays.asList(current.split(";")));
                if (dnsList.contains(dns)) {
                    System.out.println("El DNS ya existe: " + dns);
                    return;
                }
                String updated = current.isEmpty() ? dns : current + ";" + dns;
                lines.set(i, "DNS=" + updated);
                found = true;
                break;
            }
        }

        if (!found) {
            lines.add("DNS=" + dns);
        }

        Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("DNS agregado: " + dns);
    }

    // 👉 Método para eliminar un Proxy
    public static void removeProxy(String proxy) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                List<String> proxies = new ArrayList<>(Arrays.asList(current.split(";")));
                if (proxies.remove(proxy)) {
                    if (proxies.isEmpty()) {
                        lines.remove(i); // Eliminar la línea si no hay proxys
                    } else {
                        lines.set(i, "PROXYS=" + String.join(";", proxies));
                    }
                    Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("Proxy eliminado: " + proxy);
                } else {
                    System.out.println("Proxy no encontrado: " + proxy);
                }
                return;
            }
        }
        System.out.println("No se encontró la clave PROXYS");
    }

    // 👉 Método para eliminar un DNS
    public static void removeDNS(String dns) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                List<String> dnsList = new ArrayList<>(Arrays.asList(current.split(";")));
                if (dnsList.remove(dns)) {
                    if (dnsList.isEmpty()) {
                        lines.remove(i); // Eliminar la línea si no hay DNS
                    } else {
                        lines.set(i, "DNS=" + String.join(";", dnsList));
                    }
                    Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("DNS eliminado: " + dns);
                } else {
                    System.out.println("DNS no encontrado: " + dns);
                }
                return;
            }
        }
        System.out.println("No se encontró la clave DNS");
    }

    // 👉 Método para modificar un Proxy
    public static void modifyProxy(String oldProxy, String newProxy) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                List<String> proxies = new ArrayList<>(Arrays.asList(current.split(";")));
                int index = proxies.indexOf(oldProxy);
                if (index != -1) {
                    proxies.set(index, newProxy);
                    lines.set(i, "PROXYS=" + String.join(";", proxies));
                    Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("Proxy modificado: " + oldProxy + " -> " + newProxy);
                } else {
                    System.out.println("Proxy a modificar no encontrado: " + oldProxy);
                }
                return;
            }
        }
        System.out.println("No se encontró la clave PROXYS");
    }

    // 👉 Método para modificar un DNS
    public static void modifyDNS(String oldDNS, String newDNS) throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                List<String> dnsList = new ArrayList<>(Arrays.asList(current.split(";")));
                int index = dnsList.indexOf(oldDNS);
                if (index != -1) {
                    dnsList.set(index, newDNS);
                    lines.set(i, "DNS=" + String.join(";", dnsList));
                    Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("DNS modificado: " + oldDNS + " -> " + newDNS);
                } else {
                    System.out.println("DNS a modificar no encontrado: " + oldDNS);
                }
                return;
            }
        }
        System.out.println("No se encontró la clave DNS");
    }

    // 👉 Método para leer todos los Proxys
    public static List<String> getProxies() throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                if (!current.trim().isEmpty()) {
                    return new ArrayList<>(Arrays.asList(current.split(";")));
                }
            }
        }
        return new ArrayList<>();
    }

    // 👉 Método para leer todos los DNS
    public static List<String> getDNSList() throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                if (!current.trim().isEmpty()) {
                    return new ArrayList<>(Arrays.asList(current.split(";")));
                }
            }
        }
        return new ArrayList<>();
    }

    // 👉 Método para abrir ventana y copiar instalador
    public void chooseAndCopyInstaller(Stage stage) throws IOException {
        File setupsFolder = new File(SETUPS_DIR);
        if (!setupsFolder.exists()) {
            setupsFolder.mkdirs();
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar instalador");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Instaladores", "*.exe", "*.msi", "*.zip"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) return;

        // --- Ventana de progreso ---
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(stage);
        progressStage.setTitle("Copiando archivo...");
        progressStage.setResizable(false);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        Label statusLabel = new Label("Iniciando copia...");
        Label fileNameLabel = new Label("Archivo: " + selectedFile.getName());
        Label sizeLabel = new Label(formatSize(selectedFile.length()));

        VBox layout = new VBox(15, statusLabel, progressBar, fileNameLabel, sizeLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        progressStage.setScene(new Scene(layout, 400, 200));

        // --- Task ---
        Task<Void> copyTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Path source = selectedFile.toPath();
                Path target = Paths.get(SETUPS_DIR, selectedFile.getName());

                long totalBytes = Files.size(source);

                try (InputStream is = Files.newInputStream(source);
                     OutputStream os = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    byte[] buffer = new byte[8192];
                    long bytesCopied = 0;
                    int bytesRead;

                    while (!isCancelled() && (bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        bytesCopied += bytesRead;

                        updateProgress(bytesCopied, totalBytes);
                        updateMessage(String.format("Copiando... %.1f%%", (bytesCopied * 100.0) / totalBytes));
                    }

                    updateProgress(1, 1);
                    updateMessage("¡Copia completada!");
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                progressStage.close();
                    if (onCopySuccess != null) {
                        onCopySuccess.run();   // 🔥 Notifica al controlador
                    }
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Copia exitosa");
                success.setHeaderText(null);
                success.setContentText("Instalador copiado a:\n" + Paths.get(SETUPS_DIR, selectedFile.getName()));
                success.showAndWait();
            }

            @Override
            protected void failed() {
                progressStage.close();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error en la copia");
                error.setHeaderText(null);
                error.setContentText("Error: " + getException().getMessage());
                error.showAndWait();
            }
        };

        // Enlazar UI con Task
        progressBar.progressProperty().bind(copyTask.progressProperty());
        statusLabel.textProperty().bind(copyTask.messageProperty());

        // Ejecutar
        new Thread(copyTask).start();
        progressStage.show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

            // Configurar botón de cancelar


    // 👉 Método para abrir ventana y copiar carpeta completa
    public static void chooseAndCopyFolder(Stage stage) throws IOException {
        File setupsFolder = new File(SETUPS_DIR);
        if (!setupsFolder.exists()) {
            setupsFolder.mkdirs();
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar carpeta a copiar");

        File selectedDir = directoryChooser.showDialog(stage);

        if (selectedDir != null) {
            Path sourceDir = selectedDir.toPath();
            Path targetDir = Paths.get(SETUPS_DIR, selectedDir.getName());

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @NotNull
                @Override
                public FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    Path targetPath = targetDir.resolve(sourceDir.relativize(dir));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @NotNull
                @Override
                public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Path targetPath = targetDir.resolve(sourceDir.relativize(file));
                    Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println("Carpeta copiada a: " + targetDir.toString());
        } else {
            System.out.println("No se seleccionó ninguna carpeta.");
        }
    }

    // Método para imprimir el contenido del archivo .env
    public static void printEnvContents() throws IOException {
        ensureEnvFileExists();
        Path path = Paths.get(ENV_FILE);
        if (Files.exists(path)) {
            System.out.println("\n=== CONTENIDO DE .env ===");
            List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
            for (String line : lines) {
                System.out.println(line);
            }
            System.out.println("=========================\n");
        }
    }

    // Ejemplo de uso con mejor diagnóstico
    public static void main(String[] args) {
        try {
            System.out.println("=== PRUEBA DE CLASE MAINTENANCE ===");
            System.out.println("Archivo .env en: " + ENV_FILE);

            // Mostrar contenido inicial
            printEnvContents();

            // Prueba de agregar
            System.out.println("\n--- Agregando Proxy ---");
            addProxy("10.99.0.50:8080");
            printEnvContents();

            System.out.println("\n--- Agregando DNS ---");
            addDNS("8.8.8.8");
            printEnvContents();

            System.out.println("\n--- Agregando segundo Proxy ---");
            addProxy("192.168.1.100:3128");
            printEnvContents();

            // Prueba de modificar
            System.out.println("\n--- Modificando Proxy ---");
            modifyProxy("10.99.0.50:8080", "10.99.0.51:8080");
            printEnvContents();

            System.out.println("\n--- Modificando DNS ---");
            modifyDNS("8.8.8.8", "1.1.1.1");
            printEnvContents();

            // Prueba de eliminar
            System.out.println("\n--- Eliminando Proxy ---");
            removeProxy("192.168.1.100:3128");
            printEnvContents();

            System.out.println("\n--- Eliminando DNS ---");
            removeDNS("1.1.1.1");
            printEnvContents();

            // Probar métodos de lectura
            System.out.println("\n--- Listando Proxys ---");
            List<String> proxies = getProxies();
            System.out.println("Proxys encontrados: " + proxies);

            System.out.println("\n--- Listando DNS ---");
            List<String> dnsList = getDNSList();
            System.out.println("DNS encontrados: " + dnsList);

        } catch (IOException e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
}