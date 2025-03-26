package org.elos.fify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String topic = "other";

    public Word(String english, String ukrainian) {
        this.english = english;
        this.ukrainian = ukrainian;
    }

    public Word(String english, String ukrainian, String topic) {
        this.english = english;
        this.ukrainian = ukrainian;
        this.topic = topic;
    }


}