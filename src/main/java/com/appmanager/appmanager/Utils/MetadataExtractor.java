package com.appmanager.appmanager.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetadataExtractor {

    public static Map<String, Map<String, String>> getExeMetadataFromFolder(String folderPath) {
        File folder = new File(folderPath);
        Map<String, Map<String, String>> allMetadata = new HashMap<>();

        // Verificar que la carpeta existe y es accesible
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("La carpeta no existe o no es accesible: " + folderPath);
            return allMetadata;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".exe"));
        if (files == null || files.length == 0) {
            System.out.println("No se encontraron archivos .exe en la carpeta: " + folderPath);
            return allMetadata;
        }

        System.out.println("Procesando " + files.length + " archivos .exe...");

        for (File file : files) {
            System.out.println("Analizando: " + file.getName());
            Map<String, String> metadata = extractSingleFileMetadata(file);
            allMetadata.put(file.getName(), metadata);
        }

        return allMetadata;
    }

    private static Map<String, String> extractSingleFileMetadata(File file) {
        Map<String, String> metadata = new HashMap<>();

        try {
            // Escapar la ruta correctamente para PowerShell
            String escapedPath = file.getAbsolutePath().replace("'", "''");

            // Comando PowerShell con manejo seguro de rutas
            String powerShellCommand =
                    "$file = Get-Item -LiteralPath '" + escapedPath + "'; " +
                            "$sizeMB = [math]::Round($file.Length / 1MB, 2); " +
                            "Write-Output ('Name:' + $file.Name); " +
                            "Write-Output ('FileVersion:' + $file.VersionInfo.FileVersion); " +
                            "Write-Output ('FileDescription:' + $file.VersionInfo.FileDescription); " +
                            "Write-Output ('SizeMB:' + $sizeMB)";

            String[] commands = {
                    "powershell",
                    "-Command",
                    powerShellCommand
            };

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true); // Combinar stdout y stderr

            Process process = processBuilder.start();

            // Leer la salida
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8")
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        metadata.put(key, value);
                    }
                }
            }

            // Esperar con timeout para evitar bloqueos
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("Timeout analizando: " + file.getName());
                metadata.put("Error", "Timeout en el análisis");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                System.err.println("PowerShell terminó con código: " + exitCode + " para: " + file.getName());
            }

            reader.close();

        } catch (Exception e) {
            System.err.println("Error analizando " + file.getName() + ": " + e.getMessage());
            metadata.put("Error", e.getMessage());
        }

        // Verificar que tenemos todos los campos necesarios
        ensureRequiredFields(metadata, file);

        return metadata;
    }

    private static void ensureRequiredFields(Map<String, String> metadata, File file) {
        if (!metadata.containsKey("Name")) {
            metadata.put("Name", file.getName());
        }
        if (!metadata.containsKey("FileVersion")) {
            metadata.put("FileVersion", "N/A");
        }
        if (!metadata.containsKey("FileDescription")) {
            metadata.put("FileDescription", "N/A");
        }
        if (!metadata.containsKey("SizeMB")) {
            double sizeMB = file.length() / (1024.0 * 1024.0);
            metadata.put("SizeMB", String.format("%.2f", sizeMB));
        }
    }
}