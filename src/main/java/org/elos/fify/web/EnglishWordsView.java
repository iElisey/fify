package org.elos.fify.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.elos.fify.model.Word;
import org.elos.fify.repository.WordRepository;

@Route("english-words")
@PageTitle("English Words")
public class EnglishWordsView extends VerticalLayout {
    private final WordRepository wordRepository;
    private final Grid<Word> grid = new Grid<>(Word.class);
    private final H4 h4 = new H4("Add new word");
    private final TextField english = new TextField("English");
    private final TextField ukrainian = new TextField("Ukrainian");
    private final Button saveButton = new Button("Save");

    public EnglishWordsView(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
        updateList();

        saveButton.addClickListener(e -> {
            Word word = new Word();
            word.setEnglish(english.getValue());
            word.setUkrainian(ukrainian.getValue());
            wordRepository.save(word);
            updateList();
        });

        grid.addComponentColumn(word -> {
            Button editButton = new Button("Edit", event -> addEditDialog(word));
            Button deleteButton = new Button("Delete", event -> {
                wordRepository.delete(word);
                updateList();
            });
            HorizontalLayout buttons = new HorizontalLayout(editButton, deleteButton);
            return buttons;
        }).setHeader("Actions");



        add(grid, h4, english, ukrainian, saveButton);
    }
    private void addEditDialog(Word word) {
        Dialog editDialog = new Dialog();
        TextField englishField = new TextField("English", word.getEnglish());
        englishField.setValue(word.getEnglish());
        TextField ukrainianField = new TextField("Ukrainian", word.getUkrainian());
        ukrainianField.setValue(word.getUkrainian());
        Button saveButton = new Button("Save", e -> {
            word.setEnglish(englishField.getValue());
            word.setUkrainian(ukrainianField.getValue());
            wordRepository.save(word);
            updateList();
            editDialog.close();
        });
        editDialog.add(englishField, ukrainianField, saveButton);
        editDialog.open();
    }
    private void updateList() {
        grid.setItems(wordRepository.findAll());
        grid.getColumnByKey("id").setVisible(false); // Сховати колонку ID

    }
}
