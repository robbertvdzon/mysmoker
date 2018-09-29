package my.service;

import my.service.readstack.ReadService;
import my.service.writestack.SmokerCommandRepository;
import my.service.writestack.WriteService;

import javax.ws.rs.core.Response;
import java.util.List;

public class TestApp {
    private static WriteService writeService = new WriteService();
    private static ReadService readService = new ReadService();
    private static SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository();

    public static void main(String[] args){
//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.setTemp(3.1);
//        System.out.println(readService.listsessions());
//        System.out.println(smokerCommandRepository.loadState());
        writeService.removeSession("2018-09-23_10:42:30");


    }
}
