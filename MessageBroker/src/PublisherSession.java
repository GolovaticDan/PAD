import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * Created by Jonathan Sterling on 22/10/2015.
 */
public class PublisherSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherSession.class);

    private SocketChannel socketChannel;
    private SelectionKey selectionKey;
    private ByteBuffer buffer = ByteBuffer.allocate(512);

    public PublisherSession(SelectionKey selectionKey, SocketChannel socketChannel){
        this.selectionKey = selectionKey;
        this.socketChannel = socketChannel;
        try {
            this.socketChannel.configureBlocking(false);
        } catch (IOException e) {
            LOGGER.error("Failed to set a PublisherSession as non-blocking.");
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

    public String read() {
        String message = null;

        try {
            while (buffer.hasRemaining()) {
                int bytesRead = 0;
                try {
                    bytesRead = socketChannel.read(buffer);
                } catch (IOException e) {
                    LOGGER.error("Failed to read byteBuffer", e);
                }

                buffer.flip();

                message = decodeMessage(buffer, message);
            }
        } catch(Exception e){
            disconnect();
            LOGGER.error("Failed to read from a producer", e);
        }

        return message;
    }

    private String decodeMessage(ByteBuffer byteBuffer, String message) {
        Charset charset = Charset.forName(ServerSettings.DEFAULT_CHARSET);
        CharsetDecoder decoder = charset.newDecoder();
        try {
            message = decoder.decode(byteBuffer).toString();
        } catch (CharacterCodingException e) {
            LOGGER.error("Failed to decode message from producer", e);
        }
        return message;
    }
}
