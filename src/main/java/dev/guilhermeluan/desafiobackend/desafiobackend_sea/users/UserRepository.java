package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select uf from User u join u.coverageUfs uf where u.email = :email")
    Set<String> findCoverageUfsByEmail(@Param("email") String email);
}
