package my.service.readstack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.service.writestack.model.SmokerSession;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class JsonSmokerSession {
    private String sessionDateTime;
    private long lastSampleTime = 0;
    private double lastBbqTemp = 0;
    private double lastMeatTemp = 0;
    private double lastFan = 0;
    private double lastBbqSet = 0;
    private List<JsonSample> samples = new ArrayList<>();

    // ignore for json
    private long sessionStartTime = 0;
    private long samplesCount = 0;

    public int compareTo(JsonSmokerSession otherSession) {
        return getSessionDateTime().compareTo(otherSession.getSessionDateTime());
    }

}
