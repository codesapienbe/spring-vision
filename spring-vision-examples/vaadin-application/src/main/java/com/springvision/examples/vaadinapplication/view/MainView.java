package com.springvision.examples.vaadinapplication.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Main entry point for Vaadin UI.
 */
@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        Button button = new Button("Click me");
        button.addClickListener(e -> button.setText("Clicked"));
        add(button);
    }
}
