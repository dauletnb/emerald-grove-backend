package com.emeraldgrove.entity;

import com.emeraldgrove.entity.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a registered user account.
 * Password is never stored in plain text — only its BCrypt hash.
 */
@Entity
@Table(name = "users", schema = "emerald_grove")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 100)
    private String displayName;
}
