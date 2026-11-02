package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tb_user_coverage", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "uf", nullable = false, length = 2, columnDefinition = "char(2)")
    private Set<String> coverageUfs = new HashSet<>();

    public User() {
    }

    public User(Long id, String email, String passwordHash, Role role) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
