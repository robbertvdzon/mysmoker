package my.service;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

@Path("/")
public class MyResource {
    private static String TABLE_NAME = "smokersessions";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response getRoot() {
        return Response.status(200).entity("Welcome to the api of mysmoker").build();
    }

    @GET
    @Path("/listsessions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response listsessions() {
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = listAllSessions(ddb);
        List<String> sessionStrings = sessions.stream().map(session -> session.getSessionDateTime()).collect(Collectors.toList());
        return Response
                .status(200)
                .entity(sessionStrings)
                . header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS").build();
    }

    @GET
    @Path("/lastsession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response lastsession() {
        final AmazonDynamoDB ddb = getDynamoDb();
        return Response
                .status(200)
                .entity(findLastSession(ddb))
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS")
                .build();
    }

    @GET
    @Path("/session/{session}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response listSession(@PathParam("session") String session) {
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = findSession(ddb, session);
        if (sessions.isEmpty()) {
            return Response.status(404).build();
        }

        return Response
                .status(200)
                .entity(sessions.get(0))
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS").
                build();
    }

    @GET
    @Path("/add/{bbqtemp}/{meattemp}/{bbqtempset}/{fan}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response add(
            @PathParam("bbqtemp") double bbqtemp,
            @PathParam("meattemp") double meattemp,
            @PathParam("bbqtempset") double bbqtempset,
            @PathParam("fan") double fan
    ) {
        try {
            final AmazonDynamoDB ddb = getDynamoDb();
            createTablewhenNeeded(ddb);

            long currentTimeMillis = System.currentTimeMillis();
            SmokerSession smokerSession = findOrCreateSession(ddb, currentTimeMillis);
            smokerSession.getTemperatures().add(new Temperature(currentTimeMillis, bbqtemp, meattemp, fan, bbqtempset));
            smokerSession.setLastUpdate(currentTimeMillis);
            storeSession(ddb, smokerSession);
        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The table can't be found.\n");
            System.err.println("Be sure that it exists");
            System.exit(1);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return Response.status(200).entity("ok").build();
    }

    private AmazonDynamoDB getDynamoDb() {
        return AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
    }

    private void storeSession(AmazonDynamoDB ddb, SmokerSession smokerSession) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(smokerSession);
    }

    private SmokerSession findOrCreateSession(AmazonDynamoDB ddb, long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerSession lastSession = findLastSession(ddb);
        if (lastSession != null && lastSession.getLastUpdate() > timeout) {
            return lastSession;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        return new SmokerSession(currentDateString, currentTimestamp);
    }

    private SmokerSession findLastSession(AmazonDynamoDB ddb) {
        List<SmokerSession> scanResult = listAllSessions(ddb);
        OptionalLong lastSession = scanResult.stream().mapToLong(sessions -> sessions.getLastUpdate()).max();
        long sessionDatetime = lastSession.getAsLong();
        Optional<SmokerSession> last = scanResult.stream().filter(session -> session.getLastUpdate() == sessionDatetime).findFirst();
        return last.orElse(null);
    }

    private List<SmokerSession> listAllSessions(AmazonDynamoDB ddb) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return mapper.scan(SmokerSession.class, scanExpression);
    }

    private List<SmokerSession> findSession(AmazonDynamoDB ddb, String dateTimeString) {
/*
    FIX THIS CODE
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addExpressionAttributeNamesEntry("sessionDateTime", dateTimeString);
        return mapper.scan(SmokerSession.class, scanExpression);
*/
        return listAllSessions(ddb)
                .stream()
                .filter(s -> s.getSessionDateTime().equals(dateTimeString))
                .collect(Collectors.toList());

    }

    private void createTablewhenNeeded(AmazonDynamoDB dynamoDB) {
/*
    FIX THIS CODE
        ListTablesResult listTablesResult = dynamoDB.listTables();
        listTablesResult.getTableNames().stream().forEach(name -> System.out.println(name));
        boolean tableFound = listTablesResult.getTableNames().stream().anyMatch(name -> name.equals(TABLE_NAME));
        if (!tableFound) {
            CreateTableRequest createTableRequest = new CreateTableRequest();
            createTableRequest.setTableName(TABLE_NAME);
            createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(10L, 10L));
            CreateTableResult table = dynamoDB.createTable(createTableRequest);
        }
*/
    }

}

