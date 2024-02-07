package id.idtrust.billing.util;

import id.idtrust.billing.config.FilterIdConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@Data
@EqualsAndHashCode(callSuper=false)

public class LogSystem extends OncePerRequestFilter {

    private static String path;
    private static final Logger logger = LogManager.getLogger();

    private final String responseHeader;
    private final String mdcKey;
    private final String requestHeader;
    private static String logId;

    static Description ds = new Description();

    public static String getLogId()
    {
        return logId;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getPath()
    {
        return this.path;
    }
    public LogSystem()
    {
        responseHeader = FilterIdConfig.DEFAULT_HEADER_TOKEN;
        mdcKey = FilterIdConfig.DEFAULT_MDC;
        requestHeader = FilterIdConfig.DEFAULT_HEADER_TOKEN;
    }

    public LogSystem(final String responseHeader, final String mdcKey, final String requestHeader)
    {
        this.responseHeader = responseHeader;
        this.mdcKey = mdcKey;
        this.requestHeader = requestHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        try
        {
            final String token = extractToken(httpServletRequest);
            logId=token;
            MDC.put(mdcKey, token);
            if(StringUtils.hasText(responseHeader))
            {
                httpServletResponse.addHeader(responseHeader, token);
            }
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }catch(Exception e)
        {
            e.printStackTrace();
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }finally {
            MDC.remove(mdcKey);
        }
    }

    private String extractToken(final HttpServletRequest request)
    {
        final String token;

        token = UUID.randomUUID().toString().toUpperCase().replace("-", "");

        return token;
    }

    @Override
    protected boolean isAsyncDispatch(HttpServletRequest request) {
        return super.isAsyncDispatch(request);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return super.shouldNotFilterErrorDispatch();
    }

    public static void info(String message)
    {
        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + message);
    }

    public static void request(String message)
    {
        logger.info("["+ds.VERSION+"]-[BILLTRUST/REQUEST] : " + message);
    }

    public static void response(String message)
    {
        logger.info("["+ds.VERSION+"]-[BILLTRUST/RESPONSE] : " + message);
    }

    public static void error(String message)
    {
        logger.error("["+ds.VERSION+"]-[BILLTRUST/ERROR] : " + message);
    }
}
