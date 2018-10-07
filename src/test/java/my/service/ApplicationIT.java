package my.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import my.service.querystack.model.JsonSample;
import my.service.querystack.model.JsonSmokerSession;
import my.service.rest.RestHandler;
import my.service.rest.SampleJsonData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationIT {
    private static final Gson gson = new GsonBuilder().create();

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
        given().when()
                .post("http://localhost:4567/settemp/123")
                .then()
                .statusCode(200);

        given().when()
                .get("http://localhost:4567/gettemp")
                .then()
                .statusCode(200)
                .body(equalTo("123"));
    }

    @Test
    public void testAddSample() {
        // Add sample
        SampleJsonData data = createSample(1.1, 1.2, 1.3, 60);
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(data)
                .when()
                .post("http://localhost:4567/add")
                .then()
                .statusCode(201);

        // Read last session
        Response response = given().when()
                .get("http://localhost:4567/lastsession");
        assertThat(response.getStatusCode(),is(200));
        JsonSmokerSession smokerSession = response.getBody().as(JsonSmokerSession.class);
        assertThat(smokerSession.getSamplesCount(),is(1l));
        JsonSample jsonSample = smokerSession.getSamples().get(0);
        assertThat(jsonSample.getBt(),is(1.1));
        assertThat(jsonSample.getMt(),is(1.2));
        assertThat(jsonSample.getBs(),is(1.3));
        assertThat(jsonSample.getF(),is(60.0));
    }

    @Test
    public void testAddSession() {
        // Add session (should create a new session)
        SampleJsonData data = createSample(1.1, 1.2, 1.3, 60);
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(data)
                .when()
                .post("http://localhost:4567/add")
                .then()
                .statusCode(201);

        // Now there should be one session
        Response response = given().when().get("http://localhost:4567/listsessions");
        assertThat(response.getStatusCode(),is(200));
        JsonSmokerSession[] jsonSmokerSessions = gson.fromJson(response.body().asString(), JsonSmokerSession[].class);
        assertThat(jsonSmokerSessions.length,is(1));

        // create a new session
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(data)
                .when()
                .post("http://localhost:4567/newsession")
                .then()
                .statusCode(201);

        // Now there should be two sessions
        response = given().when().get("http://localhost:4567/listsessions");
        assertThat(response.getStatusCode(),is(200));
        jsonSmokerSessions = gson.fromJson(response.body().asString(), JsonSmokerSession[].class);
        assertThat(jsonSmokerSessions.length,is(2));

    }

    private SampleJsonData createSample(double bbq, double meat, double set, double fan) {
        return SampleJsonData
                    .builder()
                    .bbqtemp(bbq)
                    .meattemp(meat)
                    .bbqtempset(set)
                    .fan(fan)
                    .build();
    }


}