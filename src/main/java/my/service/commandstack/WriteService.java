package my.service.commandstack;

import my.service.commandstack.model.Sample;
import my.service.commandstack.model.SmokerSession;
import my.service.commandstack.model.SmokerState;
import my.service.commandstack.storage.SmokerCommandRepository;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteService {

    SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository();

    public void removeSession(long sessionId) {
        System.out.println("remove session "+sessionId);
        smokerCommandRepository.createTablewhenNeeded();

        smokerCommandRepository.removeSession(sessionId);
    }

    public void setTemp(double temp) {
        System.out.println("set temp " + temp);
        smokerCommandRepository.createTablewhenNeeded();

        SmokerState smokerState = smokerCommandRepository.loadState();
        smokerState.setBbqTempSet(temp);
        smokerCommandRepository.storeState(smokerState);
    }

    public SmokerSession newsession() {
        System.out.println("MyResource: newSession");
        smokerCommandRepository.createTablewhenNeeded();

        long currentTimeMillis = System.currentTimeMillis();
        SmokerSession smokerSession = createNewSmokerSession(currentTimeMillis);
        smokerSession.setLastSampleTime(currentTimeMillis);
        smokerCommandRepository.storeSession(smokerSession);

        SmokerState smokerState = smokerCommandRepository.loadState();
        smokerState.setCurrentSessionId(smokerSession.getId());
        smokerCommandRepository.storeState(smokerState);
        return smokerSession;
    }

    public void add(
            double bbqtemp,
            double meattemp,
            double bbqtempset,
            double fan
    ) {
        System.out.println("MyResource: add");
        smokerCommandRepository.createTablewhenNeeded();

        long currentTimeMillis = System.currentTimeMillis();
        SmokerSession smokerSession = findOrCreateSession(currentTimeMillis);
        long timeDiffSinceLastMinute = currentTimeMillis - smokerSession.getLastMinuteSampleTime();
        boolean newMinute = timeDiffSinceLastMinute > 60 * 1000;

        Sample sample = new Sample();
        sample.setBbqTemp(bbqtemp);
        sample.setBbqSet(bbqtempset);
        sample.setFan(fan);
        sample.setMeatTemp(meattemp);
        sample.setNewMinute(newMinute);
        sample.setSessionId(smokerSession.getId());
        sample.setTime(currentTimeMillis);

        if (newMinute) {
            smokerSession.setLastMinuteSampleTime(currentTimeMillis);
        }
        smokerSession.setLastBbqSet(sample.getBbqSet());
        smokerSession.setLastBbqTemp(sample.getBbqTemp());
        smokerSession.setLastMeatTemp(sample.getMeatTemp());
        smokerSession.setLastFan(sample.getFan());
        smokerSession.setLastSampleTime(currentTimeMillis);
        smokerSession.setSamplesCount(smokerSession.getSamplesCount() + 1);

        smokerCommandRepository.storeSession(smokerSession);
        smokerCommandRepository.storeSample(sample);
    }

    private SmokerSession findOrCreateSession(long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerState smokerState = smokerCommandRepository.loadState();
        SmokerSession lastSession = smokerCommandRepository.loadSession(smokerState.getCurrentSessionId());
        if (lastSession != null && lastSession.getLastSampleTime() > timeout) {
            return lastSession;
        }
        SmokerSession newSmokerSession = createNewSmokerSession(currentTimestamp);
        smokerState.setCurrentSessionId(newSmokerSession.getId());
        smokerCommandRepository.storeState(smokerState);
        return newSmokerSession;
    }

    private SmokerSession createNewSmokerSession(long currentTimestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        return new SmokerSession(currentDateString, currentTimestamp, currentTimestamp);
    }


}
