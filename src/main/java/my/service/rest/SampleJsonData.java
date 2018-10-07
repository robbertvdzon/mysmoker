package my.service.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SampleJsonData {
    double bbqtemp;
    double meattemp;
    double bbqtempset;
    double fan;
}
