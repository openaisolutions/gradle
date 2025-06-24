package com.example.data;

import com.example.library.Goat;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoatRepository {
    private static final List<Goat> GOATS = new ArrayList<>();

    public static void addGoat(String name, int age) {
        GOATS.add(new Goat(StringUtils.capitalize(name), age));
    }

    public static List<Goat> listGoats() {
        return Collections.unmodifiableList(GOATS);
    }
}
