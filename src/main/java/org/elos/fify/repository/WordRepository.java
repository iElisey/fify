package org.elos.fify.repository;

import org.elos.fify.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findFirstByEnglishIgnoreCaseOrUkrainianIgnoreCase(String english, String ukrainian);
    List<Word> findByEnglishStartingWithIgnoreCase(String prefix);
    List<Word> findAll();
    boolean existsByEnglishAndUkrainian(String english, String ukrainian);
    
    @Query(value = "SELECT * FROM words ORDER BY RANDOM() LIMIT 10", nativeQuery = true)
    List<Word> findRandom10Words();

    Optional<Word> findByEnglishAndUkrainian(String english, String ukrainian);

    boolean existsByEnglish(String english);
}