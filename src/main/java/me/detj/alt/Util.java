package me.detj.alt;

import lombok.experimental.UtilityClass;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@UtilityClass
public class Util {

    public static void handleException(HttpServletResponse response, Exception e) {
        try {
            e.printStackTrace(response.getWriter());
        } catch (IOException inner) {
            inner.printStackTrace();
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
