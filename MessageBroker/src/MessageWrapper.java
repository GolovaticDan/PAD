import java.sql.Timestamp;


//wraper for our message in this way we will have easier access to text content, message, datatype, etc.
public class MessageWrapper {
    private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private String dataType;
    private String message;

    public MessageWrapper(){
        // No args constructor
    }

    public MessageWrapper(String dataType, String message){
        this.dataType = dataType;
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getDataType() {
        return dataType;
    }
}
