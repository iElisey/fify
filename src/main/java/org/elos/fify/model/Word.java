package org.elos.fify.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "words")
@Getter
@Setter
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String english;
    
    private String ukrainian;

    public Word(String english, String ukrainian) {
        this.english = english;
        this.ukrainian = ukrainian;
    }

    public Word(Long id, String english, String ukrainian) {
        this.id = id;
        this.english = english;
        this.ukrainian = ukrainian;
    }

    public Word() {

    }

    public String getEnglish() {
        return english;
    }

    public Long getId() {
        return id;
    }

    public String getUkrainian() {
        return ukrainian;
    }
}