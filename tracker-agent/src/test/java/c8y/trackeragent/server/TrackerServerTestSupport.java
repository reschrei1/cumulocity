package c8y.trackeragent.server;

import static c8y.trackeragent.utils.ByteHelper.getString;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c8y.trackeragent.context.OperationContext;
import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.server.TrackerServerEvent.ReadDataEvent;
import c8y.trackeragent.tracker.ConnectedTracker;
import c8y.trackeragent.tracker.ConnectedTrackerFactory;
import c8y.trackeragent.utils.ByteHelper;

public abstract class TrackerServerTestSupport {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackerServerTestSupport.class);

    protected static final int PORT = 5100;
    
    private TrackerServer server;
    private final ExecutorService executorService = newFixedThreadPool(100);
    protected CountDownLatch reportExecutorLatch;
    protected final List<TestConnectedTrackerImpl> executors = synchronizedList(new ArrayList<TestConnectedTrackerImpl>());
    protected final ConnectionsContainer connectionsContainer = new ConnectionsContainer();
    protected final List<SocketWriter> writers = new ArrayList<SocketWriter>();
    protected ConnectedTracker customTracker = null;
    
    @Before
    public void before() throws Exception {
        reportExecutorLatch = new CountDownLatch(0);
        TrackerServerEventHandler eventHandler = new TrackerServerEventHandler(new TestConnectedTrackerFactoryImpl(), connectionsContainer);
        eventHandler.init();
        server = new TrackerServer(eventHandler);
        server.start(PORT);
        executorService.execute(server);
    }
    
    @After
    public void after() throws IOException {
        server.close();
    }
    
    protected SocketWriter newWriter() throws Exception {
        SocketWriter writer = new SocketWriter();
        writers.add(writer);
        executorService.execute(writer);
        return writer;
    }
    
    protected void assertThatReportsHandled(String... reports) {
        Object[] expected = new Object[reports.length];
        for (int index = 0; index < reports.length; index++) {
            expected[index] = new TestConnectedTrackerImpl(reports[index]);
        }
        assertThat(executors).contains(expected);
    }
    
    protected void setCountOfExpectedReports(int count) {
        reportExecutorLatch = new CountDownLatch(count);
    }
    
    protected void waitForReports() throws InterruptedException {
        reportExecutorLatch.await(2, TimeUnit.SECONDS);
    }
    
    protected class SocketWriter implements Runnable {

        volatile Deque<String> toW = new ArrayDeque<String>();
        Socket client;

        public SocketWriter() throws Exception {
            client = new Socket("localhost", PORT);
        }

        void push(String text) throws Exception {
            toW.addLast(text);
            Thread.sleep(1);
        }
                
        void stop() throws Exception {
            client.close();
            Thread.sleep(1);
        }

        @Override
        public void run() {
            while (true) {
                if (!toW.isEmpty()) {
                    String toWrite = toW.removeFirst();
                    write(toWrite);
                }
            }
        }

        private void write(String toWrite) {
            try {
                client.getOutputStream().write(ByteHelper.getBytes(toWrite));
                Thread.sleep(1);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

    }
    
    protected class TestConnectedTrackerFactoryImpl implements ConnectedTrackerFactory {
        
        @Override
        public ConnectedTracker create(ReadDataEvent readData) {
            TestConnectedTrackerImpl result = new TestConnectedTrackerImpl();
            logger.info("Created executor for data " + getString(readData.getData()));
            executors.add(result);
            logger.info("Total executors " + executors.size());
            return result;
        }
    }
    
    
    protected class DummyConnectedTracker implements ConnectedTracker {

        @Override
        public void executeOperation(OperationContext operation) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void executeReports(ConnectionDetails connectionDetails, byte[] reports) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public TrackingProtocol getTrackingProtocol() {
            return TrackingProtocol.COBAN;
        }
    }
    
    protected class TestConnectedTrackerImpl extends DummyConnectedTracker {
        
        private final List<String> processed;
        private ConnectionDetails connectionDetails;
        
        public TestConnectedTrackerImpl(String... processed) {
            this.processed = new ArrayList<String>();
            this.processed.addAll(asList(processed));
        }
        
        @Override
        public void executeReports(ConnectionDetails connectionDetails, byte[] reports) {
            this.connectionDetails = connectionDetails;
            logger.info("Handled report: \'{}\'", getString(reports));
            if (customTracker != null) {
                customTracker.executeReports(connectionDetails, reports);
            }
            String reportsStr = getString(reports);
            List<String> reportList = asList(reportsStr.split(";"));
            for (String report : reportList) {
                processed.add(report);
                reportExecutorLatch.countDown();
            }
        }

        public List<String> getProcessed() {
            return processed;
        }
        
        public ConnectionDetails getConnectionDetails() {
            return connectionDetails;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((processed == null) ? 0 : processed.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TestConnectedTrackerImpl other = (TestConnectedTrackerImpl) obj;
            if (processed == null) {
                if (other.processed != null)
                    return false;
            } else if (!processed.equals(other.processed))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return processed.toString();
        }
    }


    
}