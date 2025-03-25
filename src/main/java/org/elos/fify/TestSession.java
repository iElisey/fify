package org.elos.fify;

import lombok.Getter;
import org.elos.fify.model.Word;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;
import java.util.List;

@Getter
public class TestSession {
    private final Long chatId;
    private final List<Word> words;
    @Getter
    private int currentIndex = 0;
    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private final TelegramClient telegramClient;

    public TestSession(Long chatId, List<Word> words, TelegramClient telegramClient) {
        this.chatId = chatId;
        this.words = words;
        this.telegramClient = telegramClient;
    }

    public void askNextWord() {
        if (currentIndex < words.size()) {
            sendMsgWithButtonCancel(chatId, "❓ Як перекладається: <b>" + words.get(currentIndex).getEnglish() + "</b>?");
        } else {
            int percent = (int) Math.round(((double) correctAnswers / (correctAnswers + wrongAnswers)) * 100);
            String emoji = correctAnswers > 5 ? "\uD83D\uDC4C" : "\uD83D\uDC4E";
            sendMsg(chatId, emoji + " <b>Тест завершено!</b>\nПравильних відповідей: " + correctAnswers +
                    "\nНеправильних відповідей: " + wrongAnswers + "\nВідсоток правильних: " + percent + "%");
        }
    }

    public void processAnswer(String answer) {
        Word currentWord = words.get(currentIndex);
        if (answer.equalsIgnoreCase(currentWord.getUkrainian())) {
            correctAnswers++;
            sendMsg(chatId, "✅ Правильно!");
        } else {
            wrongAnswers++;
            sendMsg(chatId, "❌ Неправильно. Правильна відповідь: <b>" + currentWord.getUkrainian() + "</b>");
        }
        currentIndex++;
        askNextWord();
    }

    private void sendMsg(Long chatId, String text) {
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

    private void sendMsgWithButtonCancel(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        KeyboardRow row = new KeyboardRow();
        row.add("Скасувати");
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup
                .builder().keyboard(Arrays.asList(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}