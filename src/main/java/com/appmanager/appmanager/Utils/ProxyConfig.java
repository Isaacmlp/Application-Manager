package com.appmanager.appmanager.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.github.cdimascio.dotenv.Dotenv;

public class ProxyConfig {
    Dotenv dotenv = Dotenv.load();

    // Ruta del registro entre comillas para evitar errores
    String registryPath = "\"" + dotenv.get("REGISTRY_PATH",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings") + "\"";

    String[] Proxys = Objects.requireNonNull(dotenv.get("PROXYS")).split(";");
    // Excepciones que quieres aplicar (separadas por ;)
    String defaultExceptions = "*.mired.local;localhost;192.168.*";

    public String[] getProxys() {
        return Proxys;
    }

    // Método para actualizar desde archivo .env
    public String[] updateProxys() {
        try {
            List<String> proxyList = Maintenance.getProxies();
            this.Proxys = proxyList.toArray(new String[0]);
            return this.Proxys;
        } catch (IOException e) {
            e.printStackTrace();
            return this.Proxys != null ? this.Proxys : new String[0];
        }
    }

    // Método para establecer proxys
    public void setProxys(String[] proxys) {
        this.Proxys = proxys;
    }

    // Método para obtener array (si no existe)
    public String[] getProxysArray() {
        return this.Proxys != null ? this.Proxys : new String[0];
    }

    // Habilitar el Uso de Proxys en el Sistema Operativo
    public void ConfigurarProxy(int numberProxyInArray) {
        ProcessBuilder setProxy = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "ProxyServer",
                        "/t", "REG_SZ",
                        "/d", Proxys[numberProxyInArray],
                        "/f"));

        ProcessBuilder enableProxy = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "ProxyEnable",
                        "/t", "REG_DWORD",
                        "/d", "1",
                        "/f"));

        ProcessBuilder disableAutoDetect = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "AutoDetect",
                        "/t", "REG_DWORD",
                        "/d", "0",
                        "/f"));

        // Solo escribir excepciones si no existen
        ProcessBuilder setProxyExceptions = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "ProxyOverride",
                        "/t", "REG_SZ",
                        "/d", defaultExceptions,
                        "/f"));

        // Refresco silencioso
        ProcessBuilder silentRefresh = new ProcessBuilder(
                "RUNDLL32.EXE", "inetcpl.cpl,ClearMyTracksByProcess", "255");

        try {
            setProxy.start().waitFor();
            setProxyExceptions.start().waitFor();
            enableProxy.start().waitFor();
            disableAutoDetect.start().waitFor();
            silentRefresh.start().waitFor();

            System.out.println("Configuracion de Proxy Exitosa");
            System.out.println("Proxy actual: " + Proxys[numberProxyInArray]);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error configurando proxy: " + e.getMessage());
        }
    }

    // 🔴 Nuevo método: Desactivar Proxy
    public String DesactivarProxy() {
        ProcessBuilder disableProxy = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "ProxyEnable",
                        "/t", "REG_DWORD",
                        "/d", "0",
                        "/f"));

        ProcessBuilder clearProxyServer = new ProcessBuilder(
                Arrays.asList("reg", "delete", registryPath,
                        "/v", "ProxyServer",
                        "/f"));

        ProcessBuilder clearProxyOverride = new ProcessBuilder(
                Arrays.asList("reg", "delete", registryPath,
                        "/v", "ProxyOverride",
                        "/f"));

        ProcessBuilder resetAutoDetect = new ProcessBuilder(
                Arrays.asList("reg", "add", registryPath,
                        "/v", "AutoDetect",
                        "/t", "REG_DWORD",
                        "/d", "1",
                        "/f"));

        ProcessBuilder silentRefresh = new ProcessBuilder(
                "RUNDLL32.EXE", "inetcpl.cpl,ClearMyTracksByProcess", "255");

        try {
            disableProxy.start().waitFor();
            clearProxyServer.start().waitFor();
            clearProxyOverride.start().waitFor();
            resetAutoDetect.start().waitFor();
            silentRefresh.start().waitFor();

            System.out.println("Proxy desactivado correctamente");
            return "Proxy desactivado correctamente";
        } catch (IOException | InterruptedException e) {
            System.out.println("Error desactivando proxy: " + e.getMessage());
            return "Error desactivando proxy: " + e.getMessage();
        }
    }

}