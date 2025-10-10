package com.appmanager.appmanager.Controller;

import com.appmanager.appmanager.Model.DashboardModel;
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

import java.net.URL;
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

        private ObservableList<DashboardModel> todasLasAplicaciones;
        private FilteredList<DashboardModel> aplicacionesFiltradas;

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            inicializarDatos();
            configurarInterfaz();
            configurarFiltros();
        }

       /* private void InicializarSetupDinamicamente (Map<String, String> Metadato ) {
            while (Metadato.) {
                FXCollections.observableArrayList(new DashboardModel(
                        Metadato.get("Name"),
                        Metadato.get("FileDescription"),
                        Metadato.get("FileVersion"),
                        Double.parseDouble(Metadato.get("SizeMB")),
                        "Categoría", // Aquí podrías asignar una categoría basada en alguna lógica
                        "URL de descarga", // Aquí podrías asignar una URL de descarga basada en alguna lógica
                        "Comando de instalación" // Aquí podrías asignar un comando de instalación basado en alguna lógica
                ));
            }
            todasLasAplicaciones = FXCollections.observableArrayList();

            aplicacionesFiltradas = new FilteredList<>(todasLasAplicaciones);
            tablaAplicaciones.setItems(aplicacionesFiltradas);

        }*/

        private void inicializarDatos() {
            todasLasAplicaciones = FXCollections.observableArrayList(
                    new DashboardModel("Google Chrome", "Navegador web rápido", "115.0", 120.5,
                            "Navegadores", "https://dl.google.com/chrome/install/chrome.exe",
                            "chrome_installer.exe /silent /install"),

                    new DashboardModel("Mozilla Firefox", "Navegador open-source", "116.0", 200.0,
                            "Navegadores", "https://download.mozilla.org/?product=firefox-latest",
                            "firefox_installer.exe -ms"),

                    new DashboardModel("VLC Media Player", "Reproductor multimedia", "3.0.18", 85.7,
                            "Multimedia", "https://get.videolan.org/vlc/3.0.18/win64/vlc-3.0.18-win64.exe",
                            "vlc_installer.exe /S"),

                    new DashboardModel("Visual Studio Code", "Editor de código", "1.81.0", 300.2,
                            "Desarrollo", "https://code.visualstudio.com/sha/download?build=stable&os=win32-x64",
                            "VSCodeSetup.exe /silent /mergetasks=!runcode"),

                    new DashboardModel("7-Zip", "Compresor de archivos", "23.01", 4.5,
                            "Utilidades", "https://www.7-zip.org/a/7z2301-x64.exe",
                            "7z_installer.exe /S"),

                    new DashboardModel("Discord", "App de comunicación", "1.0.9016", 150.8,
                            "Comunicación", "https://dl.discordapp.net/apps/win/DiscordSetup.exe",
                            "DiscordSetup.exe --silent")
            );

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

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/instalacion-view.fxml"));
                Parent vistaInstalacion = loader.load();

               /* InstalacionController controller = loader.getController();
                controller.setAplicacionesAInstalar(appsSeleccionadas);
                controller.setPanelPrincipal(mainBorderPane);*/

                mainBorderPane.setCenter(vistaInstalacion);

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
}

