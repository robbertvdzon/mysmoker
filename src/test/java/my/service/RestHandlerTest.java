package my.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import my.service.rest.RestHandler;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

public class RestHandlerTest {

    @ClassRule
    public static LocalDbCreationRule localDynamoDB = new LocalDbCreationRule();

    @Before
    public void setup() {
        AmazonDynamoDB dynamodb = DynamoDBEmbedded.create().amazonDynamoDB();
        new RestHandler(dynamodb);
    }

    @Test
    public void testWelcomeScreen() {
        given().when().get("http://localhost:4567/")
                .then()
                .statusCode(200)
                .body(equalTo("Welcome to the api of mysmoker"));
    }

    @Test
    public void testSetTemp() {
        given().when().get("http://localhost:4567/").then().statusCode(200);

        given().when()
                .get("http://localhost:4567/settemp/123")
                .then()
                .statusCode(200);

        given().when()
                .get("http://localhost:4567/gettemp")
                .then()
                .statusCode(200)
                .body(equalTo("123"));
    }


}