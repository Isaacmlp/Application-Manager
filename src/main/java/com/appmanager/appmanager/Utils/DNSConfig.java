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

    // Nombre fijo de la interfaz
    String DEFAULT_INTERFACE = dontenv.get("DEFAULT_INTERFACE");

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
     * @param primaryDNS Dirección IP del DNS primario
     * @return true si el comando se ejecutó correctamente
     */
    public boolean setPrimaryDNS(String primaryDNS) {
        System.out.println("netsh interface ip set dns name=" + DEFAULT_INTERFACE + " static " + primaryDNS);
        return runCommand("netsh interface ip set dns name=" + DEFAULT_INTERFACE + " static " + primaryDNS);    }

    /**
     * Agrega un DNS secundario a la interfaz Ethernet
     * @param secondaryDNS Dirección IP del DNS secundario
     * @return true si el comando se ejecutó correctamente
     */
    public boolean addSecondaryDNS(String secondaryDNS) {
        return runCommand("netsh interface ip add dns name=\"" + DEFAULT_INTERFACE + "\" " + secondaryDNS + " index=2");
    }

    /**
     * Restablece la configuración de DNS a automático (DHCP) en Ethernet
     * @return true si el comando se ejecutó correctamente
     */
    public boolean resetDNS() {
        return runCommand("netsh interface ip set dns name=\"" + DEFAULT_INTERFACE + "\" dhcp");
    }

    /**
     * Desactiva DNS manuales en Ethernet (equivalente a resetDNS)
     */
    public boolean disableDNS() {
        System.out.println("Desactivando DNS manuales en la interfaz: " + DEFAULT_INTERFACE);
        return resetDNS();
    }

    /**
     * Ejecuta un comando en Windows y devuelve si fue exitoso
     */
    private boolean runCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer salida del comando
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
}