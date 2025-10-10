package com.appmanager.appmanager.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataExtractor {

    public static Map<String, Map<String, String>> getExecutableMetadataFromFolder(String folderPath) {
        File folder = new File(folderPath);
        Map<String, Map<String, String>> allMetadata = new HashMap<>();

        // Verificar que la carpeta existe y es accesible
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("La carpeta no existe o no es accesible: " + folderPath);
            return allMetadata;
        }

        // Filtrar archivos .exe y .msi
        File[] files = folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".exe") || lowerName.endsWith(".msi");
        });

        if (files == null || files.length == 0) {
            System.out.println("No se encontraron archivos .exe o .msi en la carpeta: " + folderPath);
            return allMetadata;
        }

        System.out.println("Procesando " + files.length + " archivos ejecutables...");

        for (File file : files) {
            System.out.println("Analizando: " + file.getName());
            Map<String, String> metadata = extractSingleFileMetadata(file, folderPath);
            allMetadata.put(file.getName(), metadata);
        }

        return allMetadata;
    }

    /**
     * Extrae metadatos de un archivo individual (.exe o .msi)
     */
    private static Map<String, String> extractSingleFileMetadata(File file, String baseFolderPath) {
        Map<String, String> metadata = new HashMap<>();

        try {
            // Agregar información básica del archivo
            metadata.put("FileName", file.getName());
            metadata.put("AbsolutePath", file.getAbsolutePath());
            metadata.put("RelativePath", getRelativePath(file, baseFolderPath));
            metadata.put("FileExtension", getFileExtension(file.getName()));
            metadata.put("LastModified", new Date(file.lastModified()).toString());

            // Calcular tamaño
            double sizeMB = file.length() / (1024.0 * 1024.0);
            metadata.put("SizeMB", String.format("%.2f", sizeMB));
            metadata.put("SizeBytes", String.valueOf(file.length()));

            // Extraer metadatos específicos según el tipo de archivo
            if (file.getName().toLowerCase().endsWith(".exe")) {
                extractExeMetadata(file, metadata);
            } else if (file.getName().toLowerCase().endsWith(".msi")) {
                extractMsiMetadata(file, metadata);
            }

        } catch (Exception e) {
            System.err.println("Error analizando " + file.getName() + ": " + e.getMessage());
            metadata.put("Error", e.getMessage());
        }

        // Verificar que tenemos todos los campos necesarios
        ensureRequiredFields(metadata, file);

        return metadata;
    }

    /**
     * Extrae metadatos específicos de archivos .exe usando PowerShell
     */
    private static void extractExeMetadata(File file, Map<String, String> metadata) {
        try {
            String escapedPath = file.getAbsolutePath().replace("'", "''");

            String powerShellCommand =
                    "$file = Get-Item -LiteralPath '" + escapedPath + "'; " +
                            "$shell = New-Object -ComObject Shell.Application; " +
                            "$folder = $shell.Namespace($file.DirectoryName); " +
                            "$item = $folder.ParseName($file.Name); " +
                            "Write-Output ('FileVersion:' + $file.VersionInfo.FileVersion); " +
                            "Write-Output ('ProductVersion:' + $file.VersionInfo.ProductVersion); " +
                            "Write-Output ('FileDescription:' + $file.VersionInfo.FileDescription); " +
                            "Write-Output ('ProductName:' + $file.VersionInfo.ProductName); " +
                            "Write-Output ('CompanyName:' + $file.VersionInfo.CompanyName); " +
                            "Write-Output ('Copyright:' + $file.VersionInfo.LegalCopyright); " +
                            "Write-Output ('OriginalFilename:' + $file.VersionInfo.OriginalFilename); " +
                            "Write-Output ('IsDebug:' + $file.VersionInfo.IsDebug); " +
                            "Write-Output ('IsPreRelease:' + $file.VersionInfo.IsPreRelease); " +
                            "Write-Output ('IsPatched:' + $file.VersionInfo.IsPatched); " +
                            "Write-Output ('IsPrivateBuild:' + $file.VersionInfo.IsPrivateBuild); " +
                            "Write-Output ('IsSpecialBuild:' + $file.VersionInfo.IsSpecialBuild)";

            executePowerShellCommand(powerShellCommand, metadata, file.getName());

        } catch (Exception e) {
            System.err.println("Error extrayendo metadatos EXE de " + file.getName() + ": " + e.getMessage());
            metadata.put("EXE_Error", e.getMessage());
        }
    }

    /**
     * Extrae metadatos específicos de archivos .msi usando Windows Installer
     */
    private static void extractMsiMetadata(File file, Map<String, String> metadata) {
        try {
            String escapedPath = file.getAbsolutePath().replace("'", "''");

            // Comando PowerShell para extraer propiedades MSI
            String powerShellCommand =
                    "try { " +
                            "    $windowsInstaller = New-Object -ComObject WindowsInstaller.Installer; " +
                            "    $database = $windowsInstaller.GetType().InvokeMember('OpenDatabase', 'InvokeMethod', $null, $windowsInstaller, @('" + escapedPath + "', 0)); " +
                            "    $view = $database.GetType().InvokeMember('OpenView', 'InvokeMethod', $null, $database, (\"SELECT * FROM Property\")); " +
                            "    $view.GetType().InvokeMember('Execute', 'InvokeMethod', $null, $view, $null); " +
                            "    $record = $view.GetType().InvokeMember('Fetch', 'InvokeMethod', $null, $view, $null); " +
                            "    while ($record -ne $null) { " +
                            "        $property = $record.GetType().InvokeMember('StringData', 'GetProperty', $null, $record, 1); " +
                            "        $value = $record.GetType().InvokeMember('StringData', 'GetProperty', $null, $record, 2); " +
                            "        Write-Output ('MSI_' + $property + ':' + $value); " +
                            "        $record = $view.GetType().InvokeMember('Fetch', 'InvokeMethod', $null, $view, $null); " +
                            "    } " +
                            "    $view.GetType().InvokeMember('Close', 'InvokeMethod', $null, $view, $null); " +
                            "} catch { " +
                            "    Write-Output 'MSI_Error:No se pudieron extraer propiedades MSI'; " +
                            "}";

            executePowerShellCommand(powerShellCommand, metadata, file.getName());

        } catch (Exception e) {
            System.err.println("Error extrayendo metadatos MSI de " + file.getName() + ": " + e.getMessage());
            metadata.put("MSI_Error", e.getMessage());
        }
    }

    /**
     * Ejecuta comandos de PowerShell y procesa la salida
     */
    private static void executePowerShellCommand(String command, Map<String, String> metadata, String fileName) {
        try {
            String[] commands = {"powershell", "-Command", command};

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true);

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

                        // Solo agregar si el valor no está vacío
                        if (!value.isEmpty() && !value.equals("null")) {
                            metadata.put(key, value);
                        }
                    }
                }
            }

            // Esperar con timeout
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("Timeout analizando: " + fileName);
                metadata.put("Timeout", "El análisis tomó demasiado tiempo");
            }

            reader.close();

        } catch (Exception e) {
            System.err.println("Error ejecutando PowerShell para " + fileName + ": " + e.getMessage());
            metadata.put("PowerShell_Error", e.getMessage());
        }
    }

    /**
     * Calcula la ruta relativa del archivo respecto a la carpeta base
     */
    private static String getRelativePath(File file, String baseFolderPath) {
        try {
            File baseFolder = new File(baseFolderPath);
            String absoluteFilePath = file.getAbsolutePath();
            String absoluteBasePath = baseFolder.getAbsolutePath();

            if (absoluteFilePath.startsWith(absoluteBasePath)) {
                return absoluteFilePath.substring(absoluteBasePath.length() + 1);
            } else {
                // Si no está dentro de la carpeta base, devolver solo el nombre
                return file.getName();
            }
        } catch (Exception e) {
            return file.getName();
        }
    }

    /**
     * Obtiene la extensión del archivo
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Asegura que todos los campos requeridos estén presentes
     */
    private static void ensureRequiredFields(Map<String, String> metadata, File file) {
        // Campos básicos obligatorios
        if (!metadata.containsKey("FileName")) {
            metadata.put("FileName", file.getName());
        }
        if (!metadata.containsKey("FileExtension")) {
            metadata.put("FileExtension", getFileExtension(file.getName()));
        }
        if (!metadata.containsKey("RelativePath")) {
            metadata.put("RelativePath", getRelativePath(file, file.getParent()));
        }
        if (!metadata.containsKey("SizeMB")) {
            double sizeMB = file.length() / (1024.0 * 1024.0);
            metadata.put("SizeMB", String.format("%.2f", sizeMB));
        }

        // Campos específicos de EXE
        if (file.getName().toLowerCase().endsWith(".exe")) {
            if (!metadata.containsKey("FileVersion")) {
                metadata.put("FileVersion", "N/A");
            }
            if (!metadata.containsKey("FileDescription")) {
                metadata.put("FileDescription", "N/A");
            }
            if (!metadata.containsKey("ProductName")) {
                metadata.put("ProductName", "N/A");
            }
        }

        // Campos específicos de MSI
        if (file.getName().toLowerCase().endsWith(".msi")) {
            if (!metadata.containsKey("MSI_ProductName")) {
                metadata.put("MSI_ProductName", "N/A");
            }
            if (!metadata.containsKey("MSI_ProductVersion")) {
                metadata.put("MSI_ProductVersion", "N/A");
            }
            if (!metadata.containsKey("MSI_Manufacturer")) {
                metadata.put("MSI_Manufacturer", "N/A");
            }
        }
    }
}