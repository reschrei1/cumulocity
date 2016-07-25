package c8y.trackeragent.nioserver;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class NioServerFuzzyTest extends NioServerTestSupport {

    private static final int TOTAL_WRITERS = 40;
    private static final int TOTAL_REPORST_PER_WRITER = 100;
    private final List<SocketWriter> writers = new ArrayList<SocketWriter>();
    private final List<String> sendReports = new ArrayList<String>();

    @Before
    public void before() throws Exception {
        super.before();
        for (int i = 0; i < TOTAL_WRITERS; i++) {
            writers.add(newWriter());
        }
    }

    @Test
    public void shouldReadManyReports() throws Exception {
        reportExecutorLatch = new CountDownLatch(TOTAL_WRITERS * TOTAL_REPORST_PER_WRITER);

        for (int reportNo = 0; reportNo < TOTAL_REPORST_PER_WRITER; reportNo++) {
            String report = "" + reportNo;
            sendReports.add(report);
            for (SocketWriter writer : writers) {
                writer.push(report + ";");
            }
        }

        reportExecutorLatch.await(2, TimeUnit.SECONDS);
        assertThat(reportExecutorLatch.getCount()).isEqualTo(0L);
        for (ReaderWorkerExecutorImpl executor : executors) {
            assertThat(executor.getProcessed()).hasSize(TOTAL_REPORST_PER_WRITER);
            assertThat(executor.getProcessed()).isEqualTo(sendReports);
        }
    }

}
