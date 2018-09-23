package my.service.readstack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "smokerstate")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmokerState {
    @DynamoDBHashKey
    private int id = 1;
    private long currentSessionStartTime = 0;
    private double bbqTempSet = 0;

}
