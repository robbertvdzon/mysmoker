package my.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBDocument
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Temperature {
    private long t = 0;
    private double bt = 0; // smoker temp
    private double mt = 0; // meat temp
    private double f = 0; // fan
    private double bs = 0; // smokertempset



}
