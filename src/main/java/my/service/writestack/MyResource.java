package my.service.writestack;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import my.service.readstack.model.JsonSample;
import my.service.readstack.model.JsonSmokerSession;
import my.service.writestack.model.Sample;
import my.service.writestack.model.SmokerSession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.*;
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
        System.out.println("MyResource: listSessions");
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = listAllSessions(ddb);
        List<String> sessionStrings = sessions.stream().map(session -> session.getSessionDateTime()).collect(Collectors.toList());
        return Response
                .status(200)
                .entity(sessionStrings)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS").build();
    }

    @GET
    @Path("/lastsession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response lastsession() {
        System.out.println("MyResource: listSession");
        final AmazonDynamoDB ddb = getDynamoDb();
        SmokerSession lastSession = findLastSession(ddb);
        boolean lowSamples = lastSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = findSamples(ddb, lastSession.getSessionStartTime(), lowSamples);
        JsonSmokerSession smokerSession = new JsonSmokerSession(lastSession, samples);
        return Response
                .status(200)
                .entity(smokerSession)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS")
                .build();
    }

    @GET
    @Path("/newsession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response newsession() {
        System.out.println("MyResource: newSession");
        final AmazonDynamoDB ddb = getDynamoDb();
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimeMillis));
        SmokerSession smokerSession = new SmokerSession(currentDateString, currentTimeMillis);
        smokerSession.setLastSampleTime(currentTimeMillis);
        storeSession(ddb, smokerSession);
        return Response
                .status(200)
                .entity(smokerSession)
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
        System.out.println("MyResource: session/" + session);
        final AmazonDynamoDB ddb = getDynamoDb();
        List<SmokerSession> sessions = findSession(ddb, session);
        if (sessions.isEmpty()) {
            return Response.status(404).build();
        }
        SmokerSession smokerSession = sessions.get(0);
        boolean lowSamples = smokerSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = findSamples(ddb, smokerSession.getSessionStartTime(), lowSamples);
        JsonSmokerSession jsonSmokerSession = new JsonSmokerSession(smokerSession, samples);

        return Response
                .status(200)
                .entity(jsonSmokerSession)
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
        System.out.println("MyResource: add");
        try {
            final AmazonDynamoDB ddb = getDynamoDb();
            createTablewhenNeeded(ddb);

            long currentTimeMillis = System.currentTimeMillis();
            SmokerSession smokerSession = findOrCreateSession(ddb, currentTimeMillis);
            long timeDiffSinceLastMinute = currentTimeMillis - smokerSession.getLastMinuteSampleTime();
            boolean newMinute = timeDiffSinceLastMinute > 60 * 1000;

            Sample sample = new Sample();
            sample.setBbqTemp(bbqtemp);
            sample.setBbqSet(bbqtempset);
            sample.setFan(fan);
            sample.setMeatTemp(meattemp);
            sample.setNewMinute(newMinute);
            sample.setSessionStartTime(smokerSession.getSessionStartTime());
            sample.setTime(currentTimeMillis);

            if (newMinute) {
                smokerSession.setLastMinuteSampleTime(currentTimeMillis);
            }
            smokerSession.setLastBbqSet(sample.getBbqSet());
            smokerSession.setLastBbqTemp(sample.getBbqTemp());
            smokerSession.setLastMeatTemp(sample.getMeatTemp());
            smokerSession.setLastFan(sample.getFan());
            smokerSession.setLastSampleTime(currentTimeMillis);
            smokerSession.setSamplesCount(smokerSession.getSamplesCount() + 1);

            storeSession(ddb, smokerSession);
            storeSample(ddb, sample);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            System.err.format("Error: The table can't be found.\n");
            System.err.println("Be sure that it exists");
            System.exit(1);
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
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

    private void storeSample(AmazonDynamoDB ddb, Sample sample) {
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        mapper.save(sample);
    }

    private List<JsonSample> findSamples(AmazonDynamoDB ddb, long sessionStartTime, boolean lowSamples) {
        DynamoDB dynamoDB = new DynamoDB(ddb);
        Table table = dynamoDB.getTable("smokersamples");
        ScanSpec scanSpec;
        if (lowSamples) {
            scanSpec = new ScanSpec()
                    .withFilterExpression("sessionStartTime = :sessionStartTime AND newMinute = :newMinute")
                    .withValueMap(new ValueMap()
                            .withNumber(":sessionStartTime", sessionStartTime)
                            .withNumber(":newMinute", 1)
                    );
        } else {
            scanSpec = new ScanSpec()
                    .withFilterExpression("sessionStartTime = :sessionStartTime")
                    .withValueMap(new ValueMap()
                            .withNumber(":sessionStartTime", sessionStartTime)
                    );
        }
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        try {
            List<JsonSample> result = new ArrayList<>();
            Iterator<Item> iter = items.iterator();
            while (iter.hasNext()) {
                Item item = iter.next();
                double bbqTemp = item.getDouble("bbqTemp");
                double meatTemp = item.getDouble("meatTemp");
                double fan = item.getDouble("fan");
                double bbqSet = item.getDouble("bbqSet");
                long time = item.getLong("time");
                JsonSample sample = new JsonSample();
                sample.setBt(bbqTemp);
                sample.setBs(bbqSet);
                sample.setMt(meatTemp);
                sample.setF(fan);
                sample.setT(time);
                result.add(sample);
            }
            // the list needs to be sorted!
            // this is because the scan does not have any ordering. TODO: use a query: these do have an ordering
            result.sort((m1, m2) -> m1.getT()<m2.getT() ? -1 : 1);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }


    private SmokerSession findOrCreateSession(AmazonDynamoDB ddb, long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerSession lastSession = findLastSession(ddb);
        if (lastSession != null && lastSession.getLastSampleTime() > timeout) {
            return lastSession;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        return new SmokerSession(currentDateString, currentTimestamp);
    }

    private SmokerSession findLastSession(AmazonDynamoDB ddb) {
        List<SmokerSession> scanResult = listAllSessions(ddb);
        OptionalLong lastSession = scanResult.stream().mapToLong(sessions -> sessions.getLastSampleTime()).max();
        long sessionDatetime = lastSession.orElseGet(()->0);
        Optional<SmokerSession> last = scanResult.stream().filter(session -> session.getLastSampleTime() == sessionDatetime).findFirst();
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
        createTable(dynamoDB, "smokersessions", "sessionDateTime", "S");
        createTable(dynamoDB, "smokersamples", "time", "N");
    }

    private void createTable(AmazonDynamoDB dynamoDB, String tablename, String keyName, String keyType) {
        ListTablesResult listTablesResult = dynamoDB.listTables();
        boolean tableFound = listTablesResult.getTableNames().stream().anyMatch(name -> name.equals(tablename));
        if (!tableFound) {
            ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
            keySchema.add(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH)); // Partition

            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
            attributeDefinitions
                    .add(new AttributeDefinition().withAttributeName(keyName).withAttributeType(keyType));

            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(tablename)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(5l)
                            .withWriteCapacityUnits(5l)
                    );
            dynamoDB.createTable(createTableRequest);
        }
    }

}

