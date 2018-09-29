package my.service.writestack;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import my.service.readstack.ReadService;
import my.service.readstack.model.JsonSmokerSession;
import my.service.writestack.model.Sample;
import my.service.writestack.model.SmokerSession;
import my.service.writestack.model.SmokerState;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteService {

    SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository();
    ReadService readService = new ReadService();

    public void removeSession(String sessionId) {
        smokerCommandRepository.createTablewhenNeeded();
        JsonSmokerSession jsonSmokerSession = readService.listSession(sessionId);
        if (jsonSmokerSession == null) {
            return;
        }
        smokerCommandRepository.removeSessionFromTable(sessionId);
        smokerCommandRepository.removeSamplesFromTable(jsonSmokerSession.getSessionStartTime());
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimeMillis));
        SmokerSession smokerSession = new SmokerSession(currentDateString, currentTimeMillis);
        smokerSession.setLastSampleTime(currentTimeMillis);
        smokerCommandRepository.storeSession(smokerSession);

        SmokerState smokerState = smokerCommandRepository.loadState();
        smokerState.setCurrentSessionStartTime(smokerSession.getSessionStartTime());
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
        try {
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
            sample.setSessionStartTime(smokerSession.getSessionStartTime());
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
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            System.err.format("Error: The table can't be found.\n");
            System.err.println("Be sure that it exists");
            System.exit(1);
        } catch (AmazonServiceException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
        }
    }

    public SmokerSession findOrCreateSession(long currentTimestamp) {
        long timeout = currentTimestamp - 1000 * 60 * 60;// minus one hour
        SmokerState smokerState = smokerCommandRepository.loadState();
        SmokerSession lastSession = smokerCommandRepository.loadSession(smokerState.getCurrentSessionStartTime());
        if (lastSession != null && lastSession.getLastSampleTime() > timeout) {
            return lastSession;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateString = simpleDateFormat.format(new Date(currentTimestamp));
        smokerState.setCurrentSessionStartTime(currentTimestamp);
        smokerCommandRepository.storeState(smokerState);
        return new SmokerSession(currentDateString, currentTimestamp);
    }


}
