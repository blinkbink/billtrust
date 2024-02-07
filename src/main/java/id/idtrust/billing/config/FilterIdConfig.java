package id.idtrust.billing.config;


import id.idtrust.billing.util.LogSystem;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

public class FilterIdConfig {

    public static final String DEFAULT_HEADER_TOKEN = "tokenId";
    public static final String DEFAULT_MDC = "tokenId";

    public String responseHeader = DEFAULT_HEADER_TOKEN;
    public String mdcKey = DEFAULT_MDC;
    public String requestHeader = DEFAULT_HEADER_TOKEN;

    @Bean
    public FilterRegistrationBean<LogSystem> servletRegistrationBean()
    {
        final FilterRegistrationBean<LogSystem> registrationBean = new FilterRegistrationBean();
        final LogSystem log4jMDCFilter = new LogSystem(responseHeader, mdcKey, requestHeader);
        registrationBean.setFilter(log4jMDCFilter);
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
