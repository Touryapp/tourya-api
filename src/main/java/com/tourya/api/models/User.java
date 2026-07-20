package com.tourya.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, Principal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String firstname;
    private String lastname;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(unique = true)
    private String email;
    private String password;
    private boolean accountLocked;
    private boolean enabled;
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;
    @Column(name = "uuid_social")
    private String uuidSocial;

    /**
     * BE-22d: telefono de contacto del usuario. Usado por PROVIDER_OPERATOR
     * como contacto principal del tour (RN-026). Nullable — hasta que el usuario
     * lo cargue, el TourPrincipalOperatorService cae al provider.phone.
     */
    private String phone;

    /**
     * Contador de intentos de login fallidos desde el ultimo login exitoso.
     * Se resetea a 0 en cada success. Usado por SEC-10 para lockout automatico.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * Timestamp hasta el cual la cuenta queda bloqueada por lockout automatico.
     * NULL = no bloqueada por lockout. Distinto de accountLocked (bloqueo
     * permanente por ADMIN). Cuando expira, isAccountNonLocked() vuelve a true.
     */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles", // Nombre de la tabla de unión
            joinColumns = @JoinColumn(name = "user_id"), // Columna que referencia a User
            inverseJoinColumns = @JoinColumn(name = "role_id") // Columna que referencia a Role
    )
    private List<Role> roles;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    @Column( name = "last_modified_date", insertable = false)
    private LocalDateTime lastModifiedDate;

    @Override
    public String getName() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLocked) {
            return false;
        }
        // Lockout automatico temporal (SEC-10): la cuenta queda bloqueada mientras
        // locked_until > now(). Cuando expira, se desbloquea automaticamente sin
        // necesidad de un job de cleanup: solo cambia el resultado de este metodo.
        if (lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
    public String fullName(){
        if (lastname != null && !lastname.trim().isEmpty()) {
            return firstname + " " + lastname;
        } else {
            return firstname;
        }
    }
    public String getUuidSocial() {
        return uuidSocial;
    }
}
