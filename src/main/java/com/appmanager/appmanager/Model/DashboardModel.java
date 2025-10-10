package com.appmanager.appmanager.Model;

import javafx.beans.property.*;

public class DashboardModel {

        private final StringProperty nombre;
        private final StringProperty descripcion;
        private final StringProperty version;
        private final DoubleProperty tamaño;
        private final BooleanProperty seleccionado;
        private final StringProperty categoria;
        private final StringProperty urlDescarga;
        private final StringProperty comandoInstalacion;

        public DashboardModel(String nombre, String descripcion, String version,
                          double tamaño, String categoria, String urlDescarga,
                          String comandoInstalacion) {
            this.nombre = new SimpleStringProperty(nombre);
            this.descripcion = new SimpleStringProperty(descripcion);
            this.version = new SimpleStringProperty(version);
            this.tamaño = new SimpleDoubleProperty(tamaño);
            this.seleccionado = new SimpleBooleanProperty(false);
            this.categoria = new SimpleStringProperty(categoria);
            this.urlDescarga = new SimpleStringProperty(urlDescarga);
            this.comandoInstalacion = new SimpleStringProperty(comandoInstalacion);
        }

        // Getters y Setters
        public String getNombre() { return nombre.get(); }
        public StringProperty nombreProperty() { return nombre; }

        public String getDescripcion() { return descripcion.get(); }
        public StringProperty descripcionProperty() { return descripcion; }

        public String getVersion() { return version.get(); }
        public StringProperty versionProperty() { return version; }

        public double getTamaño() { return tamaño.get(); }
        public DoubleProperty tamañoProperty() { return tamaño; }

        public boolean isSeleccionado() { return seleccionado.get(); }
        public void setSeleccionado(boolean seleccionado) { this.seleccionado.set(seleccionado); }
        public BooleanProperty seleccionadoProperty() { return seleccionado; }

        public String getCategoria() { return categoria.get(); }
        public StringProperty categoriaProperty() { return categoria; }

        public String getUrlDescarga() { return urlDescarga.get(); }
        public StringProperty urlDescargaProperty() { return urlDescarga; }

        public String getComandoInstalacion() { return comandoInstalacion.get(); }
        public StringProperty comandoInstalacionProperty() { return comandoInstalacion; }



}
