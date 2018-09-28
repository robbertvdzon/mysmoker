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
        return smokerReadRepository.findLastSession(this::useLowSampleRate);
    }

    public JsonSmokerSession listSession(String session) {
        System.out.println("MyResource: session/" + session);
        return smokerReadRepository.findSession(session, this::useLowSampleRate);
    }

    public long getTemp() {
        System.out.println("MyResource: getTemp");
        JsonSmokerState smokerState = smokerReadRepository.loadState();
        return Math.round(smokerState.getBbqTempSet());
    }

    private boolean useLowSampleRate(Long sampleCount) {
        return sampleCount > 360 * 6;
    }

}
