package com.vdzon.mysmoker.querystack;

import com.vdzon.mysmoker.querystack.model.JsonSmokerSession;
import com.vdzon.mysmoker.querystack.model.JsonSmokerState;
import com.vdzon.mysmoker.querystack.storage.SmokerQueryRepository;

import java.util.List;

public class ReadService {

    private SmokerQueryRepository smokerReadRepository;

    public ReadService(SmokerQueryRepository smokerReadRepository) {
        this.smokerReadRepository = smokerReadRepository;
    }

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
