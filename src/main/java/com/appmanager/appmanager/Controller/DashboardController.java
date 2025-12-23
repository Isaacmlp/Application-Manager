package com.appmanager.appmanager.Controller;

import com.appmanager.appmanager.Model.AppInstall;
import com.appmanager.appmanager.Model.DashboardModel;
import com.appmanager.appmanager.Utils.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import javafx.stage.Modality;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class
DashboardController implements Initializable , KeyListener {

        @FXML private BorderPane mainBorderPane;
        @FXML private TableView<DashboardModel> tablaAplicaciones;
        @FXML private TextField campoBusqueda;
        @FXML private Button botonInstalar;
        @FXML private Button botonMantenimiento;
        @FXML private VBox panelLateral;
        @FXML private Button botonProxy;
        @FXML private Button botonDNS;

        public ProxyConfig proxyConfig = new ProxyConfig();;
        public DNSConfig dnsConfig = new DNSConfig();
        final Integer[] result = {null};


        public FileChoose Fc = new FileChoose();
        public Maintenance maintenance = new Maintenance();

        private int proxyNumer;
        public List<DashboardModel> lista = new ArrayList<>();
        private final AppInstall appInstall = new AppInstall();
        private ObservableList<DashboardModel> todasLasAplicaciones;
        private FilteredList<DashboardModel> aplicacionesFiltradas;

        ClassLoader classLoader = MetadataExtractor.class.getClassLoader();
        //URL resourcesUrl = Objects.requireNonNull(classLoader.getResource("Setups")).toURI().toURL();

        //URI uri = resourcesUrl.toURI(); // decodifica correctamente
        Path path = Paths.get(System.getProperty("user.dir"), "Setups"); // convierte a ruta válida
        File carpeta = path.toFile();

    public DashboardController() throws URISyntaxException, MalformedURLException {
    }


        @Override
        public void initialize(URL location, ResourceBundle resources) {
            Map<String, Map<String, String>> result = MetadataExtractor.getExecutableMetadataFromFolder(carpeta.getAbsolutePath());
            inicializarDatos(result);
            configurarInterfaz();
            //configurarFiltros();
        }


    private double getSafeDouble(Map<String, String> metadata) {
        try {
            String value = metadata.get("SizeMB");
            if (value != null && !value.trim().isEmpty()) {
                // ✅ CORRECCIÓN: Reemplazar coma por punto para parsing
                String cleanValue = value.replace(",", ".")  // Cambiar coma por punto
                        .replaceAll("[^0-9.]", "") // Quitar caracteres no numéricos
                        .trim();
                if (!cleanValue.isEmpty()) {
                    return Double.parseDouble(cleanValue);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Error parseando '" + "SizeMB" + "': " + metadata.get("SizeMB"));
        }
        return 0.0;
    }

       private void inicializarDatos (Map<String, Map<String, String>> Metadato ) {
           double sizeMB;
           for (Map.Entry<String, Map<String, String>> entry : Metadato.entrySet()) {
               Map<String, String> metadata = entry.getValue();
               System.out.println(metadata.get("AbsolutePath") + "Path" );
               lista.add(new DashboardModel(
                       metadata.get("FileName"),
                       metadata.get("FileDescription"),
                       metadata.get("FileVersion"),
                       sizeMB = getSafeDouble(metadata),
                       "Categoría",
                       metadata.get("AbsolutePath"),
                       "Comando de instalación"

               ));
           }

           todasLasAplicaciones = FXCollections.observableArrayList(lista);
           aplicacionesFiltradas = new FilteredList<>(todasLasAplicaciones);
           tablaAplicaciones.setItems(aplicacionesFiltradas);
        }


        private void configurarInterfaz() {
            // Configurar columnas de la tabla
            TableColumn<DashboardModel, String> columnaNombre = new TableColumn<>("Nombre");
            columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());

            TableColumn<DashboardModel, String> columnaDescripcion = new TableColumn<>("Descripción");
            columnaDescripcion.setCellValueFactory(cellData -> cellData.getValue().descripcionProperty());

            TableColumn<DashboardModel, String> columnaVersion = new TableColumn<>("Versión");
            columnaVersion.setCellValueFactory(cellData -> cellData.getValue().versionProperty());

            TableColumn<DashboardModel, Double> columnaTamano = new TableColumn<>("Tamaño (MB)");
            columnaTamano.setCellValueFactory(cellData -> cellData.getValue().tamañoProperty().asObject());

            TableColumn<DashboardModel, Boolean> columnaSeleccion = getDashboardModelBooleanTableColumn();

            tablaAplicaciones.getColumns().addAll(columnaSeleccion, columnaNombre,
                    columnaDescripcion, columnaVersion, columnaTamano);
        }

    @NotNull
    private TableColumn<DashboardModel, Boolean> getDashboardModelBooleanTableColumn() {
        TableColumn<DashboardModel, Boolean> columnaSeleccion = new TableColumn<>("Instalar");
        columnaSeleccion.setCellValueFactory(cellData -> cellData.getValue().seleccionadoProperty());
        columnaSeleccion.setCellFactory(_ -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(_ -> {
                    DashboardModel app = getTableView().getItems().get(getIndex());
                    app.setSeleccionado(checkBox.isSelected());
                    actualizarBotonInstalar();
                });
            }

            @Override
            protected void updateItem(Boolean selected, boolean empty) {
                super.updateItem(selected, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(selected);
                    setGraphic(checkBox);
                }
            }
        });
        return columnaSeleccion;
    }

        private void actualizarBotonInstalar() {
            boolean algunaSeleccionada = todasLasAplicaciones.stream()
                    .anyMatch(DashboardModel::isSeleccionado);
            botonInstalar.setDisable(!algunaSeleccionada);
        }

        private void instalarSecuencialmente(List<String> nombres, List<String> rutas, int index) {
            if (index >= nombres.size() || index >= rutas.size()) {
                System.out.println("Todas las instalaciones han finalizado.");
                return;
            }

            String nombre = nombres.get(index);
            String ruta = rutas.get(index);
            System.out.println("Instalando: " + nombre);
            System.out.println("Ruta: " + ruta);

            instalarConProgreso(nombre, ruta, (resultado) -> {
                System.out.println("Instalación de " + nombre + " finalizada: " +
                        (resultado ? "Éxito" : "Fallo"));

                // Instalar la siguiente aplicación
                Platform.runLater(() -> {
                    instalarSecuencialmente(nombres, rutas, index + 1);
                });
            });
        }

        @FXML
        private void iniciarInstalacion() {
            try {
                List<DashboardModel> appsSeleccionadas = todasLasAplicaciones.stream()
                        .filter(DashboardModel::isSeleccionado)
                        .toList();

                List<String> nombresSeleccionadas = appsSeleccionadas.stream()
                        .map(DashboardModel::getNombre)
                        .toList();

                List<String> RutasSeleccionadas = appsSeleccionadas.stream()
                        .map(DashboardModel::getUrlDescarga)
                        .toList();

                if (!nombresSeleccionadas.isEmpty()) {
                    int pairCount = Math.min(nombresSeleccionadas.size(), RutasSeleccionadas.size());
                    if (pairCount == 0) {
                        System.out.println("No hay aplicaciones o rutas para instalar.");
                    } else {
                        instalarSecuencialmente(nombresSeleccionadas, RutasSeleccionadas, 0);
                    }
                }
            } catch (Exception e) {
                mostrarError("Error al iniciar instalación: " + e.getMessage());
            }
        }

    public void instalarConProgreso(String appName, String absolutePath, Consumer<Boolean> onComplete) {
        // --- Crear ventana de progreso ---
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Instalando " + appName);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);

        Label statusLabel = new Label("Iniciando instalación...");

        VBox box = new VBox(15, statusLabel, progressBar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));

        progressStage.setScene(new Scene(box, 350, 150));
        progressStage.show();

        // --- Crear tarea en segundo plano que retorna boolean ---
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    updateMessage("Ejecutando instalador...");
                    updateProgress(0.3, 1);

                    // Llamamos a tu método del modelo
                    String resultado = appInstall.installApp(appName, absolutePath);

                    updateMessage("Finalizando...");
                    updateProgress(1, 1);

                    // Considerar éxito si no se lanzó excepción
                    return true;
                } catch (Exception ex) {
                    updateMessage("Error: " + ex.getMessage());
                    return false;
                }
            }
        };
        // --- Enlazar UI con Task ---
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        // --- Cuando termina ---
        task.setOnSucceeded(e -> {
            progressStage.close();
            boolean ok = Boolean.TRUE.equals(task.getValue());

            Alert alert = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle(ok ? "Instalación completada" : "Error");
            alert.setHeaderText(appName);
            alert.setContentText(ok ? "Instalación finalizada correctamente." : "La instalación falló.");
            alert.showAndWait();

            // Llamar al callback con el resultado
            if (onComplete != null) {
                onComplete.accept(ok);
            }
        });

        task.setOnFailed(e -> {
            progressStage.close();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("La instalación falló");
            alert.setContentText(task.getException() != null ? task.getException().getMessage() : "Error desconocido");
            alert.showAndWait();

            // Llamar al callback con false
            if (onComplete != null) {
                onComplete.accept(false);
            }
        });

        // --- Ejecutar en otro hilo ---
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

        @FXML
        private void seleccionarTodo() {
            boolean todosSeleccionados = todasLasAplicaciones.stream()
                    .allMatch(DashboardModel::isSeleccionado);

            todasLasAplicaciones.forEach(app ->
                    app.setSeleccionado(!todosSeleccionados));

            actualizarBotonInstalar();
        }

        @FXML
        private void minimizarVentana() {
            ((Stage) mainBorderPane.getScene().getWindow()).setIconified(true);
        }

        @FXML
        private void cerrarAplicacion() {
            System.exit(0);
        }

        private void mostrarError(String mensaje) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        }

        public void ventanaInstalacion (String mensaje){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Instalando...");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
       }

    public boolean confirmacion(String mensaje){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        if (mainBorderPane != null && mainBorderPane.getScene() != null) {
            alert.initOwner(mainBorderPane.getScene().getWindow());
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && (result.get() == ButtonType.OK || result.get() == ButtonType.YES);
    }

    public void message(String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Información");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            if (mainBorderPane != null && mainBorderPane.getScene() != null) {
                alert.initOwner(mainBorderPane.getScene().getWindow());
            }
            alert.showAndWait();
        });
    }

    public void cambiarProxy(ActionEvent actionEvent) {
        int proxy = elegirProxy(proxyConfig.getProxys());

        if (proxy == proxyConfig.getProxys().length){
            message(proxyConfig.DesactivarProxy());
            return;
        }

        proxyConfig.ConfigurarProxy(proxy);
        message("Proxy configurado correctamente.\n Proxy Actual: " + proxyConfig.getProxys()[proxy]);
    }

    public void ConfigurarDNS(ActionEvent actionEvent) {
        int dns = selectDNS(dnsConfig.getDNS());
        if (dns == dnsConfig.getDNS().length){
            message(dnsConfig.disableDNS() ? "DNS desactivados correctamente." : "Error al desactivar DNS.");
            return;
        }
        boolean exito = dnsConfig.setPrimaryDNS(dnsConfig.getDNS()[dns]);
        if (exito) {
            message("DNS configurado correctamente.\n DNS Actual: " + dnsConfig.getDNS()[dns]);
        } else {
            message("Error al configurar DNS.");
        }
    }

    public Integer selectDNS(String[] DNS) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Configurar DNS");

        // Construir mensaje con las opciones
        StringBuilder mensajeBuilder = new StringBuilder("Proxys disponibles:\n\n");
        int index = 0;
        int windowHeight = 300 + ((DNS != null ? DNS.length : 0) * 20); // Ajustar altura según número de proxys
        if (DNS != null) {
            for (String proxy : DNS) {
                mensajeBuilder.append("Opcion ").append(index).append(" : ").append(proxy).append("\n");
                index++;
            }
        }
        mensajeBuilder.append("Opcion ").append(index).append(" : ").append("Desactivar Proxy\n");

        Label label = new Label(mensajeBuilder.toString());
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextField textField = new TextField();
        textField.setPromptText("Escribe el número de la opción...");
        textField.setStyle("-fx-font-size: 13px; -fx-padding: 8;");

        Button okButton = new Button("Aceptar");
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");


        okButton.setOnAction(e -> {
            try {
                int valor = Integer.parseInt(textField.getText().trim());
                if (valor >= 0 && valor < (DNS != null ? DNS.length + 1 : 1)) {
                    result[0] = valor;
                    dialog.close();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Número fuera de rango.");
                    alert.showAndWait();
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Por favor ingresa un número válido.");
                alert.showAndWait();
            }
        });

        cancelButton.setOnAction(e -> {
            result[0] = null;
            dialog.close();
        });

        VBox botones = new VBox(10, okButton, cancelButton);
        botones.setStyle("-fx-alignment: center;");

        VBox layout = new VBox(20, label, textField, botones);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        layout.setPrefSize(400, windowHeight); // 🔎 Ventana más grande

        Scene scene = new Scene(layout);
        // Añadir key listener JavaFX: Enter -> aceptar, Escape -> cancelar
        scene.setOnKeyPressed(evt -> {
            javafx.scene.input.KeyCode code = evt.getCode();
            if (code == javafx.scene.input.KeyCode.ENTER) {
                okButton.fire();
            } else if (code == javafx.scene.input.KeyCode.ESCAPE) {
                cancelButton.fire();
            }
        });

        dialog.setScene(scene);

        // Dar foco al campo de texto al mostrar el diálogo
        dialog.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                javafx.application.Platform.runLater(textField::requestFocus);
            }
        });

        dialog.showAndWait();

        proxyNumer = (result[0] != null) ? result[0] : -1;
        return result[0];
    }

    public Integer elegirProxy(String[] proxys) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Elegir Proxy");

        // Construir mensaje con las opciones
        StringBuilder mensajeBuilder = new StringBuilder("Proxys disponibles:\n\n");
        int index = 0;
        int windowHeight = 300 + ((proxys != null ? proxys.length : 0) * 20); // Ajustar altura según número de proxys
        if (proxys != null) {
            for (String proxy : proxys) {
                mensajeBuilder.append("Opcion ").append(index).append(" : ").append(proxy).append("\n");
                index++;
            }
        }
        mensajeBuilder.append("Opcion ").append(index).append(" : ").append("Desactivar Proxy\n");

        Label label = new Label(mensajeBuilder.toString());
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextField textField = new TextField();
        textField.setPromptText("Escribe el número de la opción...");
        textField.setStyle("-fx-font-size: 13px; -fx-padding: 8;");

        Button okButton = new Button("Aceptar");
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        final Integer[] result = {null};

        okButton.setOnAction(e -> {
            try {
                int valor = Integer.parseInt(textField.getText().trim());
                if (valor >= 0 && valor < (proxys != null ? proxys.length + 1 : 1)) {
                    result[0] = valor;
                    dialog.close();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Número fuera de rango.");
                    alert.showAndWait();
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Por favor ingresa un número válido.");
                alert.showAndWait();
            }
        });

        cancelButton.setOnAction(e -> {
            result[0] = null;
            dialog.close();
        });

        VBox botones = new VBox(10, okButton, cancelButton);
        botones.setStyle("-fx-alignment: center;");

        VBox layout = new VBox(20, label, textField, botones);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        layout.setPrefSize(400, windowHeight); // 🔎 Ventana más grande

        Scene scene = new Scene(layout);
        // Añadir key listener JavaFX: Enter -> aceptar, Escape -> cancelar
        scene.setOnKeyPressed(evt -> {
            javafx.scene.input.KeyCode code = evt.getCode();
            if (code == javafx.scene.input.KeyCode.ENTER) {
                okButton.fire();
            } else if (code == javafx.scene.input.KeyCode.ESCAPE) {
                cancelButton.fire();
            }
        });

        dialog.setScene(scene);

        // Dar foco al campo de texto al mostrar el diálogo
        dialog.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                javafx.application.Platform.runLater(textField::requestFocus);
            }
        });

        dialog.showAndWait();

        proxyNumer = (result[0] != null) ? result[0] : -1;
        return result[0];
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
            if (proxyNumer == proxyConfig.getProxys().length){
                message(proxyConfig.DesactivarProxy());
                return;
            }
            proxyConfig.ConfigurarProxy(proxyNumer);
            message("Proxy configurado correctamente.\n Proxy Actual: " + proxyConfig.getProxys()[proxyNumer]);
        }
    }

    public void Mantenimiento(ActionEvent event) {
        Maintenance(proxyConfig, dnsConfig, (Stage) mainBorderPane.getScene().getWindow());
    }

    public void Maintenance(ProxyConfig proxyConfig, DNSConfig dnsConfig, Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Gestión de Proxys y DNS");

        // Obtener arrays actuales
        String[] proxys = proxyConfig.getProxysArray(); // Asumiendo que existe este método
        String[] dnsArray = dnsConfig.getDNSArray(); // Asumiendo que existe este método

        // Construir mensaje con las opciones principales
        StringBuilder mensajeBuilder = new StringBuilder("GESTIÓN DE CONFIGURACIÓN\n\n");

        int index = 0;
        int windowHeight = 400; // Altura base ajustada

        // Mostrar proxys actuales
        mensajeBuilder.append("=== PROXYS DISPONIBLES ===\n");
        if (proxys != null && proxys.length > 0) {
            for (String proxy : proxys) {
                mensajeBuilder.append(proxy).append("\n");
            }
            windowHeight += proxys.length * 5;
        } else {
            mensajeBuilder.append("No hay proxys configurados\n");
        }

        // Mostrar DNS actuales
        mensajeBuilder.append("\n=== DNS DISPONIBLES ===\n");
        if (dnsArray != null && dnsArray.length > 0) {
            for (String dns : dnsArray) {
                mensajeBuilder.append(dns).append("\n");
            }
            windowHeight += dnsArray.length * 5;
        } else {
            mensajeBuilder.append("No hay DNS configurados\n");
        }

        mensajeBuilder.append("\n=== OPCIONES DE GESTIÓN ===\n");

        // Opciones de gestión
        int opcionBase = index;
        String[] opcionesGestion = {
                "Agregar nuevo Proxy",
                "Modificar Proxy existente",
                "Eliminar Proxy",
                "Agregar nuevo DNS",
                "Modificar DNS existente",
                "Eliminar DNS",
                "Desactivar Proxy",
                "Agregar Setup a Directorio Raiz",
                "Cancelar"
        };

        for (int i = 0; i < opcionesGestion.length; i++) {
            mensajeBuilder.append((opcionBase + i)).append(": ").append(opcionesGestion[i]).append("\n");
        }
        windowHeight += opcionesGestion.length * 20 + 105 ;

        Label label = new Label(mensajeBuilder.toString());
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        TextField textField = new TextField();
        textField.setPromptText("Escribe el número de la opción...");
        textField.setStyle("-fx-font-size: 13px; -fx-padding: 8;");

        Button okButton = new Button("Aceptar");
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

        final Integer[] result = {null};

        okButton.setOnAction(e -> {
            try {
                int valor = Integer.parseInt(textField.getText().trim());
                int maxOpcion = opcionBase + opcionesGestion.length - 1;

                if (valor >= 0 && valor <= maxOpcion) {
                    // Si es una opción de proxy o DNS existente (para seleccionar)
                    if (valor < opcionBase) {
                        result[0] = valor;
                        dialog.close();
                    }
                    // Si es una opción de gestión
                    else {
                        int opcionGestion = valor - opcionBase;
                        procesarOpcionGestion(opcionGestion, proxyConfig, dnsConfig, dialog, label);
                    }
                } else {
                    mostrarAlerta("Número fuera de rango. Debe estar entre 0 y " + maxOpcion);
                }
            } catch (NumberFormatException ex) {
                mostrarAlerta("Por favor ingresa un número válido.");
            }
        });

        cancelButton.setOnAction(e -> {
            result[0] = null;
            dialog.close();
        });

        HBox botones = new HBox(20, okButton, cancelButton);
        botones.setStyle("-fx-alignment: center; -fx-padding: 10 0 0 0;");

        VBox layout = new VBox(20, label, textField, botones);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        layout.setPrefSize(500, windowHeight);

        Scene scene = new Scene(layout);

        scene.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (code == KeyCode.ENTER) {
                okButton.fire();
            } else if (code == KeyCode.ESCAPE) {
                cancelButton.fire();
            }
        });

        dialog.setScene(scene);

        dialog.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                Platform.runLater(textField::requestFocus);
            }
        });

        dialog.showAndWait();

        proxyNumer = (result[0] != null) ? result[0] : -1;
    }

    // Método auxiliar para procesar opciones de gestión
    private void procesarOpcionGestion(int opcion, ProxyConfig proxyConfig, DNSConfig dnsConfig, Stage dialog, Label label) {
        try {
            // Obtener arrays actualizados
            String[] proxys = proxyConfig.updateProxys();
            String[] dnsArray = dnsConfig.updateDNS();

            switch (opcion) {
                case 0: // Agregar nuevo Proxy
                    String nuevoProxy = mostrarInputDialog("Agregar Proxy", "Ingrese el nuevo proxy (formato: IP:PUERTO):");
                    if (nuevoProxy != null && !nuevoProxy.trim().isEmpty()) {
                        Maintenance.addProxy(nuevoProxy.trim());
                        mostrarInfo("Proxy agregado exitosamente");
                        // Recargar proxys desde archivo .env y actualizar ProxyConfig
                        List<String> nuevosProxys = Maintenance.getProxies();
                        proxyConfig.setProxys(nuevosProxys.toArray(new String[0]));
                        // Actualizar interfaz
                        actualizarInterfaz(proxyConfig, dnsConfig, label);
                    }
                    break;

                case 1: // Modificar Proxy existente
                    if (proxys == null || proxys.length == 0) {
                        mostrarAlerta("No hay proxys para modificar");
                        return;
                    }
                    String proxyModificar = mostrarSeleccionSimple("Seleccionar Proxy a modificar",
                            "Seleccione el proxy a modificar:", proxys);
                    if (proxyModificar != null) {
                        String nuevoValor = mostrarInputDialog("Modificar Proxy",
                                "Proxy actual: " + proxyModificar + "\nIngrese el nuevo valor:");
                        if (nuevoValor != null && !nuevoValor.trim().isEmpty()) {
                            Maintenance.modifyProxy(proxyModificar, nuevoValor.trim());
                            mostrarInfo("Proxy modificado exitosamente");
                            // Recargar proxys desde archivo .env
                            List<String> nuevosProxys = Maintenance.getProxies();
                            proxyConfig.setProxys(nuevosProxys.toArray(new String[0]));
                            // Actualizar interfaz
                            actualizarInterfaz(proxyConfig, dnsConfig, label);
                        }
                    }
                    break;
                case 2: // Eliminar Proxy
                    if (proxys == null || proxys.length == 0) {
                        mostrarAlerta("No hay proxys para eliminar");
                        return;
                    }
                    String proxyEliminar = mostrarSeleccionSimple("Eliminar Proxy",
                            "Seleccione el proxy a eliminar:", proxys);
                    if (proxyEliminar != null) {
                        if (mostrarConfirmacion("¿Está seguro de eliminar el proxy: " + proxyEliminar + "?")) {
                            Maintenance.removeProxy(proxyEliminar);
                            mostrarInfo("Proxy eliminado exitosamente");
                            // Recargar proxys desde archivo .env
                            List<String> nuevosProxys = Maintenance.getProxies();
                            proxyConfig.setProxys(nuevosProxys.toArray(new String[0]));
                            // Actualizar interfaz
                            actualizarInterfaz(proxyConfig, dnsConfig, label);
                        }
                    }
                    break;
                case 3: // Agregar nuevo DNS
                    String nuevoDNS = mostrarInputDialog("Agregar DNS", "Ingrese el nuevo DNS (ejemplo: 8.8.8.8):");
                    if (nuevoDNS != null && !nuevoDNS.trim().isEmpty()) {
                        Maintenance.addDNS(nuevoDNS.trim());
                        mostrarInfo("DNS agregado exitosamente");
                        // Recargar DNS desde archivo .env
                        List<String> nuevosDNS = Maintenance.getDNSList();
                        dnsConfig.setDNS(nuevosDNS.toArray(new String[0]));
                        // Actualizar interfaz
                        actualizarInterfaz(proxyConfig, dnsConfig, label);
                    }
                    break;
                case 4: // Modificar DNS existente
                    if (dnsArray == null || dnsArray.length == 0) {
                        mostrarAlerta("No hay DNS configurados para modificar");
                        return;
                    }
                    String dnsModificar = mostrarSeleccionSimple("Seleccionar DNS a modificar",
                            "Seleccione el DNS a modificar:", dnsArray);
                    if (dnsModificar != null) {
                        String nuevoDNSValor = mostrarInputDialog("Modificar DNS",
                                "DNS actual: " + dnsModificar + "\nIngrese el nuevo valor:");
                        if (nuevoDNSValor != null && !nuevoDNSValor.trim().isEmpty()) {
                            Maintenance.modifyDNS(dnsModificar, nuevoDNSValor.trim());
                            mostrarInfo("DNS modificado exitosamente");
                            // Recargar DNS desde archivo .env
                            List<String> nuevosDNS = Maintenance.getDNSList();
                            dnsConfig.setDNS(nuevosDNS.toArray(new String[0]));
                            // Actualizar interfaz
                            actualizarInterfaz(proxyConfig, dnsConfig, label);
                        }
                    }
                    break;
                case 5: // Eliminar DNS
                    if (dnsArray == null || dnsArray.length == 0) {
                        mostrarAlerta("No hay DNS configurados para eliminar");
                        return;
                    }
                    String dnsEliminar = mostrarSeleccionSimple("Eliminar DNS",
                            "Seleccione el DNS a eliminar:", dnsArray);
                    if (dnsEliminar != null) {
                        if (mostrarConfirmacion("¿Está seguro de eliminar el DNS: " + dnsEliminar + "?")) {
                            Maintenance.removeDNS(dnsEliminar);
                            mostrarInfo("DNS eliminado exitosamente");
                            // Recargar DNS desde archivo .env
                            List<String> nuevosDNS = Maintenance.getDNSList();
                            dnsConfig.setDNS(nuevosDNS.toArray(new String[0]));
                            // Actualizar interfaz
                            actualizarInterfaz(proxyConfig, dnsConfig, label);
                        }
                    }
                    break;
                case 6: // Desactivar Proxy
                    // Retorna el índice correspondiente a "Desactivar Proxy"
                    result[0] = (proxys != null ? proxys.length : 0);
                    dialog.close();
                    break;
                case 7:
                    maintenance.setOnCopySuccess(() -> {
                        // 🔄 Reanalizar carpeta
                        Map<String, Map<String, String>> result =
                                MetadataExtractor.getExecutableMetadataFromFolder(carpeta.getAbsolutePath());

                        // 🧹 Limpiar lista
                        lista.clear();

                        // ♻️ Volver a cargar datos
                        inicializarDatos(result);

                        // 🔃 Refrescar tabla
                        tablaAplicaciones.refresh();

                        System.out.println("UI actualizada después de copiar instalador.");
                    });
                    maintenance.chooseAndCopyInstaller((Stage) mainBorderPane.getScene().getWindow());
                    break;
                case 8: // Cancelar
                    dialog.close();
                    break;
            }
        } catch (IOException ex) {
            mostrarAlerta("Error al realizar la operación: " + ex.getMessage());
        }
    }

    // Método para actualizar la interfaz
    private void actualizarInterfaz(ProxyConfig proxyConfig, DNSConfig dnsConfig, Label label) {
        try {
            // Obtener arrays actualizados directamente desde las configuraciones
            String[] proxys = proxyConfig.updateProxys();
            String[] dnsArray = dnsConfig.updateDNS();

            // Reconstruir el mensaje
            StringBuilder mensajeBuilder = new StringBuilder("GESTIÓN DE CONFIGURACIÓN\n\n");

            int index = 0;

            // Mostrar proxys actuales
            mensajeBuilder.append("=== PROXYS DISPONIBLES ===\n");
            if (proxys != null && proxys.length > 0) {
                for (String proxy : proxys) {
                    mensajeBuilder.append(proxy).append("\n");
                }
            } else {
                mensajeBuilder.append("No hay proxys configurados\n");
            }

            // Mostrar DNS actuales
            mensajeBuilder.append("\n=== DNS DISPONIBLES ===\n");
            if (dnsArray != null && dnsArray.length > 0) {
                for (String dns : dnsArray) {
                    mensajeBuilder.append(dns).append("\n");
                }
            } else {
                mensajeBuilder.append("No hay DNS configurados\n");
            }

            mensajeBuilder.append("\n=== OPCIONES DE GESTIÓN ===\n");

            // Opciones de gestión
            int opcionBase = index;
            String[] opcionesGestion = {
                    "Agregar nuevo Proxy",
                    "Modificar Proxy existente",
                    "Eliminar Proxy",
                    "Agregar nuevo DNS",
                    "Modificar DNS existente",
                    "Eliminar DNS",
                    "Desactivar Proxy",
                    "Agregar Setup a Directorio Raiz",
                    "Cancelar"
            };

            for (int i = 0; i < opcionesGestion.length; i++) {
                mensajeBuilder.append((opcionBase + i)).append(": ").append(opcionesGestion[i]).append("\n");
            }

            // Actualizar el texto del label
            Platform.runLater(() -> label.setText(mensajeBuilder.toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos auxiliares para diálogos (ya los tienes)
    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.showAndWait();
    }

    private boolean mostrarConfirmacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje,
                ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private String mostrarInputDialog(String titulo, String mensaje) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.setContentText(mensaje);

        return dialog.showAndWait().orElse(null);
    }

    private String mostrarSeleccionSimple(String titulo, String mensaje, String[] opciones) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(opciones.length > 0 ? opciones[0] : "", opciones);
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.setContentText(mensaje);

        return dialog.showAndWait().orElse(null);
    }

    // Método para obtener DNS actual desde archivo .env
    private String obtenerDNSActual() throws IOException {
        List<String> dnsList = Maintenance.getDNSList();
        return String.join(";", dnsList);
    }
}

