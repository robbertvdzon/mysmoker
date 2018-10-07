package com.vdzon.mysmoker.commandstack.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.vdzon.mysmoker.common.Const;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = Const.SMOKERSAMPLES_TABLENAME)
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
