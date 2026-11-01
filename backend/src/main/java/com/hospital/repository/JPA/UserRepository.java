package com.hospital.repository.JPA;

import com.hospital.model.EntidadesJPA.User;
import com.hospital.model.EntidadesJPA.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UserId> {

    @EntityGraph(attributePaths = "dataUser")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "dataUser")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
