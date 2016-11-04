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
 * NOTE: pipe streams use a circular buffer of finite space, so the splitter will block on a write if:
 *   - it has filled the buffer and has more input to write AND
 *   - either consumer thread is not consuming the pipe, thereby not freeing up the pipe buffer space
 * If a thread dies and its exception is uncaught, leaving its stream open, this blocked write could
 * hang indefinitely.
 * The easiest way to avoid this is to have the consuming services wrap all logic in a try/finally
 * and close the input stream on any exception.
 * Alternatively, it may be possible to override the exception handling behavior of the
 * java.util.concurrent.FutureTask used by Spring's Async feature, but it may not be trivial.
 * We want to use Future<> in this context to join the threads, but Spring does not accommodate an
 * UnhandledExceptionHandler when using Future return types.  Use of the Future return type causes the
 * exception to be held until when/if Future.get() is called.  This complicates things in the
 * multi-thread stream write block scenario assuming the splitter run and the Future.get() are both
 * in the main thread.
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
