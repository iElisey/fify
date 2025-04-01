package org.elos.fify.controller;

import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*") // Разрешить запросы с Vue (порт 8081)
@RestController
@RequestMapping("api")


public class ApiController {
    private final WordRepository wordRepository;
    private final TelegramClient telegramClient;

    private static final String CORRECT_USERNAME = "eloseng";
    private static final String CORRECT_PASSWORD = "7777";

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

    private Map<String, Integer> loginAttempts = new HashMap<>();
    private List<String> blockedIPs = new ArrayList<>();

    @PostMapping("/login")
    public ResponseEntity<Map<String, Boolean>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        String ip = credentials.get("ip");

        Map<String, Boolean> response = new HashMap<>();

        if (blockedIPs.contains(ip)) {
            response.put("success", false);
            return ResponseEntity.ok(response);
        }

        if (username != null && password != null &&
                username.equals(CORRECT_USERNAME) && password.equals(CORRECT_PASSWORD)) {
            loginAttempts.remove(ip); // Reset attempts on success
            response.put("success", true);
        } else {
            int attempts = loginAttempts.getOrDefault(ip, 0) + 1;
            loginAttempts.put(ip, attempts);
            if (attempts >= 3) {
                blockedIPs.add(ip);
                loginAttempts.remove(ip);
            }
            response.put("success", false);
        }

        return ResponseEntity.ok(response);
    }
}
