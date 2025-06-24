package com.example.library;

import com.example.common.Util;

public class Library {
    public static String lib() {
        return Util.greet("Library");
    }

    public static Goat newGoat(String name, int age) {
        return new Goat(name, age);
    }
}
