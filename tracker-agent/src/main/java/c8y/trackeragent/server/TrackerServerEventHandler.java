package c8y.trackeragent.server;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.TrackerConfiguration;
import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.server.TrackerServerEvent.CloseConnectionEvent;
import c8y.trackeragent.server.TrackerServerEvent.ReadDataEvent;
import c8y.trackeragent.server.writer.OutWriter;
import c8y.trackeragent.server.writer.OutWriterImpl;
import c8y.trackeragent.tracker.ConnectedTracker;
import c8y.trackeragent.tracker.ConnectedTrackerFactory;

@Component
public class TrackerServerEventHandler implements ActiveConnectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(TrackerServerEventHandler.class);

    private final ExecutorService workers;
    private final ConnectedTrackerFactory connectedTrackerFactory;    
    private final ConnectionsContainer connectionsContainer;    
    private final Object monitor = new Object();
    private final TrackerConfiguration trackerConfiguration;

    @Autowired
    public TrackerServerEventHandler(ConnectedTrackerFactory connectedTrackerFactory, ConnectionsContainer connectionsContainer, TrackerConfiguration trackerConfiguration) {
        this.connectedTrackerFactory = connectedTrackerFactory;
        this.connectionsContainer = connectionsContainer;
        this.trackerConfiguration = trackerConfiguration;
        this.workers = newFixedThreadPool(trackerConfiguration.getNumberOfReaderWorkers());
    }

    @PostConstruct
    public void init() {
        logger.info("Number of reader workers: {}.", trackerConfiguration.getNumberOfReaderWorkers());
    }
    
    public void shutdownWorkers(){
        this.workers.shutdown();
    }

    public void handle(ReadDataEvent readDataEvent) {
        try {
            synchronized (monitor) {
                ActiveConnection connection = getActiveConnection(readDataEvent);
                connection.getReportBuffer().append(readDataEvent.getData(), readDataEvent.getNumRead());
                ReaderWorker worker = new ReaderWorker(this);
                workers.execute(worker);
            }
        } catch (Exception e) {
            logger.error("Exception handling read event " + readDataEvent, e);
        }
    }
    
    public void handle(CloseConnectionEvent closeConnectionEvent) {
        synchronized (monitor) {
            connectionsContainer.remove(closeConnectionEvent.getChannel());
        }
    }

    private ActiveConnection getActiveConnection(ReadDataEvent readEvent) throws Exception {
        ActiveConnection connection = connectionsContainer.get(readEvent.getChannel());
        if (connection == null) {
            connection = createConnection(readEvent);
            connectionsContainer.add(connection);
        }
        return connection;
    }

    private ActiveConnection createConnection(ReadDataEvent readEvent) throws Exception {
        ConnectedTracker connectedTracker = connectedTrackerFactory.create(readEvent);
        TrackingProtocol trackingProtocol = connectedTracker.getTrackingProtocol();
        OutWriter outWriter = new OutWriterImpl(readEvent.getServer(), readEvent.getChannel());
        ConnectionDetails connectionDetails = new ConnectionDetails(trackingProtocol, outWriter, readEvent.getChannel());
        return new ActiveConnection(connectionDetails, connectedTracker);
    }

    @Override
    public ActiveConnection next() {
        synchronized (monitor) {
            return connectionsContainer.next();
        }
    }
}
