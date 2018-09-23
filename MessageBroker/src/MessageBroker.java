import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MessageBroker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);
    //here we define lists it is important that they will be concurent safe lists
    private ConcurrentLinkedQueue<SubscriberSession> subscriberSessions = new ConcurrentLinkedQueue<SubscriberSession>();
    private ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<String>();
    //publisherMap is a map were we basically will store all publishers if there will be several instances of them
    private HashMap<SelectionKey, PublisherSession> publisherMap = new HashMap<SelectionKey, PublisherSession>();

    public MessageBroker(){}

    public void startBrokerServer() {
        // we will create a selectable channel , that will have a specific key
        // in docs.oracle you can find that this type of object will have 3 sets of keys
        // first key set will represent the current channel
        // second delected key set, a subset of the key set
        // cancelled key, keys that have been cancelled but they are not lost , and not directly accessible
        Selector selector = null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("Failed to open Selector", e);
        }
        //configure  web socket to localhost and different ports
        configureServerSocketChannels(selector);

        while(true) {
            try {
                selector.selectNow();
            } catch (IOException e) {
                LOGGER.error("Failed to get set of keys from Selector", e);
                System.exit(-1);
            }
            
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey selectedKey = selectedKeys.next();

                if (!selectedKey.isValid()) {
                    continue;
                }

                if (selectedKey.isAcceptable()) {
                    SocketChannel socketChannel = null;
                    try {
                        socketChannel = ((ServerSocketChannel) selectedKey.channel()).accept();
                    } catch (IOException e) {
                        LOGGER.error("Failed to accept ServerSocketChannel connection", e);
                    }
                    if(socketChannel != null) {
                        try {
                            socketChannel.configureBlocking(false);
                        } catch (IOException e) {
                            LOGGER.error("Failed to configure blocking on SocketChannel", e);
                        }
                        //we need to initialize a socket in order to
                        Socket socket = socketChannel.socket();
                        //based on the port nr we will be able to tell apart a subscriber from a publisher
                        //imagine our message broker is already running so when  publisher is connected we
                        //need to tell them apart in order to know from whom to exepect messages and whom should we forward them
                        //this is process is in a while loop so the message broker will always be waiting to whom to assign
                        //tasks, so the scope is to forward the ball , and for that he needs to establish
                        // from whom to whom he will do that
                        if(socket.getLocalPort() == ServerSettings.PUBLISHER_PORT){
                            try {
                                SelectionKey readKey = socketChannel.register(selector, SelectionKey.OP_READ);
                                publisherMap.put(readKey, new PublisherSession(readKey, socketChannel));
                            } catch (ClosedChannelException e) {
                                LOGGER.error("Failed to attach read selector to publisher");
                            }
                        } else {
                            try {
                                SelectionKey writeKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
                                subscriberSessions.add(new SubscriberSession(writeKey, socketChannel));
                                sendMessageBacklog();
                            } catch (ClosedChannelException e) {
                                LOGGER.error("Failed to attach write selector to subscriber");
                            }
                        }
                    }
                } else if (selectedKey.isReadable()) {
                    //we will create sessions for each element from publisher map
                    PublisherSession publisherSession = publisherMap.get(selectedKey);
                    if(publisherSession == null){
                        continue;
                    }
                    //read the input of each publisher
                    String message = publisherSession.read();

                    if(StringUtils.isNotBlank(message)){
                        //and output it to subscribers
                        sendMessageToSubscribers(message);
                    }
                }
            }
        }
    }

    private void sendMessageBacklog() {
        if(pendingMessages.size() > 0){
            Iterator iterator = pendingMessages.iterator();
            while(iterator.hasNext()){
                String message = (String) iterator.next();
                sendMessageToSubscribers(message);
                iterator.remove();
            }
        }
    }

    //this is the initial configuration for comincation channel
    private void configureServerSocketChannels(Selector selector) {
        ServerSocketChannel publisherServerSocketChannel = null;
        try {
            publisherServerSocketChannel = ServerSocketChannel.open();
            publisherServerSocketChannel.configureBlocking(false);
            publisherServerSocketChannel.socket().bind(new InetSocketAddress(ServerSettings.BROKER_HOSTNAME, ServerSettings.PUBLISHER_PORT));
            publisherServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            LOGGER.error("Failed to open publisher ServerSocketChannel", e);
        }

        try {
            ServerSocketChannel subscriberServerSocketChannel = ServerSocketChannel.open();
            subscriberServerSocketChannel.configureBlocking(false);
            subscriberServerSocketChannel.socket().bind(new InetSocketAddress(ServerSettings.BROKER_HOSTNAME, ServerSettings.SUBSCRIBER_PORT));
            subscriberServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            LOGGER.error("Failed to open subscriber ServerSocketChannel", e);
        }
    }
//function that saves all the messages in a map  and adds them into a messaging queue
    private void sendMessageToSubscribers(String message){
        CharsetEncoder encoder = Charset.forName(ServerSettings.DEFAULT_CHARSET).newEncoder();
        int messagesSent = 0;
        Iterator<SubscriberSession> iterator = subscriberSessions.iterator();
        while(iterator.hasNext()){
            SubscriberSession subscriberSession = iterator.next();
            if(subscriberSession.write(message)){
                messagesSent++;
            } else {
                iterator.remove();
            }
        }

        if(messagesSent == 0 ){
            pendingMessages.add(message);
        }

    }

    public static void main(String[] args){
        MessageBroker broker = new MessageBroker();
        broker.startBrokerServer();
    }
}
