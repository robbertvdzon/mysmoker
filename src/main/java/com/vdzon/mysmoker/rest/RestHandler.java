package com.vdzon.mysmoker.rest;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vdzon.mysmoker.commandstack.WriteService;
import com.vdzon.mysmoker.commandstack.model.SmokerSession;
import com.vdzon.mysmoker.commandstack.storage.SmokerCommandRepository;
import com.vdzon.mysmoker.querystack.ReadService;
import com.vdzon.mysmoker.querystack.storage.SmokerQueryRepository;
import spark.Request;
import spark.Response;

import static spark.Spark.*;


public class RestHandler {
    private static final Gson gson = new GsonBuilder().create();

    public RestHandler(AmazonDynamoDB amazonDynamoDB) {
        defineRoutes(amazonDynamoDB);
    }

    private void defineRoutes(AmazonDynamoDB amazonDynamoDB) {

        SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository(amazonDynamoDB);
        SmokerQueryRepository smokerReadRepository = new SmokerQueryRepository(amazonDynamoDB);
        WriteService writeService = new WriteService(smokerCommandRepository);
        ReadService readService = new ReadService(smokerReadRepository);


        before((request, response) -> response.type("application/json"));
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        before((request, response) -> response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT"));

        get("/", (req, res) -> "Welcome to the api of mysmoker");
        get("/listsessions", (req, res) -> readService.listsessions(), gson::toJson); // return 404 when not found
        get("/lastsession", (req, res) -> readService.lastsession(), gson::toJson);
        get("/session/:sessionId", (req, res) -> readService.listSession(Long.parseLong(req.params(":sessionId"))), gson::toJson);
        get("/gettemp", (req, res) -> readService.getTemp(), gson::toJson);

        post("/newsession", (req, res) -> addSession(writeService, res), gson::toJson);
        post("/add", (req, res) -> addSample(writeService, req, res), gson::toJson); // return 201 : change to POST
        post("/settemp/:temp", (req, res) -> setTemp(writeService, req), gson::toJson);

        post("/removesession/:sessionId", (req, res) -> removeSession(writeService, req), gson::toJson);
    }

    private SmokerSession addSession(WriteService writeService, Response res) {
        SmokerSession result = writeService.newsession();
        res.status(201);
        return result;
    }

    private Object setTemp(WriteService writeService, Request req) {
        writeService.setTemp(Double.parseDouble(req.params(":temp")));
        return "ok";
    }

    private Object removeSession(WriteService writeService, Request req) {
        writeService.removeSession(Long.parseLong(req.params(":sessionId")));
        return "ok";
    }

    private Object addSample(WriteService writeService, Request req, Response res) {
        SampleJsonData sample = gson.fromJson(req.body(), SampleJsonData.class);
        writeService.add(
                sample.bbqtemp,
                sample.meattemp,
                sample.bbqtempset,
                sample.fan
        );
        res.status(201);
        return "ok";
    }
}