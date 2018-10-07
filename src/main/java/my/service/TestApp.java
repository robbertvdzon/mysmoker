package my.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import my.service.commandstack.WriteService;
import my.service.commandstack.storage.SmokerCommandRepository;
import my.service.querystack.ReadService;
import my.service.querystack.storage.SmokerQueryRepository;

public class TestApp {

    public static void main(String[] args) {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
        SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository(amazonDynamoDB);
        SmokerQueryRepository smokerQueryRepository = new SmokerQueryRepository(amazonDynamoDB);
        WriteService writeService = new WriteService(smokerCommandRepository);
        ReadService readService = new ReadService(smokerQueryRepository);


//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.setTemp(3.1);
//        System.out.println(readService.lastsession());
        System.out.println(readService.listsessions());
//        System.out.println(smokerCommandRepository.loadState());
//        writeService.removeSession(1538224970903l);


    }
}
