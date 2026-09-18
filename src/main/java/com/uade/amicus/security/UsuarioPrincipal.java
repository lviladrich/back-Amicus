package com.uade.amicus.security;

import com.uade.amicus.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad Usuario y lo que Spring Security entiende.
 *
 * Spring Security no conoce nuestra entidad: no sabe que la contrasena esta en
 * el campo password, que el login es por mail ni que tenemos un campo activo.
 * Lo que conoce es la interfaz UserDetails, un contrato con preguntas fijas:
 * cual es tu nombre de usuario, cual es tu contrasena hasheada, que permisos
 * tenes, estas habilitado. Esta clase responde esas preguntas mirando la
 * entidad.
 *
 * Se eligio un envoltorio en vez de hacer que Usuario implemente UserDetails
 * directamente. La entidad es un objeto de persistencia; atarla a una interfaz
 * de seguridad mezcla dos responsabilidades y hace que un cambio en Spring
 * Security obligue a tocar el modelo. Con el envoltorio, la entidad no sabe
 * que existe la seguridad.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    /** Acceso a la entidad para quien necesite el id u otros datos del dominio. */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * Permisos del usuario, en el formato que Spring Security entiende.
     *
     * Un rol se traduce en una GrantedAuthority con la convencion "ROLE_" +
     * nombre. El prefijo importa: es lo que permite escribir hasRole("ADMIN")
     * en las reglas de acceso cuando se cierre el SecurityFilterChain.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    /**
     * Para Spring Security, "username" es el identificador con el que se
     * autentica. En Amicus el login es por mail, asi que se devuelve el mail y
     * no el campo username de la entidad, que es un nombre visible.
     */
    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    /** Nuestro campo activo se traduce en enabled. Un usuario dado de baja no puede autenticarse. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getActivo());
    }

    /*
     * Los tres estados que siguen no existen en el dominio de Amicus (no hay
     * cuentas con vencimiento ni bloqueo por intentos fallidos), asi que se
     * responde siempre true. Devolver false en alguno haria que Spring Security
     * rechace la autenticacion.
     */

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
