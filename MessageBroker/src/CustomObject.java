import java.io.Serializable;
import java.util.UUID;

//wraper for our message in this way we will have easier access to text content, message, datatype, etc.
//THIS CLASS is not used, but it is kept to shpow that clients can send objects of different complexity
// and number of fiels can be modified
public class CustomObject implements Serializable{
    private String message;
    private UUID id;

    public CustomObject(){
        // No args constructor 
    }

    public CustomObject(String message, UUID id){
        this.message = message;
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
