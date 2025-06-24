package com.example.data;

import com.example.shared.SharedUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class Repository {
  public static String getData() {
    String base = StringUtils.capitalize(SharedUtil.shared());
    try {
      File tmp = File.createTempFile("sample", ".txt");
      FileUtils.write(tmp, base, StandardCharsets.UTF_8);
      return FileUtils.readFileToString(tmp, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return base;
    }
  }
}
