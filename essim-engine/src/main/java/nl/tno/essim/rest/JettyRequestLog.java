package nl.tno.essim.rest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

@Slf4j
public class JettyRequestLog extends CustomRequestLog {
    @Override
    public void log(Request request, Response response) {
        super.log(request, response);
        log.debug("{} {} {} from {} --> {} ({})",
                request.getMethod(),
                request.getOriginalURI(),
                request.getContentType() == null ? "" : "(" + request.getContentType() + ")",
                request.getRemoteInetSocketAddress(),
                response.getStatus(),
                response.getContentType());
    }
}
