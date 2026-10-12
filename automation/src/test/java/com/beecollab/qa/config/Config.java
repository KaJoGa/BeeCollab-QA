package com.beecollab.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config/local.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Unable to load config/local.properties: " + e.getMessage());
        }
    }

    public static String get(String key, String defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static String getBaseUrl() {
        return get("base.url", "https://beecollab.vercel.app");
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(get("headless", "true"));
    }

    public static int getExplicitTimeout() {
        return Integer.parseInt(get("timeout.explicit", "15"));
    }

    public static int getWindowWidth() {
        return Integer.parseInt(get("window.width", "1280"));
    }

    public static int getWindowHeight() {
        return Integer.parseInt(get("window.height", "900"));
    }
}
