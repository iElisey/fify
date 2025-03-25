package org.elos.fify.controller;

import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*") // Разрешить запросы с Vue (порт 8081)
@RestController
@RequestMapping("api")
public class ApiController {
    private final WordRepository wordRepository;

    @Autowired
    public ApiController(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    @GetMapping
    public List<Word> getWords() {
        return wordRepository.findAll();
    }

    @DeleteMapping
    public void deleteWord(@RequestParam Long id) {
        wordRepository.deleteById(id);
    }

    @PutMapping
    public Word updateWord(@RequestBody Word word) {
        return wordRepository.save(word);
    }

    @PostMapping
    public void addWord(@RequestBody Word word) {
        wordRepository.save(word);
    }
}
