package my.service.readstack.model;

import com.amazonaws.services.dynamodbv2.document.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.service.writestack.model.Sample;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JsonSample {
    private long s; // link to sessionStartTime
    private long t = 0;
    private double bt = 0; // smoker temp
    private double mt = 0; // meat temp
    private double f = 0; // fan
    private double bs = 0; // smokertempset

    public JsonSample(Sample sample) {
        s = sample.getSessionStartTime();
        t = sample.getTime();
        bt = sample.getBbqTemp();
        mt = sample.getMeatTemp();
        f = sample.getFan();
        bs = sample.getBbqSet();
    }

    public int compareTo(JsonSample otherJsonSample) {
        return this.getT() < otherJsonSample.getT() ? -1 : 1;
    }

    public static JsonSample fromItem(Item item) {
        double bbqTemp = item.getDouble("bbqTemp");
        double meatTemp = item.getDouble("meatTemp");
        double fan = item.getDouble("fan");
        double bbqSet = item.getDouble("bbqSet");
        long time = item.getLong("time");
        JsonSample sample = new JsonSample();
        sample.setBt(bbqTemp);
        sample.setBs(bbqSet);
        sample.setMt(meatTemp);
        sample.setF(fan);
        sample.setT(time);
        return sample;
    }

}
