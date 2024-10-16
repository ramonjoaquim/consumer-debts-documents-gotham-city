package com.gotham.joker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "dividas")
public class Divida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String hashDocumento;

    @Column(nullable = true)
    private String hashAssinatura;

    @Column(nullable = true)
    private Boolean executouScript;

}
