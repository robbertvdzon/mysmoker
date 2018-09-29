package my.service;

import my.service.querystack.ReadService;
import my.service.commandstack.storage.SmokerCommandRepository;
import my.service.commandstack.WriteService;

public class TestApp {
    private static WriteService writeService = new WriteService();
    private static ReadService readService = new ReadService();
    private static SmokerCommandRepository smokerCommandRepository = new SmokerCommandRepository();

    public static void main(String[] args){
//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.add(1.1,1.2,1.3,1.4);
//        writeService.setTemp(3.1);
//        System.out.println(readService.lastsession());
//        System.out.println(readService.listsessions());
//        System.out.println(smokerCommandRepository.loadState());
        writeService.removeSession(1538224970903l);


    }
}
