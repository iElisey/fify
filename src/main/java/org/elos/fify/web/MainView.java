package org.elos.fify.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        // Заголовок сторінки
        H1 title = new H1("Welcome to Database Management");

        // Опис
        Paragraph description = new Paragraph("You can manage your database here. Choose a table to view or edit:");

        // Кнопка для переходу до таблиці користувачів
        Button usersButton = new Button("Manage Users", event -> getUI().ifPresent(ui -> ui.navigate("users")));
        usersButton.addClassName("main-button");

        // Кнопка для переходу до таблиці слів
        Button wordsButton = new Button("Manage Words", event -> getUI().ifPresent(ui -> ui.navigate("english-words")));
        wordsButton.addClassName("main-button");

        // Додавання всіх елементів на сторінку
        add(title, description, usersButton, wordsButton);

        // Додавання класів для стилізації
        setAlignItems(Alignment.CENTER);
        setSizeFull();
    }
}
