package my.service;


import my.service.readstack.ReadService;
import my.service.readstack.model.JsonSmokerSession;
import my.service.writestack.WriteService;
import my.service.writestack.model.SmokerSession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
public class MyResource {
    private WriteService writeService = new WriteService();
    private ReadService readService = new ReadService();

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
        List<String> sessionStrings = readService.listsessions();
        return buildResponse(200, sessionStrings);
    }

    @GET
    @Path("/lastsession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response lastsession() {
        JsonSmokerSession smokerSession = readService.lastsession();
        return buildResponse(200, smokerSession);
    }

    @GET
    @Path("/newsession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response newsession() {
        SmokerSession smokerSession = writeService.newsession();
        return buildResponse(200, smokerSession);
    }

    @GET
    @Path("/session/{session}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response listSession(@PathParam("session") String session) {
        JsonSmokerSession jsonSmokerSession = readService.listSession(session);
        int statusCode = jsonSmokerSession == null ? 404 : 200;
        return buildResponse(statusCode, jsonSmokerSession);
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
        writeService.add(bbqtemp, meattemp, bbqtempset, fan);
        return buildResponse(201, null);
    }

    @GET
    @Path("/removesession/{session}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response removeSession(@PathParam("session") String session) {
        writeService.removeSession(session);
        return buildResponse(200, null);
    }

    @GET
    @Path("/settemp/{temp}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setTemp(@PathParam("temp") double temp) {
        writeService.setTemp(temp);
        return buildResponse(200, null);
    }

    @GET
    @Path("/gettemp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response getTemp() {
        double temp = readService.getTemp();
        return buildResponse(200, temp);
    }

    private Response buildResponse(int statusCode, Object entity) {
        return Response
                .status(statusCode)
                .entity(entity)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("GET,OPTIONS").build();
    }

}

