package com.vectoxlabs.prescription_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "record_entries")
@Getter
@Setter
@NoArgsConstructor
public class RecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;


    @Column(nullable = false)
    private String entryName;


    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String frequency;


    @Column(nullable = false)
    private String duration;

    private String instructions;
}
