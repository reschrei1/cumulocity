package c8y.trackeragent.utils;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import c8y.Position;
import c8y.trackeragent.TelicConstants;

import com.google.common.base.Function;

public class Reports {
    
    public static final String HEADER = "0000123456|262|02|003002016";
    
    public static byte[] getTelicReportBytes(final String imei, Position... positions) throws UnsupportedEncodingException {
        Iterable<String> reports = from(asList(positions)).transform(new Function<Position, String>() {
           public String apply(Position input) {
               return getTelicReportStr(imei, input);
           }
        });
        int reportsSize = calculateReportsSize(reports);
        byte[] HEADERBYTES = HEADER.getBytes("US-ASCII");
        byte[] bytes = new byte[HEADERBYTES.length + reportsSize + 1];
        
        int i = 0;
        for (byte b : HEADERBYTES) {
            bytes[i++] = b;
        }
        for(String report : reports) {
            i += 5;
            byte[] reportBytes = report.getBytes("US-ASCII");
            for (byte b : reportBytes) {
                bytes[i++] = b;
            }
        }
        return bytes;
    }
    
    private static int calculateReportsSize(Iterable<String> reports) throws UnsupportedEncodingException {
        int result = 0;
        for (String report : reports) {
            result += report.getBytes("US-ASCII").length;
            result += 5;
        }
        return result;
    }

    public static String getTelicReportStr(String imei, Position position) {
        return "0721" + 
                imei + 
                "99,200311121210,0,200311121210," +
                asTelicStringCoord(position.getLng()) + 
                "," +
                asTelicStringCoord(position.getLat()) + 
                ",3,4,67,4,,," + 
                asTelicStringCoord(position.getAlt()) +
                ",11032,,010 1,00,238,0,0,0";
    }
    
    public static String[] getTelicReport(String imei, Position position) {
        return getTelicReportStr(imei, position).split(TelicConstants.FIELD_SEP);
    }
    
    private static String asTelicStringCoord(BigDecimal coord) {
        return coord.toString().replaceAll("\\.", "");
    }
}
