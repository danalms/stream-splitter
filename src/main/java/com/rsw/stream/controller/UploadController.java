package com.rsw.stream.controller;

import com.google.common.base.Stopwatch;
import com.rsw.stream.domain.BlueResult;
import com.rsw.stream.domain.GreenResult;
import com.rsw.stream.service.BlueDelegateService;
import com.rsw.stream.service.GreenDelegateService;
import com.rsw.stream.utils.StreamSplitter;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by dalms on 10/21/16.
 *
 * This is a simple file uploader to demonstrate a stream splitting utility.
 * The consumers of the stream run on separate threads and in this case are actually REST calls to delegate
 * services.  The REST delegates pass the stream on through without buffering the entire contents based on how
 * the RestTemplate is configured
 */
@Controller
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    private static final int STREAM_SPLITTER_BUF_SIZE = 4096;

    @Autowired
    private GreenDelegateService greenDelegateService;

    @Autowired
    private BlueDelegateService blueDelegateService;


    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public String home() {
        return "/";
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public String uploadFile(HttpServletRequest request) throws IOException {

        Upload upload = null;
        try {
            upload = parseMultipart(request);
        } catch (IOException | FileUploadException ex) {
            LOG.error("Problem parsing multipart file upload request", ex);
        }

        if (upload != null) {
            StreamSplitter splitter = new StreamSplitter(upload.inputStream, STREAM_SPLITTER_BUF_SIZE);

            try {
                LOG.info("Receiving file {} for green and blue processing...", upload.name);
                Stopwatch timer = Stopwatch.createStarted();

                // async invocation
                Future<GreenResult> greenExec = greenDelegateService.performGreen(splitter.getStreamA(), upload.name);

                // async invocation
                Future<BlueResult> blueExec = blueDelegateService.performBlue(splitter.getStreamB(), upload.name);

                // Read the entire input stream, feeding StreamA and StreamB threads
                // See the note in StreamSplitter regarding write blocking and exception handling

                splitter.readToEof();

                // wait for green thread to complete (Future.get() blocks)
                GreenResult greenResult = greenExec.get();
                LOG.info("Green service processing of file {} is complete", upload.name);

                // wait (if necesssary) for blue thread to complete
                BlueResult blueResult = blueExec.get();
                LOG.info("Blue service processing of file {} is complete", upload.name);

                timer.stop();
                LOG.info("Overall delegate and upload time was {} ms.", timer.elapsed(TimeUnit.MILLISECONDS));

            } catch(Exception ex) {
                LOG.error("Exception during file streaming!", ex);
            }
        }
        return "redirect:/upload";
    }

    private Upload parseMultipart(HttpServletRequest request) throws IOException, FileUploadException {
        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator itemIterator = fileUpload.getItemIterator(request);
        InputStream stream;
        while(itemIterator.hasNext()) {
            FileItemStream item = itemIterator.next();
            stream = item.openStream();

            if(!item.isFormField()){
                return new Upload(stream, item.getName());
            }
        }

        throw new IllegalArgumentException("No Stream on upload");
    }

    private static class Upload {
        private InputStream inputStream;
        private String name;

        public Upload(InputStream inputStream, String name) {
            this.inputStream = inputStream;
            this.name = name;
        }
    }

}
