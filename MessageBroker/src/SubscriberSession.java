import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;


public class SubscriberSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherSession.class);

    private final CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();

    private SocketChannel socketChannel;
    private SelectionKey selectionKey;

    public SubscriberSession(SelectionKey selectionKey, SocketChannel socketChannel){
        this.selectionKey = selectionKey;
        this.socketChannel = socketChannel;
        try {
            this.socketChannel.configureBlocking(false);
        } catch (IOException e) {
            LOGGER.error("Failed to set a SubscriberSesion as non-blocking.");
        }
    }

    public void disconnect(){
        try{
            if(selectionKey != null){
                selectionKey.cancel();
            }

            if(socketChannel != null){
                socketChannel.close();
            }
        } catch (IOException e) {
            // Ignore
            LOGGER.info("Failed to close channel and/or cancel key.", e);
        }
    }

    public boolean write(String message) {
        try {
            socketChannel.write(encoder.encode(CharBuffer.wrap(message)));
            return true;
        } catch (Exception e){
            disconnect();
            LOGGER.error("Failed to write to a subscriber channel.  Connection probably closed by remote host and/or no subscribers are currently connected.  " +
                    "The message that failed was: '" + message + "'");
            return false;
        }
    }
}