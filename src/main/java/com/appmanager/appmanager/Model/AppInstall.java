package com.appmanager.appmanager.Model;

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

    public AppInstall() throws URISyntaxException {
        ClassLoader classLoader = AppInstall.class.getClassLoader();
        URL resource = classLoader.getResource("Setups");
        if (resource == null) {
            throw new IllegalStateException("No se encontró el recurso `Setups` en el classpath");
        }
        //Obteniendo ruta relativa de la carpeta "Setups" para que sea compatible con cualquier equipo
        setupsDir = Paths.get(resource.toURI());
    }

    public void installApp(String appName) throws IOException, InterruptedException {
        Path installer = setupsDir.resolve(appName);
        if (!Files.exists(installer)) {
            throw new IOException("Instalador no encontrado: " + installer);
        }
        ProcessBuilder pb = new ProcessBuilder(installer.toAbsolutePath().toString(), "/s");
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode == 0) {
            JOptionPane.showMessageDialog(this,appName + " Instalado Exitosamente" + "\n Status" + exitCode );
        } else {
            JOptionPane.showMessageDialog(this,appName + " No se pudo instalar" + "\n Status" + exitCode);
        }
    }
}


