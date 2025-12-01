package com.appmanager.appmanager.Model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import com.appmanager.appmanager.Controller.DashboardController;
import io.github.cdimascio.dotenv.Dotenv;


public class ProxyConfig {
    static Dotenv dotenv = Dotenv.load();
    static DashboardController dc;


    static String[] Proxys = Objects.requireNonNull(dotenv.get("PROXYS")).split(";");
    static String registryPath = dotenv.get("REGISTRY_PATH");
    // Excepciones que quieres aplicar (separadas por ;)
    static String exceptions = "*.mired.local;localhost;192.168.*";


    public ProxyConfig(String[] proxys, String registrypath) {
        Proxys = proxys;
        registryPath = registrypath;
    }

    //Habilitar el Uso de Proxys en el Sistema Operativo
    public static void ConfigurarProxy(int numberProxyInArray) {
        ProcessBuilder enableProxy = new ProcessBuilder(
                                     Arrays.asList(
                                        "reg","add",registryPath,
                                        "/v","ProxyEnable",
                                        "/t","REG_DWORD",
                                        "/d", "1",
                                        "/f"
                                     )
        );

        ProcessBuilder disableAutoDetect = new ProcessBuilder(
            Arrays.asList(
                "reg","add",registryPath,
                "/v","AutoDetect",
                "/t","REG_DWORD",
                "/d","0",
                "/f"
            )
        );

        // Crear el ProcessBuilder para agregar/modificar ProxyOverride
        ProcessBuilder setProxyExceptions = new ProcessBuilder(
                Arrays.asList(
                        "reg","add",registryPath,
                        "/v","ProxyOverride",
                        "/t","REG_SZ",
                        "/d",exceptions,
                        "/f"
                )
        );


        ProcessBuilder setProxy = new ProcessBuilder(
            Arrays.asList(
                "reg","add",registryPath,
                "/v" ,"ProxyServer",
                "/d" ,Proxys[numberProxyInArray],
                "/f"
            )
        );

        try{
            disableAutoDetect.inheritIO();
            disableAutoDetect.start();

            setProxyExceptions.start().waitFor();

            dc.message("Ejecutando REG ADD para habilitar el proxy");
            System.out.println("Ejecutando REG ADD para habilitar el proxy");
            enableProxy.start().waitFor();

            dc.message("Ejecutando REG ADD para establecer la direccion del proxy");
            System.out.println("Ejecutando REG ADD para establecer la direccion del proxy");
            setProxy.start().waitFor();

            //Notificar al Sistema operativo que el registro a cambiado
            System.out.println("Configuracion de Proxy Exitosa");
            System.out.println("Proxy actual: " + Proxys[numberProxyInArray] );
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

}
