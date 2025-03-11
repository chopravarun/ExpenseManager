package com.em;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private static final String OPEN_AI_KEY = "openAI.apiKey";
    private static final String USER_TOKEN = "user.token";

    private static final Dotenv env = Dotenv.load();

    public static String openAIKey() {
        return env.get(OPEN_AI_KEY);
    }

    public static String userToken() {
        return env.get(USER_TOKEN);
    }
}
