package com.example.ui;

import com.example.service.Service;
import javax.swing.*;

public class AppWindow {
  public static void show() {
    JFrame frame = new JFrame("Cookbook UI");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JLabel label = new JLabel("Service says: " + Service.message());
    frame.add(label);
    frame.pack();
    frame.setVisible(true);
  }
}
