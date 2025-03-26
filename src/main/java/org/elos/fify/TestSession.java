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
        if (answer.equalsIgnoreCase(currentWord.getUkrainian())) {
            correctAnswers++;
            sendMessage(chatId, "✅ Правильно!");
        } else {
            sendMessage(chatId, "❌ Неправильно. Правильна відповідь: <b>" + currentWord.getUkrainian() + "</b>");
        }
        currentIndex++;
        askNextWord();
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