package org.elos.fify;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class ElosBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final WordRepository wordRepository;
    private final Set<Long> activeChatIds = new HashSet<>();
    private final Map<Long, TestSession> testSessions = new HashMap<>();
    private static final String API_URL = "https://api.aimlapi.com/v1/chat/completions";
    private static final String API_KEY = "01c7a757bd2d41b68824763f6633976c";
    private static final Long adminId = 975340794L;
    @Autowired
    public ElosBot(WordRepository wordRepository) {
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.wordRepository = wordRepository;
        loadWordsFromFile();
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

            if (testSessions.containsKey(chatId)) {
                testSessions.get(chatId).processAnswer(messageText);
                return;
            }

            if (trySendTranslation(chatId, messageText)) {
                return;
            }
            if(messageText.startsWith("/add_word")){
                String englishAndUkr = messageText.split(" ")[1];
                String english = englishAndUkr.split("=")[0];
                String ukrainian = englishAndUkr.split("=")[1];
                Word word = new Word(english,ukrainian);
                Word savedWord = wordRepository.save(word);
                sendMsg(chatId, "✅ Word "+savedWord.getEnglish()+" successfully added to database!");
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
                case "/analytics":
                    if (chatId==adminId) {
                        return;
                    }
                    showAnalytics(chatId);
                    break;
                default:
                    sendMsg(chatId, "❓ Невідома команда.");
            }
        }
    }

    private void showAnalytics(Long chatId) {
        List<Word> words = wordRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>\uD83D\uDCCA Analytic:</b>\n");
        sb.append("Words amount: ").append(words.size());
        sendMsg(chatId, sb.toString());
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

    public static String askAI(String userMessage) {
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


    private void start(Long chatId) {
        sendMsg(chatId, "🌟 <b>Ласкаво просимо!</b> Слова з’являтимуться кожні 10 секунд.\n/stop - зупинити\n/test_words - перевірити себе на знання 10 слів");
    }

    private void loadWordsFromFile() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("words.txt"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 1) {
                    String english = parts[0].trim();

                    // Збираємо всі варіанти значення в українській мові (все після першого елемента)
                    String ukrainian = String.join(", ", Arrays.copyOfRange(parts, 1, parts.length)).trim();

                    // Перевіряємо чи вже існує запис для цього англійського слова
                    if (!wordRepository.existsByEnglish(english)) {
                        wordRepository.save(new Word(english, ukrainian));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка завантаження слів: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 10000)
    private void sendRandomWord() {
        if (activeChatIds.isEmpty()) return;

        List<Word> words = wordRepository.findAll();
        if (!words.isEmpty()) {
            Collections.shuffle(words);
            Word randomWord = words.get(0);
            String message = "✨ <i>" + randomWord.getEnglish() + "</i> - <i>" + randomWord.getUkrainian() + "</i>";
            for (Long chatId : activeChatIds) {
                sendMsg(chatId, message);
            }
        }
    }

    private void startTest(Long chatId) {
        List<Word> words = wordRepository.findRandom10Words();
        if (words.isEmpty()) {
            sendMsg(chatId, "⚠️ У базі немає слів для тесту.");
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

    @AfterBotRegistration
    private void afterRegistration(BotSession botSession) {
        System.out.println("Bot running: " + botSession.isRunning());
    }
}