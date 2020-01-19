package com.clhost.memes.app.tree;

import feign.Logger;
import feign.Request;
import org.apache.logging.log4j.LogManager;
import org.slf4j.MDC;

import static feign.Util.valuesOrEmpty;

public class FeignLogger extends Logger {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(FeignLogger.class);

    @Override
    protected void log(String configKey, String format, Object... args) {
        LOGGER.debug(format(configKey, format, args));
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        log(configKey, "---> %s %s CID %s HTTP/1.1", request.httpMethod().name(), request.url(), MDC.get("cid"));
        if (logLevel.ordinal() >= Level.HEADERS.ordinal()) {

            for (String field : request.headers().keySet()) {
                for (String value : valuesOrEmpty(request.headers(), field)) {
                    log(configKey, "%s: %s", field, value);
                }
            }

            int bodyLength = 0;
            if (request.requestBody().asBytes() != null) {
                bodyLength = request.requestBody().asBytes().length;
                if (logLevel.ordinal() >= Level.FULL.ordinal()) {
                    String bodyText =
                            request.charset() != null
                                    ? new String(request.requestBody().asBytes(), request.charset())
                                    : null;
                    log(configKey, ""); // CRLF
                    log(configKey, "%s", bodyText != null ? bodyText : "Binary data");
                }
            }
            log(configKey, "---> END HTTP (%s-byte body)", bodyLength);
        }
    }

    protected String format(String configKey, String format, Object... args) {
        return String.format(methodTag(configKey) + format, args);
    }
}
