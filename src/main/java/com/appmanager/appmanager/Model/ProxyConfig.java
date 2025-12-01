package com.appmanager.appmanager.Model;

import java.io.IOException;
import java.util.Arrays;

public class ProxyConfig {
    //Array para guardar todos los proxys
    static String[] Proxys = {"10.25.0.152:3128","10.25.0.152:3128","10.25.0.152:3128"} ;
    static String registryPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";

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

            System.out.println("Ejecutando REG ADD para habilitar el proxy");
            enableProxy.start().waitFor();

            System.out.println("Ejecutando REG ADD para establecer la direccion del proxy");
            setProxy.start().waitFor();

            //Notificar al Sistema operativo que el registro a cambiado
            System.out.println("Configuracion de Proxy Exitosa");
            System.out.println("Proxy actual: " + Proxys[numberProxyInArray] );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        ConfigurarProxy(0);
    }
}
