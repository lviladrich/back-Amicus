package com.uade.amicus.config;

import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.Categoria;
import com.uade.amicus.model.Rol;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.model.Zona;
import com.uade.amicus.repository.CarritoRepository;
import com.uade.amicus.repository.CategoriaRepository;
import com.uade.amicus.repository.ServicioRepository;
import com.uade.amicus.repository.UsuarioRepository;
import com.uade.amicus.repository.ZonaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ServicioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaInicialDeDatos(CategoriaRepository categoriaRepository,
                               ZonaRepository zonaRepository,
                               UsuarioRepository usuarioRepository,
                               CarritoRepository carritoRepository,
                               ServicioRepository servicioRepository,
                               PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.zonaRepository = zonaRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        cargarCategorias();
        cargarZonas();
        cargarAdministrador();
        cargarServiciosDeEjemplo();
    }

    /**
     * Sin servicios la pantalla de inicio arranca vacia, asi que se siembran
     * cuatro profesionales de ejemplo con sus servicios. Son usuarios comunes
     * (rol USUARIO) con contrasena "amicus123", y sirven tambien para probar
     * la compra con otra cuenta, porque nadie puede comprar su propio servicio.
     *
     * La guarda mira si ya existe la primera profesional, igual que con el
     * admin, para no duplicar nada en cada arranque.
     */
    private void cargarServiciosDeEjemplo() {
        if (usuarioRepository.existsByEmail("marta.gomez@amicus.com")) {
            return;
        }

        Map<String, Zona> zonas = zonaRepository.findAll().stream()
                .collect(Collectors.toMap(Zona::getNombre, Function.identity()));

        Usuario marta = crearProfesional("marta.gomez", "Marta", "Gomez");
        Usuario diego = crearProfesional("diego.ferreyra", "Diego", "Ferreyra");
        Usuario carla = crearProfesional("carla.benitez", "Carla", "Benitez");
        Usuario ramon = crearProfesional("ramon.ibarra", "Ramon", "Ibarra");

        servicioRepository.saveAll(List.of(
                crearServicio(marta, "Plomeria", "Destapación de cañerías",
                        "Destapo piletas, inodoros y cañerías con máquina. Incluye revisión de pérdidas.",
                        28000, 6, zonas, "Almagro", "Caballito", "Villa Crespo"),
                crearServicio(marta, "Plomeria", "Cambio de grifería",
                        "Cambio canillas, flexibles y llaves de paso en cocina o baño. La grifería la ponés vos.",
                        22000, 8, zonas, "Almagro", "Caballito", "Flores"),
                crearServicio(marta, "Gas", "Instalación de cocina o calefón",
                        "Gasista matriculada. Conexión, prueba de hermeticidad y certificado.",
                        45000, 3, zonas, "Caballito", "Flores", "Moron"),
                crearServicio(diego, "Electricidad", "Instalación de luminarias",
                        "Instalo lámparas, apliques y ventiladores de techo. Materiales aparte.",
                        35000, 5, zonas, "Belgrano", "Nunez", "Villa Urquiza"),
                crearServicio(diego, "Electricidad", "Revisión de tablero eléctrico",
                        "Reviso el tablero, cambio térmicas y disyuntor si hace falta y te explico qué encontré.",
                        40000, 4, zonas, "Belgrano", "Palermo", "Recoleta"),
                crearServicio(diego, "Aire acondicionado", "Mantenimiento de aire acondicionado",
                        "Limpieza de filtros y unidad exterior, control de gas y prueba de funcionamiento.",
                        38000, 6, zonas, "Palermo", "Belgrano", "Vicente Lopez"),
                crearServicio(carla, "Pintura", "Pintura de ambiente completo",
                        "Paredes y techo de un ambiente de hasta 20 m², con enduido y dos manos.",
                        120000, 2, zonas, "San Telmo", "Recoleta", "Palermo"),
                crearServicio(carla, "Limpieza", "Limpieza profunda de hogar",
                        "Cocina, baños, vidrios y pisos. Llevo los productos. Hasta 3 ambientes.",
                        30000, 10, zonas, "San Telmo", "Palermo", "Recoleta"),
                crearServicio(carla, "Limpieza", "Limpieza de fin de obra",
                        "Retiro de polvo de obra, restos de pintura y limpieza de aberturas.",
                        65000, 3, zonas, "Avellaneda", "Quilmes", "San Telmo"),
                crearServicio(ramon, "Carpinteria", "Arreglo de puertas y placares",
                        "Ajusto bisagras, cambio correderas y reparo puertas que no cierran.",
                        25000, 7, zonas, "San Isidro", "Vicente Lopez", "Tigre"),
                crearServicio(ramon, "Cerrajeria", "Cambio de cerradura",
                        "Cambio de cerradura o combinación. Incluye tres copias de llave.",
                        32000, 5, zonas, "San Isidro", "Vicente Lopez", "Nunez"),
                crearServicio(ramon, "Jardineria", "Corte de césped y poda",
                        "Corte de pasto, bordes y poda de arbustos. Me llevo los residuos verdes.",
                        27000, 8, zonas, "Tigre", "San Isidro", "San Martin"),
                crearServicio(ramon, "Mudanzas", "Flete y armado de muebles",
                        "Flete con ayudante dentro del AMBA. Desarmo y armo los muebles en destino.",
                        80000, 2, zonas, "Lomas de Zamora", "Avellaneda", "Quilmes")
        ));
    }

    private Usuario crearProfesional(String username, String nombre, String apellido) {
        Usuario profesional = usuarioRepository.save(Usuario.builder()
                .username(username)
                .email(username + "@amicus.com")
                .password(passwordEncoder.encode("amicus123"))
                .nombre(nombre)
                .apellido(apellido)
                .rol(Rol.USUARIO)
                .build());
        carritoRepository.save(Carrito.builder().usuario(profesional).build());
        return profesional;
    }

    private Servicio crearServicio(Usuario profesional, String categoria, String titulo, String descripcion,
                                   int precio, int cupos, Map<String, Zona> zonas, String... barrios) {
        return Servicio.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .precio(BigDecimal.valueOf(precio))
                .cuposDisponibles(cupos)
                .categoria(categoriaRepository.findByNombreIgnoreCase(categoria).orElseThrow())
                .profesional(profesional)
                .zonas(new HashSet<>(List.of(barrios).stream().map(zonas::get).toList()))
                .build();
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
