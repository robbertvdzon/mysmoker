package my.service.readstack.model;

import com.amazonaws.services.dynamodbv2.document.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import my.service.writestack.model.Sample;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JsonSample {
    private long s;
    private long t = 0;
    private double bt = 0;
    private double mt = 0;
    private double f = 0;
    private double bs = 0;

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
        return JsonSample
                .builder()
                .bt(bbqTemp)
                .bs(bbqSet)
                .mt(meatTemp)
                .f(fan)
                .t(time)
                .build();
    }

}
