package ru.zolotuhin.bonus_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;
}
