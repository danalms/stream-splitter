package com.rsw.auth.stream.service;

import com.rsw.auth.stream.domain.BlueResult;

import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * Created by dalms on 10/26/16.
 */
public interface BlueDelegateService {

    Future<BlueResult> performBlue(InputStream fileInputStream, final String fileName);

}
