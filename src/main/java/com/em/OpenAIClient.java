package com.em;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class OpenAIClient {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = Config.openAIKey();
    private static final String ENCODING = "utf-8";
    private static final String GPT_MODEL = "gpt-3.5-turbo";
    private static final String PROMPT =
            "Extract banking details from the given SMS. Identify and return the following fields in JSON format:\n" +
                    "- **Bank Name**  \n" +
                    "- **Account Number (masked if available)**  \n" +
                    "- **Transaction Amount**  \n" +
                    "- **Transaction Date**  \n" +
                    "- **Transaction Type (Credit/Debit/UPI Transfer, etc.)**  \n" +
                    "- **Recipient (if applicable, UPI ID or name)**  \n" +
                    "\n" +
                    "If any field is missing or cannot be determined from the SMS, return an **empty string (\"\")** for that field instead of omitting it.\n" +
                    "Return only valid JSON output without any explanations.\n";

    public static Map<String, Object> extractExpenseDetails(String sms, LambdaLogger logger) throws IOException {
        try {
            HttpURLConnection conn = getConnection(sms);
            StringBuilder response = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), ENCODING))) {
                String line;
                while((line= reader.readLine())!=null){
                    response.append(line.trim());
                }
            }

            JsonNode gptResponse = mapper.readTree(response.toString());
            JsonNode maybeNull = gptResponse.at("/choices/0/message/content");
            if(!maybeNull.isMissingNode()){
                JsonNode extracted = mapper.readTree(maybeNull.asText().trim());
                return mapper.convertValue(extracted, new TypeReference<Map<String, Object>>() {});
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.log("An exception has occur : "+e);
        }
        return Collections.emptyMap();
    }

    private static HttpURLConnection getConnection(String sms) throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonRequest = getRequest(sms);
        try(OutputStream os = conn.getOutputStream()){
            byte[] inputBytes = jsonRequest.getBytes(ENCODING);
            os.write(inputBytes, 0, inputBytes.length);
        }
        return conn;
    }

    private static String getRequest(String sms) throws JsonProcessingException {
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", GPT_MODEL);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode prompt = mapper.createObjectNode();
        prompt.put("role", "system");
        prompt.put("content", PROMPT);

        ObjectNode message = mapper.createObjectNode();
        message.put("role","user");
        message.put("content",sms);
        messages.add(prompt);
        messages.add(message);

        requestBody.set("messages", messages);
        return mapper.writeValueAsString(requestBody);
    }


}

