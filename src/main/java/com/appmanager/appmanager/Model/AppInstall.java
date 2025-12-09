package com.appmanager.appmanager.Model;

import com.appmanager.appmanager.Controller.DashboardController;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppInstall extends Component {
    // Carpeta externa "Setups" junto al JAR
    private final Path setupsDir;
    public DashboardController dc;

    public AppInstall() {
        // Directorio actual donde se ejecuta el JAR
        setupsDir = Paths.get(System.getProperty("user.dir"), "Setups");
        if (!Files.exists(setupsDir)) {
            throw new IllegalStateException("No se encontró la carpeta Setups en: " + setupsDir.toAbsolutePath());
        }
    }

    public String installApp(String appName, String absolutePath) throws IOException, InterruptedException {
        Path installer = setupsDir.resolve(absolutePath);
        if (!Files.exists(installer)) {
            throw new IOException("Instalador no encontrado: " + installer);
        }

        System.out.println(appName);

        // Caso especial SetupAll.exe con archivo INI
        if (appName.equals("SetupAll.exe")) {
            Path iniFile = setupsDir.resolve("CustomInstall.ini");
            if (!Files.exists(iniFile)) {
                throw new IOException("Archivo INI no encontrado: " + iniFile);
            }

            ProcessBuilder pbAll = new ProcessBuilder(
                    installer.toAbsolutePath().toString(),
                    "/Silent",
                    "/NoDlg",
                    "/IniFile=" + iniFile.toAbsolutePath()
            );
            pbAll.inheritIO();
            Process p = pbAll.start();
            int exitCode = p.waitFor();
            return exitCode == 0
                    ? appName + " Instalado Exitosamente\nStatus " + exitCode
                    : appName + " No se pudo instalar\nStatus " + exitCode;
        }

        // Instalar MSI
        if (installer.toString().endsWith(".msi")) {
            ProcessBuilder pbMsi = new ProcessBuilder(
                    "msiexec",
                    "/i",
                    installer.toAbsolutePath().toString(),
                    "/qn"
            );
            pbMsi.inheritIO();
            Process p = pbMsi.start();
            int exitCode = p.waitFor();
            return exitCode == 0
                    ? appName + " Instalado Exitosamente\nStatus " + exitCode
                    : appName + " No se pudo instalar\nStatus " + exitCode;

            // Instalar EXE
        } else if (installer.toString().endsWith(".exe")) {
            ProcessBuilder pb = new ProcessBuilder(installer.toAbsolutePath().toString(), "/s");
            System.out.println(absolutePath);
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0
                    ? appName + " Instalado Exitosamente\nStatus " + exitCode
                    : appName + " No se pudo instalar\nStatus " + exitCode;
        }

        return appName + " Tipo de instalador no soportado.";
    }
}