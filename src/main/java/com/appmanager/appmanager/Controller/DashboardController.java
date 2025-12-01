package com.appmanager.appmanager.Controller;

import com.appmanager.appmanager.Model.AppInstall;
import com.appmanager.appmanager.Model.DashboardModel;
import com.appmanager.appmanager.Utils.MetadataExtractor;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class DashboardController implements Initializable  {

        @FXML private BorderPane mainBorderPane;
        @FXML private TableView<DashboardModel> tablaAplicaciones;
        @FXML private TextField campoBusqueda;
        @FXML private ComboBox<String> comboFiltroCategoria;
        @FXML private ProgressBar barraProgreso;
        @FXML private Label etiquetaEstado;
        @FXML private Button botonInstalar;
        @FXML private VBox panelLateral;

        private AppInstall appInstall = new AppInstall();
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


    private double getSafeDouble(Map<String, String> metadata, String key, double defaultValue) {
        try {
            String value = metadata.get(key);
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
            System.err.println("⚠️ Error parseando '" + key + "': " + metadata.get(key));
        }
        return defaultValue;
    }

       private void inicializarDatos (Map<String, Map<String, String>> Metadato ) {
           List<DashboardModel> lista = new ArrayList<>();
           double sizeMB;
           for (Map.Entry<String, Map<String, String>> entry : Metadato.entrySet()) {
               Map<String, String> metadata = entry.getValue();
               System.out.println(metadata.get("RelativePath") + "Path" );
               lista.add(new DashboardModel(
                       metadata.get("FileName"),
                       metadata.get("FileDescription"),
                       metadata.get("FileVersion"),
                       sizeMB = getSafeDouble(metadata, "SizeMB", 0.0),
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

            TableColumn<DashboardModel, Double> columnaTamaño = new TableColumn<>("Tamaño (MB)");
            columnaTamaño.setCellValueFactory(cellData -> cellData.getValue().tamañoProperty().asObject());

            TableColumn<DashboardModel, Boolean> columnaSeleccion = new TableColumn<>("Instalar");
            columnaSeleccion.setCellValueFactory(cellData -> cellData.getValue().seleccionadoProperty());
            columnaSeleccion.setCellFactory(tc -> new TableCell<DashboardModel, Boolean>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    checkBox.setOnAction(e -> {
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

            tablaAplicaciones.getColumns().addAll(columnaSeleccion, columnaNombre,
                    columnaDescripcion, columnaVersion, columnaTamaño);
        }

        private void configurarFiltros() {
            // Configurar categorías
            List<String> categorias = todasLasAplicaciones.stream()
                    .map(DashboardModel::getCategoria)
                    .distinct()
                    .collect(Collectors.toList());

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
                    if (!app.getCategoria().equals(filtroCategoria)) {
                        return false;
                    }
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
                        .collect(Collectors.toList());

                List<String> nombresSeleccionadas = appsSeleccionadas.stream()
                        .map(DashboardModel::getNombre)
                        .collect(Collectors.toList());

                if (!nombresSeleccionadas.isEmpty()) {
                    for (String Nombre : nombresSeleccionadas) {
                        System.out.println("Instalando: " + Nombre);
                        appInstall.installApp(Nombre);
                        ventanaInstalacion("Instalando: " + Nombre);
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

    public AppInstall getAppInstall() {
        return appInstall;
    }

    public void setAppInstall(AppInstall appInstall) {
        this.appInstall = appInstall;
    }
}

