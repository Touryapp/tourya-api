package com.tourya.api.repository;

import com.tourya.api.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    @Query("""
            SELECT user
            FROM User user
            Where ( (:firstName IS NULL ) OR ( lower(  cast(user.firstname as string) ) like  lower(  cast(concat('%', :firstName,'%') as string)  )  )) 
            """)
    Page<User> findAllUser(@Param("firstName") String firstName, Pageable pageable);
}
