package my.service.writestack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static my.service.common.Const.SMOKERSAMPLES_TABLENAME;
@DynamoDBTable(tableName = SMOKERSAMPLES_TABLENAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sample {
    @DynamoDBHashKey
    private long time = 0;
    private long sessionId;
    private double bbqTemp = 0;
    private double meatTemp = 0;
    private double fan = 0;
    private double bbqSet = 0;
    private boolean newMinute = false;
}
