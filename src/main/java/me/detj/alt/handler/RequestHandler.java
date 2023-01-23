package me.detj.alt.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Objects;

@AllArgsConstructor
public class RequestHandler extends AbstractHandler {

    private final LoginHandler loginHandler;
    private final AuthHandler authHandler;

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        baseRequest.setHandled(true);
        response.setContentType("text/plain");
        if (Objects.equals(request.getHeader("X-IS-LOGIN"), "true")
                && Objects.equals(request.getMethod(), "POST")) {
            loginHandler.handleLogin(request, response);
        } else {
            authHandler.handle(request, response);
        }
    }
}
