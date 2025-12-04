package com.appmanager.appmanager.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MetadataExtractor {

    // Variables para controlar carpetas ya procesadas
    private static final Set<String> processedSpecialFolders = new HashSet<>();

    public static Map<String, Map<String, String>> getExecutableMetadataFromFolder(String folderPath) {
        return getExecutableMetadataFromFolder(folderPath, true);
    }

    public static Map<String, Map<String, String>> getExecutableMetadataFromFolder(String folderPath, boolean recursive) {
        // Resetear las variables de control al inicio de cada búsqueda
        processedSpecialFolders.clear();

        File folder = new File(folderPath);
        Map<String, Map<String, String>> allMetadata = new HashMap<>();

        // Verificar que la carpeta existe y es accesible
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("La carpeta no existe o no es accesible: " + folderPath);
            return allMetadata;
        }

        // Buscar archivos con reglas especiales
        List<File> executableFiles = findExecutableFilesWithSpecialRules(folder, recursive, folderPath);

        if (executableFiles.isEmpty()) {
            System.out.println("No se encontraron archivos .exe o .msi en la carpeta: " + folderPath);
            return allMetadata;
        }

        System.out.println("Procesando " + executableFiles.size() + " archivos ejecutables...");

        for (File file : executableFiles) {
            String relativePath = getRelativePath(file, folderPath);
            System.out.println("Analizando: " + relativePath);

            Map<String, String> metadata = extractSingleFileMetadata(file, folderPath);

            // Usar la ruta relativa como clave para evitar conflictos de nombres duplicados
            allMetadata.put(relativePath, metadata);
        }

        return allMetadata;
    }

    /**
     * Busca archivos con reglas especiales
     */
    private static List<File> findExecutableFilesWithSpecialRules(File folder, boolean recursive, String baseFolderPath) {
        List<File> executableFiles = new ArrayList<>();

        if (recursive) {
            // Búsqueda recursiva con manejo especial de carpetas
            findExecutableFilesRecursiveWithRules(folder, executableFiles, baseFolderPath);
        } else {
            // Búsqueda no recursiva (solo carpeta actual)
            File[] files = folder.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".exe") || lowerName.endsWith(".msi");
            });

            if (files != null) {
                executableFiles.addAll(Arrays.asList(files));
            }
        }

        return executableFiles;
    }

    /**
     * Método recursivo con reglas especiales
     */
    private static void findExecutableFilesRecursiveWithRules(File folder, List<File> executableFiles, String baseFolderPath) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }

        // Evitar procesar carpetas especiales ya procesadas
        String folderKey = folder.getAbsolutePath();
        if (processedSpecialFolders.contains(folderKey)) {
            System.out.println("Carpeta especial ya procesada, omitiendo: " + folder.getName());
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Verificar el nombre de la carpeta (no la ruta completa)
                String folderName = file.getName().toLowerCase();

                // REGLA 1: Si encontramos una carpeta llamada "SIAR", IGNORARLA COMPLETAMENTE
                if (folderName.equals("siar")) {
                    System.out.println("⚠ Carpeta SIAR detectada. IGNORANDO completamente: " + file.getAbsolutePath());
                    // Marcar como procesada y NO entrar
                    processedSpecialFolders.add(file.getAbsolutePath());
                    continue;
                }

                // REGLA 2: Si encontramos una carpeta llamada "DISK1" en ubicación raíz
                if (folderName.equals("disk1")) {
                    // Verificar si esta carpeta DISK1 está en una ubicación "raíz" (no muy profunda)
                    String relativePath = getRelativePath(file, baseFolderPath);
                    int depth = countPathDepth(relativePath);

                    // Si DISK1 está a poca profundidad (ej.: directamente en Setups o a 1-2 niveles)
                    if (depth <= 3) {
                        System.out.println("📁 Carpeta DISK1 en ubicación raíz detectada: " + file.getAbsolutePath());

                        // Buscar SIAR.msi en esta carpeta DISK1
                        boolean foundSiar = findAndAddSIARMsiInDisk1(file, executableFiles);

                        if (foundSiar) {
                            System.out.println("✅ SIAR.msi encontrado en DISK1 de raíz.");
                            // Marcar esta carpeta DISK1 como procesada
                            processedSpecialFolders.add(file.getAbsolutePath());
                            // SALIR de esta carpeta DISK1 pero CONTINUAR con el resto
                            continue;
                        } else {
                            System.out.println("❌ SIAR.msi NO encontrado en DISK1. Continuando búsqueda normal...");
                            // Si no encontró SIAR.msi, procesar normalmente
                        }
                    }
                }

                // REGLA 3: Si es la carpeta GUI de SAP Corpoelec
                String relativePath = getRelativePath(file, baseFolderPath).toLowerCase();
                if (isSAPCorpoelecGUIFolder(relativePath)) {
                    System.out.println("📁 Carpeta SAP Corpoelec/GUI detectada: " + file.getAbsolutePath());
                    findAndAddSetupAllExe(file, executableFiles);
                    // Marcar como procesada y NO entrar en subcarpetas
                    processedSpecialFolders.add(file.getAbsolutePath());
                    continue;
                }

                // Para otras carpetas, continuar recursión normalmente
                findExecutableFilesRecursiveWithRules(file, executableFiles, baseFolderPath);

            } else {
                // Para archivos en la carpeta actual
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".exe") || fileName.endsWith(".msi")) {
                    // Verificar que no estamos en una carpeta especial ya procesada
                    String parentPath = file.getParent();
                    if (parentPath == null || !processedSpecialFolders.contains(parentPath)) {
                        executableFiles.add(file);
                    }
                }
            }
        }
    }

    /**
     * Cuenta la profundidad de una ruta (número de directorios)
     */
    private static int countPathDepth(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return 0;
        }

        // Contar separadores de directorio
        String normalizedPath = relativePath.replace("\\", "/");
        String[] parts = normalizedPath.split("/");

        // Filtrar partes vacías
        int depth = 0;
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                depth++;
            }
        }

        return depth;
    }

    /**
     * Verifica si es la carpeta GUI de SAP Corpoelec
     */
    private static boolean isSAPCorpoelecGUIFolder(String relativePath) {
        if (relativePath == null) return false;

        // Normalizar separadores de ruta
        String normalizedPath = relativePath.replace("\\", "/");

        // Buscar patrones que indiquen la carpeta GUI de SAP Corpoelec
        return normalizedPath.contains("sap") &&
                normalizedPath.contains("corpoelec") &&
                normalizedPath.contains("gui");
    }

    /**
     * Busca y agrega SIAR.msi en carpeta DISK1
     * Retorna true si encontró SIAR.msi
     */
    private static boolean findAndAddSIARMsiInDisk1(File disk1Folder, List<File> executableFiles) {
        System.out.println("   🔍 Buscando SIAR.msi en: " + disk1Folder.getAbsolutePath());

        // Listar todos los archivos para debug
        File[] allFiles = disk1Folder.listFiles();
        if (allFiles != null && allFiles.length > 0) {
            System.out.println("   📄 Contenido de DISK1:");
            for (File f : allFiles) {
                System.out.println("      - " + f.getName() + (f.isDirectory() ? " [carpeta]" : ""));
            }
        } else {
            System.out.println("   📄 Carpeta DISK1 está vacía");
        }

        // Buscar SIAR.msi específicamente
        File[] siarFiles = disk1Folder.listFiles((dir, name) ->
                name.equalsIgnoreCase("SIAR.msi")
        );

        if (siarFiles != null && siarFiles.length > 0) {
            executableFiles.add(siarFiles[0]);
            System.out.println("   ✅ SIAR.msi encontrado en DISK1");
            return true;
        } else {
            System.out.println("   ❌ SIAR.msi NO encontrado en DISK1");
            return false;
        }
    }

    /**
     * Busca y agrega solo SetupAll.exe en carpeta GUI
     */
    private static void findAndAddSetupAllExe(File guiFolder, List<File> executableFiles) {
        System.out.println("   🔍 Buscando SetupAll.exe en carpeta GUI...");

        File[] setupAllFiles = guiFolder.listFiles((dir, name) ->
                name.equalsIgnoreCase("SetupAll.exe")
        );

        if (setupAllFiles != null && setupAllFiles.length > 0) {
            executableFiles.add(setupAllFiles[0]);
            System.out.println("   ✅ SetupAll.exe encontrado en carpeta GUI");
        } else {
            System.out.println("   ❌ SetupAll.exe NO encontrado en carpeta GUI");

            // Opcional: listar lo que sí hay en la carpeta GUI
            File[] allFiles = guiFolder.listFiles();
            if (allFiles != null && allFiles.length > 0) {
                System.out.println("   📄 Contenido de carpeta GUI:");
                for (File f : allFiles) {
                    if (f.getName().toLowerCase().endsWith(".exe")) {
                        System.out.println("      - " + f.getName() + " (archivo .exe encontrado)");
                    }
                }
            }
        }
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
            metadata.put("ParentFolder", file.getParent());

            // Calcular tamaño (siempre funciona)
            double sizeMB = file.length() / (1024.0 * 1024.0);
            metadata.put("SizeMB", String.format("%.2f", sizeMB));
            metadata.put("SizeBytes", String.valueOf(file.length()));

            // Verificar si es un archivo especial
            String relativePath = getRelativePath(file, baseFolderPath).toLowerCase();

            // Marcar como archivo especial de SAP Corpoelec/GUI
            if (file.getParent() != null &&
                    isSAPCorpoelecGUIFolder(getRelativePath(file.getParentFile(), baseFolderPath)) &&
                    file.getName().equalsIgnoreCase("SetupAll.exe")) {
                metadata.put("IsSAPCorpoelecGUI", "true");
                metadata.put("SpecialFile", "SetupAll.exe en carpeta GUI");
            }

            // Marcar como archivo especial de SIAR en DISK1 de raíz
            if (file.getParent() != null &&
                    file.getParentFile().getName().equalsIgnoreCase("DISK1") &&
                    file.getName().equalsIgnoreCase("SIAR.msi")) {
                metadata.put("IsRootDisk1SIAR", "true");
                metadata.put("SpecialFile", "SIAR.msi en DISK1 de raíz");

                // Para SIAR.msi, usar extracción robusta con manejo de errores
                extractSIARMsiMetadataWithFallback(file, metadata);
            } else {
                // Extracción normal para otros archivos
                if (file.getName().toLowerCase().endsWith(".exe")) {
                    extractExeMetadata(file, metadata);
                } else if (file.getName().toLowerCase().endsWith(".msi")) {
                    extractMsiMetadata(file, metadata);
                }
            }

        } catch (Exception e) {
            System.err.println("Error analizando " + file.getName() + ": " + e.getMessage());
            metadata.put("Error", e.getMessage());

            // Aun así guardar información básica
            metadata.put("FileVersion", "N/A (Error: " + e.getMessage() + ")");
            metadata.put("ProductName", "N/A (Error en extracción)");
        }

        // Verificar que tenemos todos los campos necesarios
        ensureRequiredFields(metadata, file);

        return metadata;
    }

    /**
     * Extrae metadatos de SIAR.msi con manejo robusto de errores
     */
    private static void extractSIARMsiMetadataWithFallback(File file, Map<String, String> metadata) {
        System.out.println("   🔧 Extrayendo metadatos de SIAR.msi (con manejo de errores)...");

        try {
            // Primero intentar extracción normal
            extractMsiMetadata(file, metadata);

            // Verificar si la extracción fue exitosa
            if (!metadata.containsKey("MSI_ProductVersion") ||
                    metadata.get("MSI_ProductVersion").equals("N/A") ||
                    metadata.get("MSI_ProductVersion").contains("Error")) {

                System.out.println("   ⚠ Extracción normal falló, intentando método alternativo...");

                // Método alternativo simplificado
                String simpleCommand = getString(file);

                executeSimplePowerShellForSIAR(simpleCommand, metadata);

                // Si aún falla, marcar como problema
                if (!metadata.containsKey("ProductVersion") || metadata.get("ProductVersion").contains("Error")) {
                    metadata.put("SIAR_ExtractionNote", "Extracción limitada - archivo puede estar corrupto o protegido");
                }
            }

        } catch (Exception e) {
            System.err.println("   ❌ Error en extracción de SIAR.msi: " + e.getMessage());
            metadata.put("SIAR_ExtractionError", e.getMessage());
            metadata.put("FileVersion", "N/A (Error en SIAR.msi)");
            metadata.put("ProductName", "SIAR 2005 (estimado)");
        }
    }

    @NotNull
    private static String getString(File file) {
        String escapedPath = file.getAbsolutePath().replace("'", "''");
        return "try { " +
                "    $installer = New-Object -ComObject WindowsInstaller.Installer; " +
                "    $db = $installer.OpenDatabase('" + escapedPath + "', 0); " +
                "    $query = 'SELECT Value FROM Property WHERE Property = \"ProductVersion\"'; " +
                "    $view = $db.OpenView($query); " +
                "    $view.Execute(); " +
                "    $record = $view.Fetch(); " +
                "    if ($record) { " +
                "        $version = $record.StringData(1); " +
                "        Write-Output 'ProductVersion:' + $version; " +
                "    } " +
                "    $view.Close(); " +
                "} catch { " +
                "    Write-Output 'Error:Fallback extraction failed'; " +
                "}";
    }

    /**
     * Ejecuta PowerShell simplificado para SIAR
     */
    private static void executeSimplePowerShellForSIAR(String command, Map<String, String> metadata) {
        try {
            String[] commands = {"powershell", "-Command", command};
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (!value.isEmpty() && !value.equals("null")) {
                            metadata.put(key, value);
                        }
                    }
                }
            }

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                metadata.put("SIAR_Timeout", "Análisis tomó demasiado tiempo");
            }

            reader.close();

        } catch (Exception e) {
            metadata.put("SIAR_PowerShellError", e.getMessage());
        }
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
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
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
        // Campos básicos obligatorios (SIEMPRE presentes)
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
        if (!metadata.containsKey("SizeBytes")) {
            metadata.put("SizeBytes", String.valueOf(file.length()));
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

        // Campos especiales para SIAR en DISK1 (si aplica)
        String fileName = file.getName().toLowerCase();
        if (fileName.equals("siar.msi") && file.getParent() != null &&
                file.getParentFile().getName().equalsIgnoreCase("DISK1")) {
            metadata.put("IsRootDisk1SIAR", "true");
            metadata.put("SpecialFile", "SIAR.msi en DISK1 de raíz");
        }
    }

    /**
     * Método de ejemplo de uso
     */
    public static void main(String[] args) {
        // Ejemplo: Analizar desde la carpeta Setups
        String setupsPath = "C:\\Setups";

        System.out.println("=".repeat(80));
        System.out.println("🔍 INICIANDO ANÁLISIS DESDE: " + setupsPath);
        System.out.println("=".repeat(80));
        System.out.println("\n📋 REGLAS ESPECIALES ACTIVAS:");
        System.out.println("  1. ❌ IGNORAR completamente cualquier carpeta llamada 'SIAR'");
        System.out.println("  2. 📁 En DISK1 cerca de la raíz: Buscar SOLO SIAR.msi");
        System.out.println("  3. 📁 En SAP Corpoelec/GUI: Buscar SOLO SetupAll.exe");
        System.out.println("  4. 🔄 Después de procesar carpetas especiales, CONTINUAR con el resto");
        System.out.println("=".repeat(80) + "\n");

        Map<String, Map<String, String>> results = getExecutableMetadataFromFolder(setupsPath);

        // Mostrar resumen
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 RESUMEN DEL ANÁLISIS");
        System.out.println("=".repeat(80));

        int totalFiles = results.size();
        int sapFiles = 0;
        int siarFiles = 0;
        int regularFiles = 0;

        for (Map.Entry<String, Map<String, String>> entry : results.entrySet()) {
            Map<String, String> meta = entry.getValue();

            if ("true".equals(meta.get("IsSAPCorpoelecGUI"))) {
                sapFiles++;
            } else if ("true".equals(meta.get("IsRootDisk1SIAR"))) {
                siarFiles++;
            } else {
                regularFiles++;
            }
        }

        System.out.println("📈 ESTADÍSTICAS:");
        System.out.println("  • Total archivos analizados: " + totalFiles);
        System.out.println("  • Archivos SAP Corpoelec/GUI: " + sapFiles);
        System.out.println("  • Archivos SIAR en DISK1: " + siarFiles);
        System.out.println("  • Archivos regulares: " + regularFiles);

        // Mostrar detalles de archivos especiales
        if (sapFiles > 0 || siarFiles > 0) {
            System.out.println("\n⭐ ARCHIVOS ESPECIALES ENCONTRADOS:");

            for (Map.Entry<String, Map<String, String>> entry : results.entrySet()) {
                Map<String, String> meta = entry.getValue();
                String filePath = entry.getKey();

                if ("true".equals(meta.get("IsSAPCorpoelecGUI"))) {
                    System.out.println("\n  🟢 SAP Corpoelec/GUI:");
                    System.out.println("    • Archivo: " + filePath);
                    System.out.println("    • Tamaño: " + meta.get("SizeMB") + " MB");
                    System.out.println("    • Producto: " + meta.getOrDefault("ProductName", "N/A"));
                    System.out.println("    • Versión: " + meta.getOrDefault("FileVersion", "N/A"));
                }

                if ("true".equals(meta.get("IsRootDisk1SIAR"))) {
                    System.out.println("\n  🔵 SIAR en DISK1:");
                    System.out.println("    • Archivo: " + filePath);
                    System.out.println("    • Tamaño: " + meta.get("SizeMB") + " MB");
                    System.out.println("    • Producto: " + meta.getOrDefault("ProductName", "N/A"));
                    System.out.println("    • Versión: " + meta.getOrDefault("FileVersion", "N/A"));

                    if (meta.containsKey("SIAR_ExtractionError")) {
                        System.err.println("    ⚠ Error: " + meta.get("SIAR_ExtractionError"));
                        System.err.println("    💡 Se guardó información básica del archivo problemático");
                    }
                }
            }
        }

        // Mostrar archivos regulares si los hay
        if (regularFiles > 0) {
            System.out.println("\n📄 ARCHIVOS REGULARES ENCONTRADOS (" + regularFiles + "):");
            for (Map.Entry<String, Map<String, String>> entry : results.entrySet()) {
                Map<String, String> meta = entry.getValue();
                if (!"true".equals(meta.get("IsSAPCorpoelecGUI")) &&
                        !"true".equals(meta.get("IsRootDisk1SIAR"))) {
                    System.out.println("  • " + entry.getKey() + " (" + meta.get("SizeMB") + " MB)");
                }
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ ANÁLISIS COMPLETADO");
        System.out.println("=".repeat(80));
    }
}