package com.example.service;

import com.example.data.Repository;
import com.example.library.Library;

public class Service {
  public static String message() {
    return Repository.getData() + " " + Library.lib();
  }
}
