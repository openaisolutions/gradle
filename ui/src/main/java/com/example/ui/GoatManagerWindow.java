package com.example.ui;

import com.example.service.GoatService;
import com.example.library.Goat;

import javax.swing.*;
import java.awt.*;

public class GoatManagerWindow {
    private final GoatService service = new GoatService();

    public void show() {
        JFrame frame = new JFrame("Goat Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        frame.add(new JScrollPane(list), BorderLayout.CENTER);

        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();
        JButton addButton = new JButton("Add Goat");
        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText();
                int age = Integer.parseInt(ageField.getText());
                service.registerGoat(name, age);
                model.clear();
                for (Goat g : service.getGoats()) {
                    model.addElement(g.toString());
                }
                nameField.setText("");
                ageField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid age");
            }
        });

        JPanel input = new JPanel(new GridLayout(3,2));
        input.add(new JLabel("Name:"));
        input.add(nameField);
        input.add(new JLabel("Age:"));
        input.add(ageField);
        input.add(addButton);

        frame.add(input, BorderLayout.SOUTH);
        frame.pack();
        frame.setSize(300, 200);
        frame.setVisible(true);
    }
}
