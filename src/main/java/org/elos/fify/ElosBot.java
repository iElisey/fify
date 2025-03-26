package org.elos.fify;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.elos.fify.model.User;
import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;
import org.elos.fify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ElosBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final WordRepository wordRepository;
    private final UserService userService;
    private final Map<Long, TestSession> testSessions = new HashMap<>();
    private final Map<Long, String> tempWords = new HashMap<>();
    private final Map<Long, String> tempTopics = new HashMap<>();
    private static final long ADMIN_ID = 975340794L;
    private static final String API_URL = "https://api.aimlapi.com/v1/chat/completions";
    private static final String API_KEY = "01c7a757bd2d41b68824763f6633976c";

    @Autowired
    public ElosBot(WordRepository wordRepository, UserService userService) {
        this.userService = userService;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.wordRepository = wordRepository;
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
        if (update.hasInlineQuery()) {
            handleInlineQuery(update.getInlineQuery());
            return;
        }
        if (update.hasMessage() && update.getMessage().getText() != null) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String messageText = message.getText().trim();

            if (!userService.existsByChatId(chatId)) {
                welcome(chatId, message.getFrom().getUserName());
            } else {
                User user = userService.findByChatId(chatId);
                if (messageText.equalsIgnoreCase("/cancel") || messageText.equalsIgnoreCase("Скасувати")) {
                    testSessions.remove(chatId);
                    user.setPosition(0);
                    userService.save(user);
                    sendMsg(chatId, "✖️ Дію скасовано.");
                    return;
                }
                if (testSessions.containsKey(chatId)) {
                    testSessions.get(chatId).processAnswer(messageText);
                    if (testSessions.get(chatId).getCurrentIndex() == 10) {
                        testSessions.remove(chatId);
                    }
                    return;
                }
                // Ось де викликається trySendTranslation
                if (trySendTranslation(chatId, messageText)) {
                    return;
                }

                if (user.getPosition() == 0) {
                    handleCommand(chatId, messageText, user, message.getMessageId());
                } else {
                    handleUserInput(chatId, messageText, user, message.getMessageId());
                }
            }
        }
    }

    private void welcome(Long chatId, String username) {
        userService.add(chatId, username);
        sendMsg(chatId, "<b>👋 Ви зареєстровані!</b>\nВведіть /start, щоб почати.");
    }

    private void handleInlineQuery(InlineQuery inlineQuery) {
        String queryText = inlineQuery.getQuery().trim();
        if (queryText.isEmpty()) return;

        List<Word> words = wordRepository.findByEnglishStartingWithIgnoreCase(queryText);
        List<InlineQueryResultArticle> results = new ArrayList<>();

        for (Word word : words) {
            InputTextMessageContent messageContent = InputTextMessageContent.builder()
                    .messageText(word.getEnglish())
                    .build();

            InlineQueryResultArticle result = InlineQueryResultArticle.builder()
                    .id(UUID.randomUUID().toString())
                    .title(word.getEnglish())
                    .inputMessageContent(messageContent)
                    .build();

            results.add(result);
        }

        AnswerInlineQuery answer = AnswerInlineQuery.builder()
                .inlineQueryId(inlineQuery.getId())
                .results(results)
                .cacheTime(1)
                .build();
        try {
            telegramClient.execute(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean trySendTranslation(Long chatId, String word) {
        Optional<Word> foundWord = wordRepository.findFirstByEnglishIgnoreCaseOrUkrainianIgnoreCase(word, word);
        if (foundWord.isPresent()) {
            Word word1 = foundWord.get();
            String response = word1.getEnglish().equalsIgnoreCase(word)
                    ? "🇺🇸 " + word1.getEnglish() + " → 🇺🇦 " + word1.getUkrainian()
                    : "🇺🇦 " + word1.getUkrainian() + " → 🇺🇸 " + word1.getEnglish();

            String exampleSentence = askAI("generate a sentence B1 level (10-12 words) with word " + word + ", send only this sentence");
            response += "\n📖 Приклад: " + exampleSentence;
            sendMsg(chatId, response);
            return true;
        }
        return false;
    }

    private void handleCommand(Long chatId, String text, User user, Integer messageId) {
        switch (text.toLowerCase()) {
            case "/start":
                sendMsg(chatId, "<b>\uD83D\uDFE2 Відправка слів почалася</b> (тема: <i>" + user.getPreferredTopic() + "</i>).\n" +
                        "Аби змінити тему, напишіть:\n/change_topic");
                user.setSendWords(true);
                userService.save(user);
                break;
            case "/change_topic":
                sendMsg(chatId, "🌟 Виберіть тему слів:");
                showTopics(chatId);
                user.setPosition(1);
                userService.save(user);
                break;
            case "/stop":
                userService.setSendWords(chatId, false);
                sendMsg(chatId, "🛑 Відправка слів зупинена.");
                break;
            case "/test_words":
                sendMsg(chatId, "📝 Виберіть тему для тесту:");
                showTopics(chatId);
                user.setPosition(2);
                userService.save(user);
                break;
            case "/add_word":
                sendMessageWithCancel(chatId, "🇺🇸 Введіть слово англійською:");
                user.setPosition(3);
                userService.save(user);
                break;
            case "/web":
                if (chatId == ADMIN_ID) {
                    sendReplyMessage(chatId, "<a href=\"https://ielisey.github.io/fifyfront/\"><b>Web Link</b></a>", messageId);
                }
                break;
            case "/ai":
                sendMessageWithCancel(chatId, "ℹ️ Введіть питання для ШІ:");
                user.setPosition(5);
                userService.save(user);
                break;
            case "/analytics":
                if (chatId == ADMIN_ID) {
                    showAnalytics(chatId);
                }
                break;
            case "/load_words":
                if (chatId == ADMIN_ID) {
                    loadWordsFromFile(chatId);
                }
                break;
            case "/help":
                sendMsg(chatId, "⚙️ <b>Команди:</b>\n" +
                        "/start - почати відправку слів\n" +
                        "/stop - зупинити відправку\n" +
                        "/change_topic - змінити тему слів\n"+
                        "/test_words - пройти тест\n" +
                        "/add_word - додати слово\n" +
                        "/web - посилання (адмін)\n" +
                        "/ai - запитати ШІ\n" +
                        "/analytics - аналітика (адмін)\n" +
                        "/load_words - завантажити слова (адмін)");
                break;
            default:
                sendMsg(chatId, "❓ Невідома команда.");
        }
    }


    private void handleUserInput(Long chatId, String text, User user, Integer messageId) {
        if (user.getPosition() == 1) { // Вибір теми для відправки
            List<String> availableTopics = wordRepository.findAllTopics();
            if (availableTopics.contains(text)) {
                userService.setPreferredTopic(chatId, text);
                userService.setSendWords(chatId, true);
                sendMsg(chatId, "✅ Відправка слів почалася (тема: " + text + ").");
            } else {
                sendMsg(chatId, "❌ Теми '" + text + "' немає в базі даних. Виберіть іншу:\n" + String.join("\n", availableTopics));
                return; // Залишаємо користувача в стані вибору теми
            }
            user.setPosition(0);
        } else if (user.getPosition() == 2) { // Вибір теми для тесту
            startTest(chatId, text);
            user.setPosition(0);
            userService.save(user);
        } else if (user.getPosition() == 3) { // Додавання слова (англ)
            tempWords.put(chatId, text);
            sendMessageWithCancel(chatId, "🇺🇦 Введіть переклад українською:");
            user.setPosition(4);
            userService.save(user);
        } else if (user.getPosition() == 4) { // Додавання слова (укр)
            String english = tempWords.remove(chatId);
            wordRepository.save(new Word(english, text, "other"));
            sendMsg(chatId, "✅ Слово <i>" + english + "</i> додано!");
            user.setPosition(0);
            userService.save(user);
        } else if (user.getPosition() == 5) { // Запит до ШІ
            String answer = askAI(text);
            sendReplyMessage(chatId, answer, messageId);
            user.setPosition(0);
            userService.save(user);
        }
    }

    private void showAnalytics(Long chatId) {
        long wordCount = wordRepository.count();
        sendMsg(chatId, "<b>📊 Аналітика:</b>\nСлів у базі: " + wordCount);
    }

    private void loadWordsFromFile(Long chatId) {
        int updatedCount = 0;
        int addedCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("words.txt"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length >= 2) {
                    String english = parts[0].trim();
                    String ukrainian = parts[1].trim();
                    String topic = (parts.length > 2) ? parts[2].trim() : "other"; // Якщо тема відсутня, встановлюємо "other"

                    // Перевіряємо, чи слово вже є в базі
                    Optional<Word> existingWord = wordRepository.findByEnglish(english);

                    if (existingWord.isPresent()) {
                        Word word = existingWord.get();
                        if (!Objects.equals(word.getTopic(), topic) || word.getTopic() == null) { // Оновлюємо лише якщо тема змінилася
                            word.setTopic(topic);
                            wordRepository.save(word);
                            updatedCount++;
                        }
                    } else {
                        wordRepository.save(new Word(english, ukrainian, topic));
                        addedCount++;
                    }
                }
            }

            sendMsg(chatId, "<b>Оновлено слів: </b>" + updatedCount +
                    "\n<b>Додано нових слів: </b>" + addedCount +
                    "\nЗагальна кількість слів: " + wordRepository.count());
        } catch (Exception e) {
            System.err.println("Помилка завантаження слів: " + e.getMessage());
        }
    }

    private String askAI(String userMessage) {
        try {
            JSONObject requestBody = new JSONObject()
                    .put("model", "gpt-4o")
                    .put("messages", new JSONArray()
                            .put(new JSONObject().put("role", "system")
                                    .put("content", "You are an AI assistant who knows everything."))
                            .put(new JSONObject().put("role", "user")
                                    .put("content", userMessage)))
                    .put("max_tokens", 100);

            HttpResponse<JsonNode> response = Unirest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .asJson();

            return response.getBody().getObject()
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void showTopics(Long chatId) {
        List<String> topics = wordRepository.findAllTopics();
        StringBuilder sb = new StringBuilder("Доступні теми:\n");
        topics.forEach(topic -> sb.append("- ").append(topic).append("\n"));
        sendMsg(chatId, sb.toString());
    }

    @Scheduled(fixedRate = 10000)
    private void sendRandomWords() {
        List<User> activeUsers = userService.findAll().stream()
                .filter(User::isSendWords)
                .toList();

        if (activeUsers.isEmpty()) return;

        for (User user : activeUsers) {
            List<Word> words = new ArrayList<>();
            String preferredTopic = user.getPreferredTopic();
            List<Word> topicWords = wordRepository.findRandomWordsByTopic(preferredTopic, 9);
            if (topicWords.isEmpty()) {
                continue; // Пропускаємо користувача, якщо в обраній темі немає слів
            }
            words.addAll(topicWords); // 90%
            words.addAll(wordRepository.findRandomWordsExcludingTopic(preferredTopic, 1)); // 10%
            if (!words.isEmpty()) {
                Collections.shuffle(words);
                Word word = words.get(0);
                sendMsg(user.getChatId(), "✨ <i>" + word.getEnglish() + "</i> - <i>" + word.getUkrainian() + "</i>");
            }
        }
    }

    private void startTest(Long chatId, String topic) {
        List<Word> words = wordRepository.findRandomWordsByTopic(topic, 10);
        if (words.isEmpty()) {
            sendMsg(chatId, "⚠️ Немає слів для тесту за темою " + topic);
            return;
        }
        testSessions.put(chatId, new TestSession(chatId, words, telegramClient));
        testSessions.get(chatId).askNextWord();
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

    private void sendReplyMessage(Long chatId, String text, Integer messageId) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyToMessageId(messageId)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}