package com.appmanager.appmanager.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class EnvManager {

    // Ruta dinámica al archivo .env en el mismo directorio del .jar
    private static final String ENV_FILE = System.getProperty("user.dir") + File.separator + ".env";

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

    // Ejemplo de uso
    public static void main(String[] args) throws IOException {
        addProxy("10.99.0.50:8080");
        addDNS("8.8.8.8");
        System.out.println("Archivo .env actualizado en: " + ENV_FILE);
    }
}