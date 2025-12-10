package com.appmanager.appmanager.Utils;

import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Maintenance {

    // Ruta dinámica al archivo .env en el mismo directorio del .jar
    private static final String ENV_FILE = System.getProperty("user.dir") + File.separator + ".env";

    // Carpeta setups al mismo nivel que el .jar
    private static final String SETUPS_DIR = System.getProperty("user.dir") + File.separator + "setups";

    // Método genérico para actualizar una clave en el .env
    private static void updateEnvKey(String key, String newValue) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);
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

        Files.write(path, lines);
    }

    // 👉 Método para agregar un Proxy
    public static void addProxy(String proxy) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                String updated = current.isEmpty() ? proxy : current + ";" + proxy;
                updateEnvKey("PROXYS", updated);
                return;
            }
        }
        updateEnvKey("PROXYS", proxy);
    }

    // 👉 Método para agregar un DNS
    public static void addDNS(String dns) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                String updated = current.isEmpty() ? dns : current + ";" + dns;
                updateEnvKey("DNS", updated);
                return;
            }
        }
        updateEnvKey("DNS", dns);
    }

    // 👉 Método para eliminar un Proxy
    public static void removeProxy(String proxy) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                List<String> proxies = new ArrayList<>(Arrays.asList(current.split(";")));
                if (proxies.remove(proxy)) {
                    updateEnvKey("PROXYS", String.join(";", proxies));
                }
                return;
            }
        }
    }

    // 👉 Método para eliminar un DNS
    public static void removeDNS(String dns) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                List<String> dnsList = new ArrayList<>(Arrays.asList(current.split(";")));
                if (dnsList.remove(dns)) {
                    updateEnvKey("DNS", String.join(";", dnsList));
                }
                return;
            }
        }
    }

    // 👉 Método para modificar un Proxy
    public static void modifyProxy(String oldProxy, String newProxy) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("PROXYS=")) {
                String current = line.substring("PROXYS=".length());
                List<String> proxies = new ArrayList<>(Arrays.asList(current.split(";")));
                int index = proxies.indexOf(oldProxy);
                if (index != -1) {
                    proxies.set(index, newProxy);
                    updateEnvKey("PROXYS", String.join(";", proxies));
                }
                return;
            }
        }
    }

    // 👉 Método para modificar un DNS
    public static void modifyDNS(String oldDNS, String newDNS) throws IOException {
        Path path = Paths.get(ENV_FILE);
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            if (line.startsWith("DNS=")) {
                String current = line.substring("DNS=".length());
                List<String> dnsList = new ArrayList<>(Arrays.asList(current.split(";")));
                int index = dnsList.indexOf(oldDNS);
                if (index != -1) {
                    dnsList.set(index, newDNS);
                    updateEnvKey("DNS", String.join(";", dnsList));
                }
                return;
            }
        }
    }

    // 👉 Método para abrir ventana y copiar instalador
    public static void chooseAndCopyInstaller(Stage stage) throws IOException {
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

        if (selectedFile != null) {
            Path source = selectedFile.toPath();
            Path target = Paths.get(SETUPS_DIR, selectedFile.getName());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Instalador copiado a: " + target.toString());
        } else {
            System.out.println("No se seleccionó ningún archivo.");
        }
    }

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

    // Ejemplo de uso
    public static void main(String[] args) throws IOException {
        addProxy("10.99.0.50:8080");
        addDNS("8.8.8.8");

        modifyProxy("10.99.0.50:8080", "10.99.0.51:8080");
        modifyDNS("8.8.8.8", "1.1.1.1");

        removeProxy("10.99.0.51:8080");
        removeDNS("1.1.1.1");

        System.out.println("Archivo .env actualizado en: " + ENV_FILE);
    }
}