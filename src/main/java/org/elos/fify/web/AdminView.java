package org.elos.fify.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.elos.fify.model.User;
import org.elos.fify.repository.UserRepository;

@Route("users")
@PageTitle("Admin Panel")
public class AdminView extends VerticalLayout {
    private final UserRepository userRepository;
    private final Grid<User> grid = new Grid<>(User.class);
    private final TextField usernameField = new TextField("Username");
    private final TextField chatIdField = new TextField("ChatId");
    private final Button saveButton = new Button("Save");

    public AdminView(UserRepository userRepository) {
        this.userRepository = userRepository;
        updateList();



        saveButton.addClickListener(e -> {
            User user = new User();
            user.setUsername(usernameField.getValue());
            user.setChatId(Long.valueOf(chatIdField.getValue()));
            userRepository.save(user);
            updateList();
        });
        grid.addComponentColumn(user -> {
            Button editButton = new Button("Edit", event -> addEditDialog(user));
            Button deleteButton = new Button("Delete", event -> {
                userRepository.delete(user);
                updateList();
            });
            HorizontalLayout buttons = new HorizontalLayout(editButton, deleteButton);
            return buttons;
        }).setHeader("Actions");


        add(grid, usernameField, chatIdField, saveButton);
    }
    private void addEditDialog(User user) {
        Dialog editDialog = new Dialog();
        TextField usernameField = new TextField("Username", user.getUsername());
        TextField chatIdField = new TextField("ChatId", String.valueOf(user.getChatId()));
        Button saveButton = new Button("Save", e -> {
            user.setUsername(usernameField.getValue());
            user.setChatId(Long.valueOf(chatIdField.getValue()));
            userRepository.save(user);
            updateList();
            editDialog.close();
        });
        editDialog.add(usernameField, chatIdField, saveButton);
        editDialog.open();
    }
    private void updateList() {
        grid.setItems(userRepository.findAll());
    }
}
