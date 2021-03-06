package c8y.trackeragent_it.simulator;

import java.util.concurrent.atomic.AtomicLong;

import c8y.trackeragent.utils.message.TrackerMessage;
import c8y.trackeragent_it.service.SocketWritter;

public class SimulatorTask {
    
    private final static AtomicLong SEQ = new AtomicLong(1); 
    
    private final SocketWritter socketWriter;
    private final TrackerMessage message;
    private final long id;
    
    public SimulatorTask(SocketWritter socketWriter, TrackerMessage message) {
        this.socketWriter = socketWriter;
        this.message = message;
        this.id = SEQ.getAndIncrement();
    }

    public SocketWritter getSocketWriter() {
        return socketWriter;
    }

    public TrackerMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SimulatorTask [id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }
    
    
    

}
