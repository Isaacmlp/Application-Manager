package com.appmanager.appmanager.Model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppInstall {
    private final Path setupsDir;

    public AppInstall() throws URISyntaxException {
        ClassLoader classLoader = AppInstall.class.getClassLoader();
        URL resource = classLoader.getResource("Setups");
        if (resource == null) {
            throw new IllegalStateException("No se encontró el recurso `Setups` en el classpath");
        }
        setupsDir = Paths.get(resource.toURI());
    }

    public void installApp(String appName) throws IOException, InterruptedException {
        Path installer = setupsDir.resolve(appName);
        if (!java.nio.file.Files.exists(installer)) {
            throw new IOException("Instalador no encontrado: " + installer);
        }
        ProcessBuilder pb = new ProcessBuilder(installer.toAbsolutePath().toString(), "/s");
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();
        System.out.println("Instalación finalizada con código: " + exitCode);
    }
}


