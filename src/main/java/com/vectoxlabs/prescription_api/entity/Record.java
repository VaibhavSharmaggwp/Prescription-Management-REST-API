package com.vectoxlabs.prescription_api.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FetchType.LAZY is an industry standard to prevent N+1 query performance problems
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private LocalDateTime recordDate;

    @Column(nullable = false)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    @Column(unique = true)
    private String idempotencyKey;

    // CascadeType.ALL means saving the Record automatically saves the Entries
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordEntry> entries = new ArrayList<>();


    // Utility method to keep the bidirectional relationship in sync
    public void addEntry(RecordEntry entry){
        entries.add(entry);
        entry.setRecord(this);
    }
}
