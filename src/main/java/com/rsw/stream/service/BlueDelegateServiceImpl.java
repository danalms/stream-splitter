package com.rsw.stream.service;

import com.google.common.base.Stopwatch;
import com.rsw.stream.domain.BlueResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Generic delegate service which calls a remote service
 */
@Service
public class BlueDelegateServiceImpl implements BlueDelegateService {

    private static final Logger LOG = LoggerFactory.getLogger(BlueDelegateServiceImpl.class);

    private RestTemplate restTemplate;

    @Autowired
    public BlueDelegateServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Async
    public Future<BlueResult> performBlue(InputStream fileInputStream, final String fileName) {

        try {
            InputStreamResource contentsAsResource = new InputStreamResource(fileInputStream) {
                @Override
                public String getFilename() {
                    return fileName;
                }

                @Override
                public long contentLength() throws IOException {
                    // Need to read the whole stream to get the length.
                    return -1;
                }
            };

            MultiValueMap<String, Object> fields = new LinkedMultiValueMap<>();
            fields.add("inputStream", contentsAsResource);
            fields.add("fileName", fileName);
            String url = "http://blue-service/file";

            LOG.info(" :: sending file {} to Blue service...", fileName);
            Stopwatch timer = Stopwatch.createStarted();

            ResponseEntity<BlueResult> result = null;
            result = restTemplate.exchange(url, HttpMethod.POST,
                        new HttpEntity<MultiValueMap<String, Object>>(fields), BlueResult.class);
            timer.stop();
            LOG.info(" :: Blue service completed in {} ms.", timer.elapsed(TimeUnit.MILLISECONDS));
            return new AsyncResult<BlueResult>(result.getBody());
        } catch (Exception e) {
            LOG.error("Error invoking Blue delegate service", e);
            throw e;
        } finally {
            // Important to notify and unblock pipe writer thread!
            IOUtils.closeQuietly(fileInputStream);
        }
    }

}
