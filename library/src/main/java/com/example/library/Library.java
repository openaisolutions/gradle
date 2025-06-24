package com.example.library;

import com.example.common.Util;

public class Library {
    public static String lib() {
        return Util.greet("Library");
    }

    public static String getMessage() {
        return lib();
    }
}
