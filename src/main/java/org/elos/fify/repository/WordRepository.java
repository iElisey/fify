package org.elos.fify.repository;

import org.elos.fify.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findFirstByEnglishIgnoreCaseOrUkrainianIgnoreCase(String english, String ukrainian);
    List<Word> findByEnglishStartingWithIgnoreCase(String prefix);
    List<Word> findAll();
    List<Word> findByTopic(String topic);

    @Query(value = "SELECT * FROM words WHERE topic = :topic ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsByTopic(String topic, int limit);

    @Query(value = "SELECT * FROM words WHERE topic != :topic ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Word> findRandomWordsExcludingTopic(String topic, int limit);

    @Query(value = "SELECT DISTINCT topic FROM words", nativeQuery = true)
    List<String> findAllTopics();

    boolean existsByEnglishAndUkrainian(String english, String ukrainian);

    List<Word> findAllByOrderByCreatedAtDesc();

    @Query("SELECT w FROM Word w WHERE w.topic = :topic ORDER BY w.createdAt DESC NULLS LAST")
    List<Word> findTop3ByTopicOrderByCreatedAtDesc(@Param("topic") String topic);

    Optional<Word> findByEnglish(String english);
}