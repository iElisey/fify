package org.elos.fify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "words")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String english;

    private String ukrainian;


    @Column(name = "created_at")
    private LocalDateTime createdAt;


    private String topic = "other";

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Word(String english, String ukrainian) {
        this.english = english;
        this.ukrainian = ukrainian;
        this.createdAt = LocalDateTime.now();
    }

    public Word(String english, String ukrainian, String topic) {
        this.english = english;
        this.ukrainian = ukrainian;
        this.topic = topic;
        this.createdAt = LocalDateTime.now();
    }


}