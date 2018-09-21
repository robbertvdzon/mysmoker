package my.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.ArrayList;
import java.util.List;

@DynamoDBTable(tableName = "smokersessions")
public class SmokerSession {
    private String sessionDateTime;
    private long lastUpdate;
    List<Temperature> temperatures = new ArrayList<>();

    public SmokerSession() {
    }

    public SmokerSession(String sessionDateTime, long lastUpdate) {
        this.sessionDateTime = sessionDateTime;
        this.lastUpdate = lastUpdate;
    }

    @DynamoDBHashKey
    public String getSessionDateTime() {
        return sessionDateTime;
    }

    public void setSessionDateTime(String sessionDateTime) {
        this.sessionDateTime = sessionDateTime;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<Temperature> getTemperatures() {
        return temperatures;
    }

    public void setTemperatures(List<Temperature> temperatures) {
        this.temperatures = temperatures;
    }
}
