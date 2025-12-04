package com.appmanager.appmanager.Model;

import com.appmanager.appmanager.Controller.DashboardController;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppInstall extends Component {
    // Constante para almacenar la Ruta hacia los setups
    private final Path setupsDir;
    public DashboardController dc;

    public AppInstall() throws URISyntaxException{
        ClassLoader classLoader = AppInstall.class.getClassLoader();
        URL resource = classLoader.getResource("Setups");
        if (resource == null) {
            throw new IllegalStateException("No se encontró el recurso `Setups` en el classpath");
        }
        //Obteniendo ruta relativa de la carpeta "Setups" para que sea compatible con cualquier equipo
        setupsDir = Paths.get(resource.toURI());
    }

    public String installApp(String appName, String absolutePath ) throws IOException, InterruptedException {
        Path installer = setupsDir.resolve(absolutePath);
        if (!Files.exists(installer)) {
            throw new IOException("Instalador no encontrado: " + installer);
        }
        System.out.println(appName);
        if (appName.equals("SetupAll.exe")) {
            System.out.println(setupsDir.resolve("CustomInstall.ini").toAbsolutePath());
            ProcessBuilder pbAll = new ProcessBuilder(
                    installer.toAbsolutePath().toString(),
                    "/Silent",
                    "/NoDlg",
                    "/IniFile="+ setupsDir.resolve("CustomInstall.ini").toAbsolutePath()
            );
            pbAll.inheritIO();
            Process p = pbAll.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                return appName + " Instalado Exitosamente" + "\n Status" + exitCode;
            } else {
                System.out.println(appName + " No se pudo instalar" + "\n Status" + exitCode);
                return appName + " No se pudo instalar" + "\n Status" + exitCode;
            }

        }


        // Process Builder Para Instalar .msi
        if (installer.toString().contains(".msi")) {
            ProcessBuilder pbMsi = new ProcessBuilder(
                    "msiexec",
                    "/i",
                    installer.toAbsolutePath().toString(),
                    "/qn"
            );
            pbMsi.inheritIO();
            Process p = pbMsi.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                return appName + " Instalado Exitosamente" + "\n Status" + exitCode;
            } else {
                System.out.println(appName + " No se pudo instalar" + "\n Status" + exitCode);
                return appName + " No se pudo instalar" + "\n Status" + exitCode;
            }

            // Process Builder Para Instalar .exe
        } else if (installer.toString().contains(".exe")) {
            ProcessBuilder pb = new ProcessBuilder(installer.toAbsolutePath().toString(), "/s");
            System.out.println(absolutePath);
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                return appName + " Instalado Exitosamente" + "\n Status" + exitCode;
            } else {
                System.out.println(appName + " No se pudo instalar" + "\n Status" + exitCode);
                return appName + " No se pudo instalar" + "\n Status" + exitCode;
            }
        }
        return appName + " Tipo de instalador no soportado.";
    }
}






