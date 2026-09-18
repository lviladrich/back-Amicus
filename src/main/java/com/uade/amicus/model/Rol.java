package com.uade.amicus.model;

/**
 * Roles de Amicus.
 *
 * Un rol agrupa permisos: en vez de decidir persona por persona que puede
 * hacer cada una, se decide por categoria. Spring Security lo lee a traves de
 * UsuarioPrincipal.getAuthorities(), que traduce cada valor a "ROLE_" + nombre.
 *
 * Son dos y no mas a proposito:
 *
 * - USUARIO: cualquiera que se registra. Publica servicios y contrata los de
 *   otros, las dos cosas, porque en Amicus no hay tipos de persona distintos
 *   (decision de dominio: una sola entidad Usuario). Lo que el sistema
 *   controla para este rol es la propiedad: solo edita sus servicios, su
 *   carrito, sus ordenes y su perfil.
 *
 * - ADMIN: mantiene el catalogo del sistema, es decir las categorias y las
 *   zonas, que no pertenecen a ningun usuario. No se registra por la API: se
 *   siembra en CargaInicialDeDatos.
 *
 * No existe CLIENTE ni PROFESIONAL: publicar y contratar son actividades de
 * un mismo usuario, no roles.
 */
public enum Rol {
    USUARIO,
    ADMIN
}
