package org.elos.fify.service;

import org.elos.fify.model.User;
import org.elos.fify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public boolean existsByChatId(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }

    public User findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public void add(Long chatId, String username) {
        User user = new User();
        user.setChatId(chatId);
        user.setUsername(username);
        user.setPosition(0);
        userRepository.save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }
}
