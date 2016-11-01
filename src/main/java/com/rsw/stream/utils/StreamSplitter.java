package com.rsw.stream.utils;

import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

/**
 * Provides two Pipe-based inputs from a single input.
 * This class manages the reading from the input and writing to one or two pipes
 *
 * Created by DAlms on 10/30/16.
 */
public class StreamSplitter {

    private InputStream input;
    private PipedOutputStream[] outputs = new PipedOutputStream[2];
    private PipedInputStream[] readers = new PipedInputStream[2];
    private byte[] buffer;
    private int bufSize;
    private static final int DEFAULT_BUF_SIZE = 2048;
    private static final int PIPE_A = 0;
    private static final int PIPE_B = 1;


    public StreamSplitter(InputStream input) {
        init(input, DEFAULT_BUF_SIZE);
    }

    public StreamSplitter(InputStream input, int bufSize) {
        init(input, bufSize);
    }

    public PipedInputStream getStreamA() throws IOException {
        if (readers[PIPE_A] != null) {
            return readers[PIPE_A];
        }
        return initReader(new PipedOutputStream(), PIPE_A);
    }

    public PipedInputStream getStreamA(PipedOutputStream output) throws IOException {
        Assert.isNull(readers[PIPE_A]);
        return initReader(output, PIPE_A);
    }

    public PipedInputStream getStreamB() throws IOException {
        if (readers[PIPE_B] != null) {
            return readers[PIPE_B];
        }
        return initReader(new PipedOutputStream(), PIPE_B);
    }

    public PipedInputStream getStreamB(PipedOutputStream output) throws IOException {
        Assert.isNull(readers[PIPE_B]);
        return initReader(output, PIPE_B);
    }

    public void readToEof() throws IOException {
        try {
            buffer = new byte[bufSize];
            int numRead;
            while ((numRead = input.read(buffer, 0, buffer.length)) > 0) {
                sendOutput(numRead);
            }
        } finally {
            close();
        }
    }

    public void close() {
        IOUtils.closeQuietly(input);
        Arrays.asList(outputs).stream().forEach(IOUtils::closeQuietly);
    }

    private void sendOutput(int numBytes) throws IOException {
        for (int ix = 0; ix < outputs.length; ix++) {
            if (outputs[ix] != null) {
                outputs[ix].write(buffer, 0, numBytes);
            }
        }
    }

    private void init(InputStream input, int bufSize) {
        Assert.notNull(input);
        this.input = input;
        this.bufSize = bufSize;
    }

    private PipedInputStream initReader(PipedOutputStream output, int index) throws IOException {
        Assert.notNull(output);
        Assert.isTrue(index >= 0 && index < outputs.length);
        outputs[index] = output;
        readers[index] = new PipedInputStream(output, bufSize);
        return readers[index];
    }
}
