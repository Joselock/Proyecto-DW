package com.mycompany.gestorui.model.utils;

import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class VentanaManager {
    
    // Constantes para las claves de las ventanas
    public static final String VENTANA_GESTION = "gestion";
    public static final String VENTANA_REPORTES = "reportes";
    public static final String VENTANA_CUENTA = "cuenta";
    public static final String VENTANA_CAMBIO_PASSWORD = "cambioPassword";
    public static final String VENTANA_MODIFICAR_PERFIL = "modificarPerfil";
    
    private static VentanaManager instance;
    private final Map<String, Stage> ventanasAbiertas;
    
    private VentanaManager() {
        ventanasAbiertas = new HashMap<>();
    }
    
    public static VentanaManager getInstance() {
        if (instance == null) {
            instance = new VentanaManager();
        }
        return instance;
    }
    
    /**
     * Abre una ventana modal usando las dimensiones del FXML
     */
    public boolean abrirVentanaModal(String key, String fxmlPath, String titulo, Stage padre) {
        if (ventanasAbiertas.containsKey(key)) {
            Stage existingStage = ventanasAbiertas.get(key);
            if (existingStage.isShowing()) {
                existingStage.toFront();
                System.out.println("⚠️ La ventana '" + titulo + "' ya está abierta");
                return false;
            } else {
                ventanasAbiertas.remove(key);
            }
        }
        
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            
            if (resource == null) {
                String pathSinBarra = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                resource = Thread.currentThread().getContextClassLoader().getResource(pathSinBarra);
            }
            
            if (resource == null) {
                System.err.println("❌ No se encontró el recurso: " + fxmlPath);
                return false;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(padre);
            //stage.initStyle(StageStyle.UNDECORATED);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle(titulo);
            
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(false);
            
            stage.setOnCloseRequest(event -> {
                ventanasAbiertas.remove(key);
                System.out.println("🔒 Ventana '" + titulo + "' cerrada");
            });
            
            ventanasAbiertas.put(key, stage);
            stage.show();
            
            // Mostrar las dimensiones reales que tomó
            System.out.println("✅ Ventana '" + titulo + "' abierta (" + 
                (int)root.prefWidth(-1) + "x" + (int)root.prefHeight(-1) + ") desde FXML");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error al abrir ventana '" + titulo + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void cerrarVentana(String key) {
        if (ventanasAbiertas.containsKey(key)) {
            Stage stage = ventanasAbiertas.get(key);
            stage.close();
            ventanasAbiertas.remove(key);
        }
    }
    
    public boolean estaAbierta(String key) {
        return ventanasAbiertas.containsKey(key) && ventanasAbiertas.get(key).isShowing();
    }
    
    public void cerrarTodas() {
        for (Stage stage : ventanasAbiertas.values()) {
            stage.close();
        }
        ventanasAbiertas.clear();
    }
    
    public Stage getVentana(String key) {
        return ventanasAbiertas.get(key);
    }
}