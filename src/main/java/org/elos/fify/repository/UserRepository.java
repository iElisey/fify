package org.elos.fify.repository;


import org.elos.fify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByChatId(Long chatId);

    User findByChatId(Long chatId);
}
