package my.service.writestack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "smokersessions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmokerSession {
    @DynamoDBHashKey
    private long id = 0;
    private String sessionDateTime;
    private long sessionStartTime = 0;
    private long lastSampleTime = 0;
    private long lastMinuteSampleTime = 0;
    private long samplesCount = 0;
    private double lastBbqTemp = 0;
    private double lastMeatTemp = 0;
    private double lastFan = 0;
    private double lastBbqSet = 0;


    public SmokerSession(String sessionDateTime, long sessionStartTime, long id) {
        this.sessionDateTime = sessionDateTime;
        this.sessionStartTime = sessionStartTime;
        this.id= id;
    }

}
