package org.elos.fify;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class MapperBean {

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient("8017845979:AAGVDJgdDtk7ps0U2SiBjNX80hGEDVheFt4");
    }

}