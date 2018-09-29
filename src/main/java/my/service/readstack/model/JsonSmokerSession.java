package my.service.readstack.model;

import com.amazonaws.services.dynamodbv2.document.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class JsonSmokerSession {
    private long id = 0;
    private String sessionDateTime;
    private long sessionStartTime = 0;
    private long lastSampleTime = 0;
    private long samplesCount = 0;
    private double lastBbqTemp = 0;
    private double lastMeatTemp = 0;
    private double lastFan = 0;
    private double lastBbqSet = 0;
    private List<JsonSample> samples = new ArrayList<>();

    public static JsonSmokerSession fromItemWithoutSamples(Item item) {
        String sessionDateTime = item.getString("sessionDateTime");
        long lastSampleTime = item.getLong("lastSampleTime");
        long sessionStartTime = item.getLong("sessionStartTime");
        long id = item.getLong("id");
        long samplesCount = item.getLong("samplesCount");
        double lastBbqTemp = item.getDouble("lastBbqTemp");
        double lastMeatTemp = item.getDouble("lastMeatTemp");
        double lastFan = item.getDouble("lastFan");
        double lastBbqSet = item.getDouble("lastBbqSet");

        return JsonSmokerSession.builder()
                .sessionDateTime(sessionDateTime)
                .sessionStartTime(sessionStartTime)
                .id(id)
                .samplesCount(samplesCount)
                .lastSampleTime(lastSampleTime)
                .lastBbqTemp(lastBbqTemp)
                .lastBbqSet(lastBbqSet)
                .lastMeatTemp(lastMeatTemp)
                .lastFan(lastFan)
                .build();
    }

    public static String getSessionDateTimeFromItem(Item item) {
        return item.getString("sessionDateTime");
    }

    public int compareTo(JsonSmokerSession otherSession) {
        return getSessionDateTime().compareTo(otherSession.getSessionDateTime());
    }

}
