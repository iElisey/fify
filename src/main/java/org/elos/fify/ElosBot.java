package org.elos.fify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class ElosBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final JdbcTemplate jdbcTemplate;
    private final Set<Long> activeChatIds = new HashSet<>();
    private final Map<Long, TestSession> testSessions = new HashMap<>();

    @Autowired
    public ElosBot(DataSource dataSource) {
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        createTableIfNotExists();
        loadWordsFromFile(); // Завантажуємо слова з файлу при запуску
    }

    @Override
    public String getBotToken() {
        return "8017845979:AAGVDJgdDtk7ps0U2SiBjNX80hGEDVheFt4";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().getText() != null) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String messageText = message.getText().trim();

            if (testSessions.containsKey(chatId)) {
                testSessions.get(chatId).processAnswer(messageText);
                return;
            }

            switch (messageText.toLowerCase()) {
                case "/start":
                    start(chatId);
                    activeChatIds.add(chatId);
                    break;
                case "/stop":
                    activeChatIds.remove(chatId);
                    sendMsg(chatId, "🛑 Відправка слів зупинена.");
                    break;
                case "/test_words":
                    startTest(chatId);
                    break;
                default:
                    sendMsg(chatId, "❓ Невідома команда.");
            }
        }
    }

    private void start(Long chatId) {
        sendMsg(chatId, "🌟 <b>Ласкаво просимо!</b> Слова з’являтимуться кожні 10 секунд.\n/stop - зупинити\n/test_words - перевірити себе на знання 10 слів");
    }

    private void createTableIfNotExists() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS words (" +
                "id SERIAL PRIMARY KEY, " +
                "english VARCHAR(255) NOT NULL, " +
                "ukrainian VARCHAR(255) NOT NULL)");
    }

    private void loadWordsFromFile() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("words.txt"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String english = parts[0].trim();
                    String ukrainian = parts[1].trim();
                    // Перевіряємо, чи слово вже є в базі
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM words WHERE english = ? AND ukrainian = ?",
                            Integer.class, english, ukrainian);
                    if (count != null && count == 0) {
                        jdbcTemplate.update("INSERT INTO words (english, ukrainian) VALUES (?, ?)", english, ukrainian);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка завантаження слів: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 10000) // Кожні 10 секунд
    private void sendRandomWord() {
        if (activeChatIds.isEmpty()) return;

        List<Word> words = jdbcTemplate.query(
                "SELECT english, ukrainian FROM words",
                (rs, rowNum) -> new Word(rs.getString("english"), rs.getString("ukrainian")));
        if (!words.isEmpty()) {
            Collections.shuffle(words);
            Word randomWord = words.get(0);
            String message = "✨ <i>" + randomWord.english + "</i> - <i>" + randomWord.ukrainian + "</i>";
            for (Long chatId : activeChatIds) {
                sendMsg(chatId, message);
            }
        }
    }


    private void startTest(Long chatId) {
        List<Word> words = jdbcTemplate.query(
                "SELECT english, ukrainian FROM words ORDER BY RANDOM() LIMIT 10",
                (rs, rowNum) -> new Word(rs.getString("english"), rs.getString("ukrainian")));

        if (words.isEmpty()) {
            sendMsg(chatId, "⚠️ У базі немає слів для тесту.");
            return;
        }

        testSessions.put(chatId, new TestSession(chatId, words));
        testSessions.get(chatId).askNextWord();
    }

    private Message sendMsg(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        try {
            return telegramClient.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class TestSession {
        private final Long chatId;
        private final List<Word> words;
        private int currentIndex = 0;
        private int correctAnswers = 0;
        private int wrongAnswers = 0;

        TestSession(Long chatId, List<Word> words) {
            this.chatId = chatId;
            this.words = words;
        }

        void askNextWord() {
            if (currentIndex < words.size()) {
                sendMsg(chatId, "❓ Як перекладається: <b>" + words.get(currentIndex).english + "</b>?");
            } else {
                sendMsg(chatId, "✅ Тест завершено!\nПравильних відповідей: " + correctAnswers + "\nНеправильних відповідей: " + wrongAnswers);
                testSessions.remove(chatId);
            }
        }

        void processAnswer(String answer) {
            Word currentWord = words.get(currentIndex);
            if (answer.equalsIgnoreCase(currentWord.ukrainian)) {
                correctAnswers++;
                sendMsg(chatId, "✅ Правильно!");
            } else {
                wrongAnswers++;
                sendMsg(chatId, "❌ Неправильно. Правильна відповідь: <b>" + currentWord.ukrainian + "</b>");
            }
            currentIndex++;
            askNextWord();
        }
    }

    @AfterBotRegistration
    private void afterRegistration(BotSession botSession) {
        System.out.println("Bot running: " + botSession.isRunning());
    }

    private static class Word {
        String english;
        String ukrainian;

        Word(String english, String ukrainian) {
            this.english = english;
            this.ukrainian = ukrainian;
        }
    }
}