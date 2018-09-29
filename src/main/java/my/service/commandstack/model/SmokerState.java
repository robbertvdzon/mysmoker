package my.service.commandstack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import static my.service.common.Const.SMOKERSTATE_TABLENAME;

@DynamoDBTable(tableName = SMOKERSTATE_TABLENAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmokerState {
    @DynamoDBHashKey
    private int id = 1;
    private long currentSessionId = 0;
    private double bbqTempSet = 0;

}
