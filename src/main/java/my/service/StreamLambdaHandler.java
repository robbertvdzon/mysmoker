package my.service;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spark.SparkLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import my.service.commandstack.WriteService;
import my.service.querystack.ReadService;

import static spark.Spark.before;
import static spark.Spark.get;


public class StreamLambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =
            SparkLambdaContainerHandler.getAwsProxyHandler();
    private boolean initialized = false;
    private WriteService writeService = new WriteService();
    private ReadService readService = new ReadService();

    public StreamLambdaHandler() throws ContainerInitializationException {
    }

    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
        if (!initialized) {
            defineRoutes();
            initialized = true;
        }
        return handler.proxy(awsProxyRequest, context);
    }

    private void defineRoutes() {
        Gson gson = new Gson();
        before((request, response) -> response.type("application/json"));
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        before((request, response) -> response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT"));
        get("/", (req, res) -> "Welcome to the api of mysmoker");
        get("/listsessions", (req, res) -> readService.listsessions(), gson::toJson); // return 404 when not found
        get("/lastsession", (req, res) -> readService.lastsession(), gson::toJson);
        get("/newsession", (req, res) -> writeService.newsession(), gson::toJson);
        get("/session/:sessionId", (req, res) -> readService.listSession(Long.parseLong(req.params(":sessionId"))), gson::toJson);
        get("/add/:bbqtemp/:meattemp/:bbqtempset/:fan", (req, res) ->
        {
            writeService.add(
                    Double.parseDouble((req.params(":bbqtemp"))),
                    Double.parseDouble((req.params(":meattemp"))),
                    Double.parseDouble((req.params(":bbqtempset"))),
                    Double.parseDouble((req.params(":fan")))
            );
            return "ok";
        }, gson::toJson); // return 201 : change to POST
        get("/removesession/:sessionId", (req, res) -> {
            writeService.removeSession(Long.parseLong(req.params(":sessionId")));
            return "ok";
        }, gson::toJson);
        get("/settemp/:temp", (req, res) -> {
            writeService.setTemp(Double.parseDouble(req.params(":temp")));
            return "ok";
        }, gson::toJson);
        get("/gettemp", (req, res) -> readService.getTemp(), gson::toJson);

    }
}