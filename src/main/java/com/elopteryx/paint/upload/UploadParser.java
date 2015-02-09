/*
 * Copyright (C) 2015- Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elopteryx.paint.upload;

import com.elopteryx.paint.upload.impl.AsyncUploadParser;
import com.elopteryx.paint.upload.impl.BlockingUploadParser;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Builder class. Provides a fluent API for the users to
 * customize the parsing process.
 */
public abstract class UploadParser {
    /**
     * The type of the HTTP request.
     */
    private static final String POST_METHOD = "POST";

    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";

    /**
     * The request object.
     */
    protected final HttpServletRequest request;

    /**
     * The response object.
     */
    protected final HttpServletResponse response;

    /**
     * The part begin callback, called at the beginning of each part parsing. Mandatory.
     */
    protected OnPartBegin partBeginCallback;

    /**
     * The part end callback, called at the end of each part parsing. Mandatory.
     */
    protected OnPartEnd partEndCallback;

    /**
     * The request callback, called after every part has been processed. Optional.
     */
    protected OnRequestComplete requestCallback;

    /**
     * The error callback, called when an error occurred. Optional.
     */
    protected OnError errorCallback;

    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     */
    protected int sizeThreshold;

    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    protected long maxPartSize = -1;

    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    protected long maxRequestSize = -1;

    /**
     * Protected constructor, to prevent invalid usages.
     * @param request The servlet request
     * @param response The servlet response
     */
    protected UploadParser(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response) {
        this.request = requireNonNull(request);
        this.response = requireNonNull(response);
    }

    /**
     * Utility method which can be used to check whether the request
     * should be processed by this parser or not.
     * @param request The servlet request
     * @return Whether the request is a proper multipart request
     */
    public static boolean isMultipart(@Nonnull HttpServletRequest request) {
        return POST_METHOD.equalsIgnoreCase(request.getMethod()) && request.getContentType() != null &&
                request.getContentType().toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

    /**
     * Returns a parser implementation, allowing the caller to set configuration.
     * @param request The servlet request
     * @param response The servlet response
     * @return A parser object
     * @throws ServletException If the parameters are invalid
     */
    public static UploadParser newParser(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response)
            throws ServletException {
        if (!isMultipart(request))
            throw new ServletException("Not a multipart request!");
        return request.isAsyncSupported() ? new AsyncUploadParser(request, response) : new BlockingUploadParser(request, response);
    }

    /**
     * Sets a callback for each part, called at the beginning.
     * @param partBeginCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onPartBegin(@Nonnull OnPartBegin partBeginCallback) {
        this.partBeginCallback = requireNonNull(partBeginCallback);
        return this;
    }

    /**
     * Sets a callback for each part, called at the end.
     * @param partEndCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onPartEnd(@Nonnull OnPartEnd partEndCallback) {
        this.partEndCallback = requireNonNull(partEndCallback);
        return this;
    }

    /**
     * Sets a callback for the request, called after each part is processed.
     * @param requestCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onRequestComplete(@Nonnull OnRequestComplete requestCallback) {
        this.requestCallback = requireNonNull(requestCallback);
        return this;
    }

    /**
     * Sets a callback for the errors, called if any error occurs.
     * @param errorCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onError(@Nonnull OnError errorCallback) {
        this.errorCallback = requireNonNull(errorCallback);
        return this;
    }

    /**
     * Sets the amount of bytes to buffer in the memory, before
     * calling the part end callback.
     * @param sizeThreshold The amount to use
     * @return The parser will return itself
     */
    public UploadParser sizeThreshold(@Nonnegative int sizeThreshold) {
        this.sizeThreshold = Math.max(sizeThreshold, 0);
        return this;
    }

    /**
     * Sets the maximum allowed size for each part. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.PartSizeException} exception.
     * @param maxPartSize The amount to use
     * @return The parser will return itself
     */
    public UploadParser maxPartSize(@Nonnegative long maxPartSize) {
        this.maxPartSize = Math.max(maxPartSize, -1);
        return this;
    }

    /**
     * Sets the maximum allowed size for the request. Exceeding this
     * will result in a {@link com.elopteryx.paint.upload.errors.RequestSizeException} exception.
     * @param maxRequestSize The amount to use
     * @return The parser will return itself
     */
    public UploadParser maxRequestSize(@Nonnegative long maxRequestSize) {
        this.maxRequestSize = Math.max(maxRequestSize, -1);
        return this;
    }

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @throws IOException If an error occurs with the IO
     */
    public abstract void setup() throws IOException;
}