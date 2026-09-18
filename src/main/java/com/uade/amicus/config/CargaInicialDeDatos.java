package com.uade.amicus.config;

import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.Categoria;
import com.uade.amicus.model.Rol;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.model.Zona;
import com.uade.amicus.repository.CarritoRepository;
import com.uade.amicus.repository.CategoriaRepository;
import com.uade.amicus.repository.UsuarioRepository;
import com.uade.amicus.repository.ZonaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga las categorias, las zonas y el usuario administrador al arrancar.
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
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaInicialDeDatos(CategoriaRepository categoriaRepository,
                               ZonaRepository zonaRepository,
                               UsuarioRepository usuarioRepository,
                               CarritoRepository carritoRepository,
                               PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.zonaRepository = zonaRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        cargarCategorias();
        cargarZonas();
        cargarAdministrador();
    }

    /**
     * El ADMIN no se registra por la API (el endpoint de registro asigna
     * siempre USUARIO), asi que la unica forma de que exista es sembrarlo aca,
     * con la misma logica que las categorias: es un dato que define el sistema
     * y tiene que estar antes que cualquier usuario.
     *
     * La contrasena pasa por el mismo PasswordEncoder que el registro, para que
     * el login funcione igual que con cualquier otro usuario.
     */
    private void cargarAdministrador() {
        if (usuarioRepository.existsByEmail("admin@amicus.com")) {
            return;
        }

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .username("admin")
                .email("admin@amicus.com")
                .password(passwordEncoder.encode("admin123"))
                .nombre("Administrador")
                .apellido("Amicus")
                .rol(Rol.ADMIN)
                .build());

        // Todo usuario tiene carrito, aunque el admin no compre: asi el resto
        // del codigo no tiene que preguntarse si existe.
        carritoRepository.save(Carrito.builder().usuario(admin).build());
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
