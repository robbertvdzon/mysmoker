package my.service.writestack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "smokersamples")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sample {
    @DynamoDBHashKey
    private long time = 0;
    private long sessionStartTime;
    private double bbqTemp = 0;
    private double meatTemp = 0;
    private double fan = 0;
    private double bbqSet = 0;
    private boolean newMinute = false;
}
