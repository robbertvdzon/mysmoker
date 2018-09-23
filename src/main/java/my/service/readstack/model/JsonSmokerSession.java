package my.service.readstack.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.service.writestack.model.SmokerSession;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JsonSmokerSession {
    private String sessionDateTime;
    private long lastSampleTime = 0;
    private double lastBbqTemp = 0;
    private double lastMeatTemp = 0;
    private double lastFan = 0;
    private double lastBbqSet = 0;
    private List<JsonSample> samples = new ArrayList<>();

    public JsonSmokerSession(SmokerSession session, List<JsonSample> samples) {
        sessionDateTime = session.getSessionDateTime();
        lastBbqSet = session.getLastBbqSet();
        lastBbqTemp = session.getLastBbqTemp();
        lastMeatTemp = session.getLastMeatTemp();
        lastFan = session.getLastFan();
        lastSampleTime = session.getLastSampleTime();
        this.samples = samples;
    }
}
