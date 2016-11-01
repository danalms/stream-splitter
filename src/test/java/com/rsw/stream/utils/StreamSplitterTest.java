package com.rsw.stream.utils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by DAlms on 10/30/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamSplitterTest {

    private StreamSplitter subject;

    private byte[] inputBuffer;
    private String inputString = "This is the input data for testing";
    private InputStream inputStream;

    @Before
    public void setup() {
        inputBuffer = inputString.getBytes();
        inputStream = new ByteArrayInputStream(inputBuffer);
        subject = new StreamSplitter(inputStream);
    }

    @Test
    public void getStreamAB() throws Exception {
        InputStream streamA = subject.getStreamA();
        InputStream streamB = subject.getStreamB();
        assertNotNull(streamA);
        assertNotNull(streamB);
        assertNotEquals(streamA, streamB);
    }

    @Test
    public void readToEof_writerFinishesFirst() throws Exception {
        // writer's buffer size permits writer to dump it in one write, reader's buffer is requires multiple reads
        Reader readerA = new Reader(subject.getStreamA(), 5);
        Reader readerB = new Reader(subject.getStreamB(), 5);

        Thread threadA = new Thread(readerA);
        Thread threadB = new Thread(readerB);
        threadA.start();
        threadB.start();

        subject.readToEof();

        threadA.join();
        threadB.join();

        assertEquals(inputString, readerA.sb.toString());
        assertFalse(readerA.caughtException);
        assertEquals(inputString, readerB.sb.toString());
        assertFalse(readerB.caughtException);
    }

    @Test
    public void readToEof_readerWaitsOnWriter() throws Exception {
        // buffer size is < input stream length but readers have big buffer
        subject = new StreamSplitter(inputStream, 2);
        Reader readerA = new Reader(subject.getStreamA(), 1024);
        Reader readerB = new Reader(subject.getStreamB(), 1024);

        Thread threadA = new Thread(readerA);
        Thread threadB = new Thread(readerB);
        threadA.start();
        threadB.start();

        subject.readToEof();

        threadA.join();
        threadB.join();

        assertEquals(inputString, readerA.sb.toString());
        assertFalse(readerA.caughtException);
        assertEquals(inputString, readerB.sb.toString());
        assertFalse(readerB.caughtException);
    }

    @Test
    public void readToEof_writerNoReader() throws Exception {
        // make sure buffer size is < input stream length so writer has to wait
        subject = new StreamSplitter(inputStream, 2);
        PipedInputStream inputStream = subject.getStreamA();

        Runnable writer = new Runnable() {
            @Override
            public void run() {
                try {
                    subject.readToEof();
                } catch (Exception ex) {
                    // just exit
                }
            }
        };

        Thread writerThread = new Thread(writer);
        writerThread.start();

        // interrupt the blocked writer
        inputStream.close();
        writerThread.join();
        assertFalse(writerThread.isAlive());
    }

    @Test
    public void readToEof_close() throws Exception {
        InputStream input = mock(InputStream.class);
        subject = new StreamSplitter(input);

        PipedOutputStream outputA = mock(PipedOutputStream.class);
        PipedOutputStream outputB = mock(PipedOutputStream.class);
        subject.getStreamA(outputA);
        subject.getStreamB(outputB);
        when(input.read(anyVararg(), anyInt(), anyInt())).thenReturn(0);

        subject.readToEof();
        verify(input).close();
        verify(outputA).close();
        verify(outputB).close();
    }

    private class Reader implements Runnable {

        private StringBuffer sb = new StringBuffer();
        private InputStream inputStream;
        private byte[] buffer;
        private boolean caughtException = false;

        public Reader(InputStream inputStream, int bufSize) {
            this.inputStream = inputStream;
            this.buffer = new byte[bufSize];
        }

        @Override
        public void run() {
            int numRead;
            try {
                while ((numRead = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    sb.append(new String(buffer, 0, numRead));
                }
            } catch (Exception ex) {
                this.caughtException = true;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}