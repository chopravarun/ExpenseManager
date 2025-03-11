package com.em;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecordExpense implements RequestHandler<Map<String, Object>, String> {

    private static final Logger log = LoggerFactory.getLogger(RecordExpense.class);
    private final DynamoDbClient dbClient;

    private LambdaLogger logger;

    private final String token;

    public RecordExpense() {
        this.token = Config.userToken();
        dbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Override
    public String handleRequest(Map<String, Object> payload, Context context) {
        this.logger = context.getLogger();
        try{
            if(!token.equalsIgnoreCase(payload.get("user").toString())){
                return "Not authorised";
            }
            String sms = payload.get("sms").toString();
            logger.log("sms content : "+sms);
            Map<String, Object> extractMsg = OpenAIClient.extractExpenseDetails(sms, logger);
            logger.log("translated message : "+extractMsg);
            Map<String, AttributeValue> dbRecord = createDBRecord(extractMsg, sms);
            saveExpense(dbRecord);
        } catch (Exception e) {
            logger.log("Error : "+e);
            return getStackTraceAsString(e);
        }
        return "SMS processed";
    }

    public  String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }


    private Map<String, AttributeValue> createDBRecord(Map<String, Object> msgContent, String sms) throws IOException {
        Map<String, AttributeValue> dbRecord = new HashMap<>();
        dbRecord.put("time", AttributeValue.builder().n(getEpochTime()).build());
        if(!msgContent.isEmpty()) {
            boolean isTranslated = isValidMsg(msgContent);
            dbRecord.put("translated", AttributeValue.builder().bool(isTranslated).build());
            msgContent.forEach((key, value) ->
                    dbRecord.put(key, AttributeValue.builder().s(value.toString()).build())
            );
        } else {
            dbRecord.put("translated", AttributeValue.builder().bool(false).build());
        }
        dbRecord.put("sms", AttributeValue.builder().s(sms).build());
        return dbRecord;
    }

    private boolean isValidMsg(Map<String, Object> msgContent) {
        Set<String> mandatory = Set.of("Bank Name","Account Number", "Transaction Amount", "Transaction Type");
        return msgContent.entrySet()
                .stream()
                .filter(entry->mandatory.contains(entry.getKey()))
                .filter(entry-> String.valueOf(entry.getValue()).trim().isEmpty())
                .findFirst().isEmpty();

    }

    private void saveExpense(Map<String, AttributeValue> message){
        PutItemRequest writeRequest = PutItemRequest.builder()
                .tableName("finance")
                .item(message)
                .build();
        PutItemResponse response = dbClient.putItem(writeRequest);
        String responseString = " isSuccess : " + response.sdkHttpResponse().isSuccessful() +
                " httpCode : " + response.sdkHttpResponse().statusCode() +
                " httpText : " + response.sdkHttpResponse().statusText().orElse("");
        logger.log(responseString);
    }

    private String getEpochTime() {
        return String.valueOf(Instant.now().toEpochMilli());
    }


}
