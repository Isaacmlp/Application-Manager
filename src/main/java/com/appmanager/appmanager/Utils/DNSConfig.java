package com.appmanager.appmanager.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;

public class DNSConfig {

    // Cargar variables de entorno
    Dotenv dontenv = Dotenv.load();

    // Interfaz detectada dinámicamente
    String DEFAULT_INTERFACE;

    public DNSConfig() {
        this.DEFAULT_INTERFACE = detectWiredInterface();

        if (this.DEFAULT_INTERFACE == null) {
            System.out.println("⚠ No se encontró una interfaz Ethernet activa con IP.");
            this.DEFAULT_INTERFACE = "Ethernet"; // fallback opcional
        } else {
            System.out.println("✔ Interfaz detectada: " + this.DEFAULT_INTERFACE);
        }
    }

    public String[] getDNS() {
        return DNS;
    }

    // Cargar DNS desde las Variables de Entorno
    String[] DNS = Objects.requireNonNull(dontenv.get("DNS")).split(";");

    public String[] updateDNS() {
        try {
            List<String> dnsList = Maintenance.getDNSList();
            this.DNS = dnsList.toArray(new String[0]);
            return this.DNS;
        } catch (IOException e) {
            e.printStackTrace();
            return this.DNS != null ? this.DNS : new String[0];
        }
    }

    // Método para establecer DNS
    public void setDNS(String[] dns) {
        this.DNS = dns;
    }

    // Método para obtener array (si no existe)
    public String[] getDNSArray() {
        return this.DNS != null ? this.DNS : new String[0];
    }

    /**
     * Configura el DNS primario en la interfaz Ethernet
     */
    public boolean setPrimaryDNS(String primaryDNS) {
        System.out.println("netsh interface ip set dns name=" + DEFAULT_INTERFACE + " static " + primaryDNS);
        return runCommand("netsh interface ip set dns name=\"" + DEFAULT_INTERFACE + "\" static " + primaryDNS);
    }

    /**
     * Agrega un DNS secundario a la interfaz Ethernet
     */
    public boolean addSecondaryDNS(String secondaryDNS) {
        return runCommand("netsh interface ip add dns name=\"" + DEFAULT_INTERFACE + "\" " + secondaryDNS + " index=2");
    }

    /**
     * Restablece la configuración de DNS a automático (DHCP)
     */
    public boolean resetDNS() {
        return runCommand("netsh interface ip set dns name=\"" + DEFAULT_INTERFACE + "\" dhcp");
    }

    /**
     * Desactiva DNS manuales
     */
    public boolean disableDNS() {
        System.out.println("Desactivando DNS manuales en la interfaz: " + DEFAULT_INTERFACE);
        return resetDNS();
    }

    /**
     * Ejecuta un comando en Windows
     */
    private boolean runCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // 🔍 DETECCIÓN DINÁMICA DE INTERFAZ ETHERNET ACTIVA
    // -------------------------------------------------------------------------

    /**
     * Detecta la interfaz Ethernet conectada y con IP asignada
     */
    private String detectWiredInterface() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "netsh interface ipv4 show interfaces");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {

                // Ejemplo de línea:
                // 12 ...  Connected     Dedicated    Ethernet
                if (line.contains("Connected") && line.contains("Ethernet")) {

                    // Extraer nombre (última columna)
                    String[] parts = line.trim().split("\\s+");
                    String interfaceName = parts[parts.length - 1];

                    // Verificar si tiene IP
                    if (hasIPAddress(interfaceName)) {
                        return interfaceName;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Verifica si la interfaz tiene una IP asignada
     */
    private boolean hasIPAddress(String interfaceName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "netsh interface ip show config name=\"" + interfaceName + "\""
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("IP Address") || line.contains("IPv4 Address")) {
                    return true;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}