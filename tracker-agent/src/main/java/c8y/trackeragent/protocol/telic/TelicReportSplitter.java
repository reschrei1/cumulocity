package c8y.trackeragent.protocol.telic;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.tracker.BaseReportSplitter;
import c8y.trackeragent.utils.ByteHelper;

public class TelicReportSplitter extends BaseReportSplitter {

    private static Logger logger = LoggerFactory.getLogger(BaseReportSplitter.class);

    private static final int CONNECTION_HEADER = 28;
    private static final int REPORT_HEADER = 4;

    private boolean firstReportInTheConnection = true;

    public TelicReportSplitter() {
        super(TrackingProtocol.TELIC.getReportSeparator());
    }

    @Override
    public List<String> split(byte[] reports) {
        try {
            if (firstReportInTheConnection) {
                 reports = ByteHelper.stripHead(reports, CONNECTION_HEADER);
            }
            return super.split(reports);
        } finally {
            firstReportInTheConnection = false;
        }
    }

    @Override
    protected byte[] extractNext(List<String> result, byte[] tail) {
        tail = ByteHelper.stripHead(tail, REPORT_HEADER);
        if (tail == null) {
            return null;
        } else {
            return super.extractNext(result, tail);
        }
    }

}
