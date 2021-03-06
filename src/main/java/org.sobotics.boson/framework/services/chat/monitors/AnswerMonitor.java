package org.sobotics.boson.framework.services.chat.monitors;

import org.sobotics.boson.framework.model.chat.ChatRoom;
import org.sobotics.boson.framework.model.stackexchange.Answer;
import org.sobotics.boson.framework.services.chat.filters.Filter;
import org.sobotics.boson.framework.services.chat.printers.PrinterService;
import org.sobotics.boson.framework.services.data.ApiService;
import org.sobotics.boson.framework.services.data.StackExchangeApiService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class AnswerMonitor extends Monitor<Answer, Answer>{

    private Instant previousTime;

    public AnswerMonitor(ChatRoom room, int frequency, String site, String apiKey, Filter<Answer>[] filters, PrinterService<Answer> printer) {
        super(room, frequency, site, apiKey, filters, printer);
        previousTime = Instant.now().minusSeconds(60);
    }

    @Override
    protected void monitor(ChatRoom room, String site, String apiKey, Filter<Answer>[] filters, PrinterService<Answer> printer) throws IOException {
        ApiService apiService = new StackExchangeApiService(apiKey);
        List<Answer> answers = apiService.getAnswers(site, 1, 100, previousTime);
        for (Answer answer: answers){
            for (Filter<Answer> filter: filters){
                if(filter.filter(answer)){
                    room.getRoom().send(printer.print(answer));
                }
            }
        }
        previousTime = Instant.now();
    }
}
