package com.example.library;

public class Goat {
    private final String name;
    private final int age;

    public Goat(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return name + " (" + age + " years)";
    }
}
