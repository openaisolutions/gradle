package com.example.service;

import com.example.data.GoatRepository;
import com.example.library.Goat;

import java.util.List;

public class GoatService {
    public void registerGoat(String name, int age) {
        GoatRepository.addGoat(name, age);
    }

    public List<Goat> getGoats() {
        return GoatRepository.listGoats();
    }
}
