package my.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@DynamoDBTable(tableName = "smokersessions")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class SmokerSession {
    @DynamoDBHashKey
    private String sessionDateTime;
    private long lastUpdate;
    List<Temperature> temperatures = new ArrayList<>();

    public SmokerSession(String sessionDateTime, long lastUpdate) {
        this.sessionDateTime = sessionDateTime;
        this.lastUpdate = lastUpdate;
    }

}
