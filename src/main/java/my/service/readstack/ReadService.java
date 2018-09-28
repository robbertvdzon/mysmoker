package my.service.readstack;

import my.service.readstack.model.JsonSample;
import my.service.readstack.model.JsonSmokerSession;
import my.service.readstack.model.JsonSmokerState;
import my.service.writestack.model.SmokerSession;
import my.service.writestack.model.SmokerState;

import java.util.List;

public class ReadService {

    private SmokerQueryRepository smokerReadRepository = new SmokerQueryRepository();

    public List<String> listsessions() {
        return smokerReadRepository.listAllSessionIds();
    }

    public JsonSmokerSession lastsession() {
        System.out.println("MyResource: listSession");
        SmokerSession lastSession = smokerReadRepository.findLastSession();
        boolean lowSamples = lastSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = smokerReadRepository.findSamples(lastSession.getSessionStartTime(), lowSamples);
        return new JsonSmokerSession(lastSession, samples);
    }

    public JsonSmokerSession listSession(String session) {
        System.out.println("MyResource: session/" + session);
        SmokerSession smokerSession = smokerReadRepository.findSession(session);
        if (smokerSession==null) {
            return null;
        }
        boolean lowSamples = smokerSession.getSamplesCount() > 360 * 6; // after 6 hour, use slower sample rate
        List<JsonSample> samples = smokerReadRepository.findSamples(smokerSession.getSessionStartTime(), lowSamples);
        return new JsonSmokerSession(smokerSession, samples);
    }

    public long getTemp() {
        JsonSmokerState smokerState = smokerReadRepository.loadState();
        return Math.round(smokerState.getBbqTempSet());
    }


}
