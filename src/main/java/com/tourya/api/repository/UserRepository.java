package com.tourya.api.repository;

import com.tourya.api.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT user
            FROM User user
            Where ( (:firstName IS NULL ) OR ( lower(  cast(user.firstname as string) ) like  lower(  cast(concat('%', :firstName,'%') as string)  )  ))
            """)
    Page<User> findAllUser(@Param("firstName") String firstName, Pageable pageable);

    /**
     * FE-15d: usuarios que tienen asignado un rol especifico. Se usa para listar
     * BACKOFFICE_OPERATION en el panel admin. Ordena por id ASC.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
            ORDER BY u.id ASC
            """)
    List<User> findByRoleName(@Param("roleName") String roleName);
}
