/*
 * Copyright 2010-2016 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.servlets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static com.googlecode.webutilities.util.Utils.*;

/**
 * Servlet that allows proxying the requests to another external remote service/API. This is useful when the target API
 * doesn't support CORS or JSONP. In such cases the request to remote third-party service has to be routed through our
 * own server to make it look like we are serving it from our own server system.
 *
 * This Servlet supports the configurable <code>init-params</code> to dynamically inject custom headers in the
 * request (<code>injectRequestHeaders</code>) before requesting the target service. This is useful to send custom
 * authorization or token request headers. Similarly this Servlet can return custom response headers
 * (injectResponseHeaders) back to the requesting client. Currently these headers are static and whatever is defined as
 * the value for these will be set.
 *
 * <code>baseUri</code> init-param is the base URL for the target service/API. Any path after the mapping of this
 * servlet will be appended to the baseUri to build the target URL. Also any query parameters and request headers from
 * client will be passed as it to the target URL. If additional request headers parameter are specified
 * (<code>injectRequestHeaders</code>), those will also be passed to target service.
 *
 *
 * @author rpatil
 * @version 1.0
 */
public class WebProxyServlet extends HttpServlet {

    public static final String INIT_PARAM_BASE_URI = "baseUri";

    public static final String INIT_PARAM_INJECT_REQUEST_HEADERS = "injectRequestHeaders";

    public static final String INIT_PARAM_INJECT_RESPONSE_HEADERS = "injectResponseHeaders";

    private String baseUri;

    private Map<String, String> requestHeadersToInject;

    private Map<String, String> responseHeadersToInject;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebProxyServlet.class.getName());


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.baseUri = readString(config.getInitParameter(INIT_PARAM_BASE_URI), "");

        this.requestHeadersToInject = buildHeadersMapFromString(
                readString(config.getInitParameter(INIT_PARAM_INJECT_REQUEST_HEADERS), ""));

        this.responseHeadersToInject = buildHeadersMapFromString(
                readString(config.getInitParameter(INIT_PARAM_INJECT_RESPONSE_HEADERS), ""));

        LOGGER.debug("Servlet initialized: {\n\t{}:{},\n\t{}:{}\n}",
                INIT_PARAM_BASE_URI, this.baseUri
        );
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (HttpOptions.METHOD_NAME.equals(req.getMethod())) {
            resp.setStatus(200);
            this.responseHeadersToInject.forEach(resp::setHeader);
            LOGGER.debug("Sending headers headers with status 200");
            return;
        }

        try {
            this.makeProxyRequest(req, resp);
        } catch (IOException ioe) {
            LOGGER.error("Failed to make proxy request", ioe);
            resp.setStatus(404); // Return 404
        }
    }

    private HttpUriRequest getRequest(String method, String url) {
        if (HttpPost.METHOD_NAME.equals(method)) {
            return new HttpPost(url);
        } else if (HttpPut.METHOD_NAME.equals(method)) {
            return new HttpPut(url);
        } else if (HttpDelete.METHOD_NAME.equals(method)) {
            return new HttpDelete(url);
        } else if (HttpOptions.METHOD_NAME.equals(method)) {
            return new HttpOptions(url);
        } else if (HttpHead.METHOD_NAME.equals(method)) {
            return new HttpHead(url);
        }
        return new HttpGet(url);
    }

    private void makeProxyRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String thisServletPath = req.getServletPath();
        String query = req.getQueryString();
        String url = req.getRequestURI();

        // Build the target URL
        String targetUrl = this.baseUri + (url.substring(url.indexOf(thisServletPath) + thisServletPath.length()));
        targetUrl += "?" + query;

        LOGGER.debug("Proxying {}:{}", req.getMethod(), targetUrl);

        HttpUriRequest request = getRequest(req.getMethod(), targetUrl);

        // Inject response headers
        this.requestHeadersToInject.forEach(request::setHeader);
        // Proxy
        this.copyResponse(HttpClients.createDefault().execute(request), resp);
        // Inject response headers
        this.responseHeadersToInject.forEach(resp::setHeader);

    }

    private void copyResponse(CloseableHttpResponse fromResponse, HttpServletResponse toResponse) throws IOException {
        toResponse.setStatus(fromResponse.getStatusLine().getStatusCode());
        for (Header header : fromResponse.getAllHeaders()) {
            toResponse.setHeader(header.getName(), header.getValue());
        }
        HttpEntity entity = fromResponse.getEntity();
        IOUtils.copy(entity.getContent(), toResponse.getOutputStream());
    }

}

