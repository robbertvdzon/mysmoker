package my.service.readstack;

import my.service.readstack.model.JsonSmokerSession;
import my.service.readstack.model.JsonSmokerState;
import my.service.readstack.storage.SmokerQueryRepository;

import java.util.List;

public class ReadService {

    private SmokerQueryRepository smokerReadRepository = new SmokerQueryRepository();

    public List<JsonSmokerSession> listsessions() {
        return smokerReadRepository.listAllSessions();
    }

    public JsonSmokerSession lastsession() {
        System.out.println("MyResource: listSession");
        return smokerReadRepository.findLastSession(this::useLowSampleRate);
    }

    public JsonSmokerSession listSession(long sessionId) {
        System.out.println("MyResource: session/" + sessionId);
        return smokerReadRepository.findSession(sessionId, this::useLowSampleRate);
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
