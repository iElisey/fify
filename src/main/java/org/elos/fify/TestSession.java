package org.elos.fify;

import lombok.Getter;
import org.elos.fify.model.Word;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;
import java.util.List;

@Getter
public class TestSession {
    private final Long chatId;
    private final List<Word> words;
    private int currentIndex = 0;
    private int correctAnswers = 0;
    private final TelegramClient telegramClient;

    public TestSession(Long chatId, List<Word> words, TelegramClient telegramClient) {
        this.chatId = chatId;
        this.words = words;
        this.telegramClient = telegramClient;
    }

    public void askNextWord() {
        if (currentIndex < words.size()) {
            sendMessageWithCancel(chatId, "❓ Як перекладається: <b>" + words.get(currentIndex).getEnglish() + "</b>?");
        } else {
            int percent = (int) Math.round(((double) correctAnswers / words.size()) * 100);
            String emoji = correctAnswers > words.size() / 2 ? "👍" : "👎";
            sendMessage(chatId, emoji + " <b>Тест завершено!</b>\nПравильних: " + correctAnswers +
                    " з " + words.size() + " (" + percent + "%)");
        }
    }

    public void processAnswer(String answer) {
        Word currentWord = words.get(currentIndex);
        String correctAnswer = currentWord.getUkrainian().toLowerCase();
        String userAnswer = answer.trim().toLowerCase();

        // Точна відповідність
        if (userAnswer.equals(correctAnswer)) {
            correctAnswers++;
            sendMessage(chatId, "✅ Правильно!");
        } else {
            // Перевіряємо відстань Левенштейна
            int distance = calculateLevenshteinDistance(userAnswer, correctAnswer);
            double similarity = 1.0 - (double) distance / Math.max(userAnswer.length(), correctAnswer.length());

            if (similarity >= 0.85 && distance <= 2) { // 85% схожості та не більше 2 помилок
                correctAnswers++;
                sendMessage(chatId, "✅ Правильно!");
            } else {
                sendMessage(chatId, "❌ Неправильно. Правильна відповідь: <b>" + currentWord.getUkrainian() + "</b>");
            }
        }

        currentIndex++;
        askNextWord();
    }

    // Метод для обчислення відстані Левенштейна
    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessageWithCancel(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboardRow(new KeyboardRow(KeyboardButton.builder().text("Скасувати").build()))
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(true)
                        .build())
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}