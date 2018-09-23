import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

//subscriber class
public class Subscriber {
    private static Logger LOGGER = LoggerFactory.getLogger(Subscriber.class);

    private SocketChannel socketChannel;

    public Subscriber(){}
//subscriber needs to connect in the same way as publisher, subscriber port may deffer from the publisher's, just to diferentiate them
    public void connect(){
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(ServerSettings.BROKER_HOSTNAME, ServerSettings.SUBSCRIBER_PORT));
        } catch (IOException e) {
            LOGGER.error("Failed to connect to broker...", e);
        }
    }
// subscriber will read from that socketChannel where we wrote the encoded and serialized message of publisher
    public void listen(){
        while (true) {
            String message = "";
            ByteBuffer byteBuffer = ByteBuffer.allocate(512);

            while (socketChannel.isConnectionPending()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    LOGGER.error("Connection pending sleep was interrupted", e);
                }
            }

            while (byteBuffer.hasRemaining()) {
                int bytesRead = 0;
                try {
                    bytesRead = socketChannel.read(byteBuffer);
                } catch (IOException e) {
                    LOGGER.error("Failed to read from channel");
                }
                if (bytesRead == -1) {
                    continue;
                }

                byteBuffer.flip();
                Charset charset = Charset.forName(ServerSettings.DEFAULT_CHARSET);
                CharsetDecoder decoder = charset.newDecoder();

                try {
                    message = decoder.decode(byteBuffer).toString();
                } catch (CharacterCodingException e) {
                    LOGGER.error("Failed to decode message from server...", e);
                }
            }

            LOGGER.info("Message received: '" + message + "'");

            try {
                ObjectMapper mapper = new ObjectMapper();
                MessageWrapper messageWrapper = mapper.readValue(message, MessageWrapper.class);

                Class<?> cls = Class.forName(messageWrapper.getDataType());
                if(cls.newInstance() instanceof CustomObject){
                    CustomObject customObject = mapper.readValue(messageWrapper.getMessage(), CustomObject.class);

                    LOGGER.info("Converted message to CustomObject with message: '" + customObject.getMessage() + "'\n" +
                            "And UUID: '" + customObject.getId() + "'");
                } else if (cls.newInstance() instanceof  CustomObject2){
                    CustomObject2 customObject2 = mapper.readValue(messageWrapper.getMessage(), CustomObject2.class);

                    LOGGER.info("Converted message to CustomObject with message: '" + customObject2.getMessage() + "'\n" +
                            "And UUID: '" + customObject2.getId() + "'\n" +
                            "And aThirdField: '" + customObject2.getaThirdField() + "'");
                }

            } catch (IOException e) {
                LOGGER.error("Failed to decode object...", e);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Failed to decode data...", e);
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Subscriber subscriber = new Subscriber();
        subscriber.connect();
        subscriber.listen();
    }
}
