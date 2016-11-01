package com.rsw.stream.service;

import com.rsw.stream.domain.GreenResult;

import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * Created by dalms on 10/26/16.
 */
public interface GreenDelegateService {

    Future<GreenResult> performGreen(InputStream fileInputStream, String fileName);

}
