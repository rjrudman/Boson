package org.sobotics.boson.bot;


import org.sobotics.boson.framework.model.chat.ChatRoom;
import org.sobotics.boson.framework.model.stackexchange.Answer;
import org.sobotics.boson.framework.model.stackexchange.Question;
import org.sobotics.boson.framework.model.stackexchange.Tag;
import org.sobotics.boson.framework.services.chat.ChatRoomService;
import org.sobotics.boson.framework.services.chat.commands.Alive;
import org.sobotics.boson.framework.services.chat.commands.Command;
import org.sobotics.boson.framework.services.chat.filters.EmptyFilter;
import org.sobotics.boson.framework.services.chat.filters.Filter;
import org.sobotics.boson.framework.services.chat.listeners.MessageReplyEventListener;
import org.sobotics.boson.framework.services.chat.listeners.UserMentionedListener;
import org.sobotics.boson.framework.services.chat.monitors.AnswerMonitor;
import org.sobotics.boson.framework.services.chat.monitors.Monitor;
import org.sobotics.boson.framework.services.chat.monitors.QuestionMonitor;
import org.sobotics.boson.framework.services.chat.monitors.TagMonitor;
import org.sobotics.boson.framework.services.chat.printers.GenericContentPrinterService;
import org.sobotics.boson.framework.services.chat.printers.ListOfTagsPrinter;
import org.sobotics.chatexchange.chat.ChatHost;
import org.sobotics.chatexchange.chat.Message;
import org.sobotics.chatexchange.chat.Room;
import org.sobotics.chatexchange.chat.StackExchangeClient;
import org.sobotics.chatexchange.chat.event.EventType;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BosonBot {

    private Room room;
    private StackExchangeClient client;
    private Map<String, Thread> bots;

    public BosonBot(Room room, StackExchangeClient client) {
        this.room = room;
        this.client = client;
        this.bots = new HashMap<>();
    }

    public void start(){
        room.send("Boson Started");
        room.addEventListener(EventType.USER_MENTIONED, event-> {
            Message message = event.getMessage();
            String arguments[] = message.getPlainContent().split(" ");
            if (arguments[1].equals("track")) {
                ChatRoomService service = trackCommand(room, message);
                if (service != null) {

                    Thread newbot = new Thread(() -> {
                        try {
                            service.startService();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    newbot.start();

                    char[] characs = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
                    Random secureRandom = new SecureRandom();
                    String ID = "";
                    for (int i = 0; i < 10; i++)
                        ID += characs[secureRandom.nextInt(characs.length)];

                    bots.put(ID, newbot);
                }
            }
            if (arguments[1].equals("help")){
                room.send("Use the command `track sitename posttype frequency [roomID chatServer]` to start tracking sites");
            }
            if (arguments[1].equals("alive")){
                room.send("Yes, I'm alive");
            }
            if (arguments[1].equals("stop")){
                String stopBotId = arguments[2];
                if(bots.containsKey(stopBotId)) {
                    bots.get(stopBotId).interrupt();
                    bots.remove(stopBotId);
                    room.send("Bot stopped");
                }
                else {
                    room.send("Wrong bot ID");
                }
            }
        });
    }

    private ChatRoomService trackCommand(Room room, Message message){

        String arguments[] = message.getPlainContent().split(" ");
        if(arguments.length==5 || arguments.length==7){
            String site = arguments[2];
            String posttype = arguments[3];
            int frequency = Integer.parseInt(arguments[4]);
            boolean sameRoom = true;
            int otherRoomId = room.getRoomId();
            ChatHost otherRoomHost = room.getHost();
            ChatRoom chatRoom = new ChatRoom(room.getRoomId(), room.getHost(), room);
            if(arguments.length==7){
                sameRoom= false;
                otherRoomId = Integer.parseInt(arguments[5]);
                switch (arguments[6]){
                    case "stackoverflow": otherRoomHost=ChatHost.STACK_OVERFLOW; break;
                    case "stackexchange": otherRoomHost=ChatHost.STACK_EXCHANGE; break;
                }

                Room otherRoom = client.joinRoom(otherRoomHost, otherRoomId);
                chatRoom = new ChatRoom(room.getRoomId(), room.getHost(), room);
                Map<Command, Object[]> userMentionCommands = new HashMap<>();
                userMentionCommands.put(new Alive(), new Object[0]);
                chatRoom.setUserMentionedEventConsumer(new UserMentionedListener().getUserMentionedEventConsumer(room, userMentionCommands));

                Map<Command, Object[]> messageReplyCommands = new HashMap<>();
                messageReplyCommands.put(new Alive(), new Object[0]);
                chatRoom.setMessageReplyEventConsumer(new MessageReplyEventListener().getMessageReplyEventListener(room, messageReplyCommands));

            }

            Filter[] filters;
            Monitor[] monitors;

            switch (posttype){
                case "question":
                    filters = new Filter[]{new EmptyFilter<Question>()};
                    monitors = new Monitor[]{new QuestionMonitor(chatRoom, frequency, site, filters,
                            new GenericContentPrinterService<>(site))};
                    break;
                case "answer":
                    filters = new Filter[]{new EmptyFilter<Answer>()};
                    monitors = new Monitor[]{new AnswerMonitor(chatRoom, frequency, site, filters,
                            new GenericContentPrinterService<>(site))};
                    break;
//                case "comment":
//                    filters = new Filter[]{new EmptyFilter<Comment>()};
//                    monitors = new Monitor[]{new CommentMonitor(chatRoom, frequency, site, filters,
//                            new GenericContentPrinterService<>(site))};
//                    break;
                case "tag":
                    filters = new Filter[]{new EmptyFilter<Tag>()};
                    monitors = new Monitor[]{new TagMonitor(chatRoom, frequency, site, filters,
                            new ListOfTagsPrinter(site))};
                    break;
                default:
                    return null;

            }

            ChatRoomService service = new ChatRoomService(chatRoom, monitors);
            return service;

        }
        return null;
    }

}