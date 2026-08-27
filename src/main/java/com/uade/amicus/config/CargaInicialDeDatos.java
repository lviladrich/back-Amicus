package com.uade.amicus.config;

import com.uade.amicus.model.Categoria;
import com.uade.amicus.model.Zona;
import com.uade.amicus.repository.CategoriaRepository;
import com.uade.amicus.repository.ZonaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga las categorias y zonas iniciales al arrancar.
 *
 * Por que existe: cada integrante tiene su propia base local, asi que sin esto
 * cada uno tendria que crear las categorias a mano antes de poder probar nada, y
 * con nombres distintos. Como es codigo, se versiona en Git y los cinco arrancan
 * con los mismos datos.
 *
 * Es tambien lo que permite que el profesor descargue el proyecto y tenga un
 * catalogo con el que probar sin cargar nada.
 *
 * CommandLineRunner ejecuta el metodo run() una vez que la aplicacion termino de
 * levantar.
 */
@Component
public class CargaInicialDeDatos implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ZonaRepository zonaRepository;

    public CargaInicialDeDatos(CategoriaRepository categoriaRepository,
                               ZonaRepository zonaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.zonaRepository = zonaRepository;
    }

    @Override
    public void run(String... args) {
        cargarCategorias();
        cargarZonas();
    }

    /**
     * La guarda "si esta vacia" es importante: sin ella, cada arranque
     * duplicaria las categorias.
     */
    private void cargarCategorias() {
        if (categoriaRepository.count() > 0) {
            return;
        }

        categoriaRepository.saveAll(List.of(
                crearCategoria("Electricidad", "Instalaciones, tableros, tomacorrientes y artefactos"),
                crearCategoria("Plomeria", "Destapaciones, perdidas, canerias y griferia"),
                crearCategoria("Gas", "Instalacion y reparacion de artefactos a gas por matriculado"),
                crearCategoria("Pintura", "Interiores, exteriores, empapelado y revestimientos"),
                crearCategoria("Carpinteria", "Muebles a medida, puertas, placares y reparaciones"),
                crearCategoria("Limpieza", "Limpieza de hogar, fin de obra y tapizados"),
                crearCategoria("Aire acondicionado", "Instalacion, carga de gas y mantenimiento"),
                crearCategoria("Cerrajeria", "Aperturas, cambio de cerraduras y copias de llaves"),
                crearCategoria("Jardineria", "Corte de cesped, poda y mantenimiento de parques"),
                crearCategoria("Mudanzas", "Fletes, mudanzas y armado de muebles")
        ));
    }

    private void cargarZonas() {
        if (zonaRepository.count() > 0) {
            return;
        }

        List<String> nombres = List.of(
                // CABA
                "Almagro", "Belgrano", "Caballito", "Palermo", "Recoleta",
                "San Telmo", "Villa Crespo", "Villa Urquiza", "Nunez", "Flores",
                // GBA
                "Vicente Lopez", "San Isidro", "Tigre", "Quilmes", "Avellaneda",
                "Lomas de Zamora", "Moron", "San Martin"
        );

        zonaRepository.saveAll(nombres.stream()
                .map(nombre -> Zona.builder().nombre(nombre).build())
                .toList());
    }

    private Categoria crearCategoria(String nombre, String descripcion) {
        return Categoria.builder().nombre(nombre).descripcion(descripcion).build();
    }
}
