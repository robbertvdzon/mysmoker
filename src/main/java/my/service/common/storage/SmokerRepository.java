package my.service.common.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class SmokerRepository {
    private AmazonDynamoDB amazonDynamoDB;

    public SmokerRepository(AmazonDynamoDB amazonDynamoDB) {

        this.amazonDynamoDB = amazonDynamoDB;
    }

    protected AmazonDynamoDB getDynamoDb() {
        return amazonDynamoDB;
    }

    protected <T> List<T> getScanResult(ScanSpec scanSpec, Table table, Function<Item, T> toObject, Comparator<? super T> c) {
        ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        List<T> result = new ArrayList<>();
        for (Item item : items) {
            result.add(toObject.apply(item));
        }
        result.sort(c);
        return result;
    }

    protected void createTable(AmazonDynamoDB dynamoDB, String tablename, String keyName, String keyType) {
        ListTablesResult listTablesResult = dynamoDB.listTables();
        boolean tableFound = listTablesResult.getTableNames().stream().anyMatch(name -> name.equals(tablename));
        if (!tableFound) {
            ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
            keySchema.add(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH)); // Partition

            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
            attributeDefinitions
                    .add(new AttributeDefinition().withAttributeName(keyName).withAttributeType(keyType));

            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(tablename)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(5l)
                            .withWriteCapacityUnits(5l)
                    );
            dynamoDB.createTable(createTableRequest);
        }
    }

}
