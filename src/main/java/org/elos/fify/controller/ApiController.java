package org.elos.fify.controller;

import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*") // Разрешить запросы с Vue (порт 8081)
@RestController
@RequestMapping("api")

public class ApiController {
    private final WordRepository wordRepository;
    private final TelegramClient telegramClient;

    @Autowired
    public ApiController(WordRepository wordRepository, TelegramClient telegramClient) {
        this.wordRepository = wordRepository;
        this.telegramClient = telegramClient;
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
        if (word.getTopic() == null || word.getTopic().isEmpty()) {
            word.setTopic("other");
        }
        wordRepository.save(word);
    }

    @PostMapping("/words")
    public void addWords(@RequestBody List<Word> words) {
        List<Word> wordList = wordRepository.saveAll(words);
// Виділяємо всі унікальні теми зі списку wordList
        List<String> topics = wordList.stream()
                .map(Word::getTopic) // Отримуємо тему кожного слова
                .distinct()          // Видаляємо дублікати
                .toList();
        SendMessage sendMessage = SendMessage.builder()

                .text("<b>Додано нові слова!</b>\nЗагальна кількість: " + wordList.size() + "\n" +
                        "Теми: " + String.join(", ", topics))
                .chatId(975340794L)
                .parseMode("HTML").build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
