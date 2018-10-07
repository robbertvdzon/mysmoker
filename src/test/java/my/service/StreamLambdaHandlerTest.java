package my.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import my.service.commandstack.WriteService;
import my.service.commandstack.storage.SmokerCommandRepository;
import my.service.querystack.ReadService;
import my.service.querystack.storage.SmokerQueryRepository;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class StreamLambdaHandlerTest {

    private SmokerCommandRepository smokerCommandRepository;
    private SmokerQueryRepository smokerQueryRepository;
    private WriteService writeService;
    private ReadService readService;


    @ClassRule
    public static LocalDbCreationRule dynamoDB = new LocalDbCreationRule();

    @Before
    public void setup() {
        AmazonDynamoDB dynamodb = DynamoDBEmbedded.create().amazonDynamoDB();
        smokerCommandRepository = new SmokerCommandRepository(dynamodb);
        smokerQueryRepository = new SmokerQueryRepository(dynamodb);
        writeService = new WriteService(smokerCommandRepository);
        readService = new ReadService(smokerQueryRepository);

    }

    @Test
    public void test1() {
        writeService.setTemp(2.0);
        System.out.println("temp=" + readService.getTemp());
        writeService.add(1.0, 2.0, 3.0, 4.0);
        readService.listsessions().stream().forEach(s -> System.out.println(s));

    }

    @Test
    public void test2() {
//        writeService.setTemp(2.0);
        writeService.add(1.0, 2.0, 3.0, 4.0);
        System.out.println("temp2=" + readService.getTemp());
//        writeService.add(1.0, 2.0, 3.0, 4.0);
        readService.listsessions().stream().forEach(s -> System.out.println(s));

    }

}