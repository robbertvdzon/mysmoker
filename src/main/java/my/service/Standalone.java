package my.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import my.service.rest.RestHandler;


public class Standalone {
    public static void main(String[] args) {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
        new RestHandler(amazonDynamoDB);
    }

}