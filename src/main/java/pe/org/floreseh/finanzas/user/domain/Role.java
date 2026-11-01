package pe.org.floreseh.finanzas.user.domain;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static pe.org.floreseh.finanzas.user.domain.Permission.AUDIT_READ;
import static pe.org.floreseh.finanzas.user.domain.Permission.USER_DELETE;
import static pe.org.floreseh.finanzas.user.domain.Permission.USER_READ;
import static pe.org.floreseh.finanzas.user.domain.Permission.USER_WRITE;

public enum Role {
    ADMIN(Set.of(USER_READ, USER_WRITE, USER_DELETE, AUDIT_READ)),
    OPERATOR(Set.of(USER_READ, USER_WRITE, AUDIT_READ)),
    CUSTOMER(Set.of(USER_READ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public List<SimpleGrantedAuthority> getAuthorities() {
        var permissionAuthorities = permissions.stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermission()));

        var roleAuthority = Stream.of(new SimpleGrantedAuthority("ROLE_" + this.name()));

        return Stream.concat(permissionAuthorities, roleAuthority)
                .toList();
    }
}
