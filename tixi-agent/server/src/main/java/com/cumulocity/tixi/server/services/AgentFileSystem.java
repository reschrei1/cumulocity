package com.cumulocity.tixi.server.services;

import static com.google.common.base.Optional.fromNullable;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

import java.io.*;
import java.text.SimpleDateFormat;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentFileSystem {

    private static final Logger log = LoggerFactory.getLogger(AgentFileSystem.class);

    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private File incomingPath;

    private File processingPath;

    @Autowired
    public AgentFileSystem(
            @Value("${TIXI.jsonfiles.incoming}") String incomingPath,
            @Value("${TIXI.jsonfiles.processing}") String processingPath) {
        this.incomingPath = new File(incomingPath);
        this.processingPath = new File(processingPath);
    }

    @PostConstruct
    public void init() {
        incomingPath.mkdirs();
        processingPath.mkdirs();
    }
    
    public static File getFile(File parent, String fileName) {
        return new File(parent, fileName);
    }

    public String writeIncomingFile(String requestId, InputStream inputStream) {
        String fileName = fromNullable(requestId).or("") + "_" + getTimestamp() + ".xml";
        writeToFile(inputStream, getFile(incomingPath, fileName));
        return fileName;
    }
    
    public String readFromFile(String filename) {
        FileInputStream stream = null;
        try {
            stream = openInputStream(new File(incomingPath, filename));
            return IOUtils.toString(stream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("I/O error!", e);
        } finally {
            closeQuietly(stream);
        }
    }

    public static String getTimestamp() {
        return TIMESTAMP_FORMAT.format(new DateTime().toDate());
    }
    
    private void writeToFile(InputStream inputStream, File file) {
        FileOutputStream outputStream = null;
        try {
            outputStream = openOutputStream(file);
            copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("I/O error!", e);
        } finally {
            closeQuietly(outputStream);
            closeQuietly(inputStream);
        }
    }
}

