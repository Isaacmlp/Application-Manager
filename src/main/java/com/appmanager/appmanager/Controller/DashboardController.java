package com.appmanager.appmanager.Controller;

import com.appmanager.appmanager.Model.AppInstall;
import com.appmanager.appmanager.Model.DashboardModel;
import com.appmanager.appmanager.Utils.DNSConfig;
import com.appmanager.appmanager.Utils.ProxyConfig;
import com.appmanager.appmanager.Utils.FileChoose;
import com.appmanager.appmanager.Utils.MetadataExtractor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.*;
import java.nio.file.Path;
import javafx.stage.Modality;
import java.nio.file.Paths;
import java.util.*;

public class
DashboardController implements Initializable , KeyListener {

        @FXML private BorderPane mainBorderPane;
        @FXML private TableView<DashboardModel> tablaAplicaciones;
        @FXML private TextField campoBusqueda;
        @FXML private ComboBox<String> comboFiltroCategoria;
        @FXML private ProgressBar barraProgreso;
        @FXML private Label etiquetaEstado;
        @FXML private Button botonInstalar;
        @FXML private VBox panelLateral;
        @FXML private Button botonProxy;
        @FXML private Button botonDNS;

        public ProxyConfig proxyConfig = new ProxyConfig();;
        public DNSConfig dnsConfig = new DNSConfig();

        public FileChoose Fc = new FileChoose();

        private int proxyNumer;
        public List<DashboardModel> lista = new ArrayList<>();
        private final AppInstall appInstall = new AppInstall();
        private ObservableList<DashboardModel> todasLasAplicaciones;
        private FilteredList<DashboardModel> aplicacionesFiltradas;

        ClassLoader classLoader = MetadataExtractor.class.getClassLoader();
        URL resourcesUrl = Objects.requireNonNull(classLoader.getResource("Setups")).toURI().toURL();

        URI uri = resourcesUrl.toURI(); // decodifica correctamente
        Path path = Paths.get(uri); // convierte a ruta válida
        File carpeta = path.toFile();

    public DashboardController() throws URISyntaxException, MalformedURLException {
    }


        @Override
        public void initialize(URL location, ResourceBundle resources) {
            Map<String, Map<String, String>> result = MetadataExtractor.getExecutableMetadataFromFolder(carpeta.getAbsolutePath());
            inicializarDatos(result);
            configurarInterfaz();
            configurarFiltros();
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
               System.out.println(metadata.get("RelativePath") + "Path" );
               lista.add(new DashboardModel(
                       metadata.get("FileName"),
                       metadata.get("FileDescription"),
                       metadata.get("FileVersion"),
                       sizeMB = getSafeDouble(metadata),
                       "Categoría",
                       metadata.get("RelativePath"),
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

    private void configurarFiltros() {
            // Configurar categorías
            List<String> categorias = todasLasAplicaciones.stream()
                    .map(DashboardModel::getCategoria)
                    .distinct()
                    .toList();

            comboFiltroCategoria.getItems().addAll("Todas");
            comboFiltroCategoria.getItems().addAll(categorias);
            comboFiltroCategoria.setValue("Todas");

            // Configurar búsqueda
            campoBusqueda.textProperty().addListener((obs, oldVal, newVal) -> filtrarAplicaciones());
            comboFiltroCategoria.valueProperty().addListener((obs, oldVal, newVal) -> filtrarAplicaciones());
        }

        private void filtrarAplicaciones() {
            aplicacionesFiltradas.setPredicate(app -> {
                String filtroBusqueda = campoBusqueda.getText();
                String filtroCategoria = comboFiltroCategoria.getValue();

                // Filtro por búsqueda
                if (filtroBusqueda != null && !filtroBusqueda.isEmpty()) {
                    String lowerCaseFilter = filtroBusqueda.toLowerCase();
                    if (!app.getNombre().toLowerCase().contains(lowerCaseFilter) &&
                            !app.getDescripcion().toLowerCase().contains(lowerCaseFilter)) {
                        return false;
                    }
                }

                // Filtro por categoría
                if (filtroCategoria != null && !filtroCategoria.equals("Todas")) {
                    return app.getCategoria().equals(filtroCategoria);
                }

                return true;
            });
        }

        private void actualizarBotonInstalar() {
            boolean algunaSeleccionada = todasLasAplicaciones.stream()
                    .anyMatch(DashboardModel::isSeleccionado);
            botonInstalar.setDisable(!algunaSeleccionada);
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
                    for (String Nombre : nombresSeleccionadas) {
                        for(String Ruta : RutasSeleccionadas){
                            System.out.println("Instalando: " + Nombre);
                            System.out.println("Ruta: " + RutasSeleccionadas);
                            String Result = appInstall.installApp(Nombre,Ruta);
                            message(Result);
                            if (Nombre.contains("Thunderbird")) {
                                boolean thunderbird = confirmacion("Desea Agregar Carpeta de perfil?");

                                if (thunderbird) {
                                    Fc.directoryChooser(botonInstalar);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                mostrarError("Error al iniciar instalación: " + e.getMessage());
            }
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

        final Integer[] result = {null};

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
}

