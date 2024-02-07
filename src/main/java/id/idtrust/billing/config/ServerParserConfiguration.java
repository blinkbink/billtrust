package id.idtrust.billing.config;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.http.HttpRequestParser;
import org.springframework.cloud.sleuth.http.HttpResponseParser;
import org.springframework.cloud.sleuth.instrument.web.HttpServerRequestParser;
import org.springframework.cloud.sleuth.instrument.web.HttpServerResponseParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration(proxyBeanMethods = false)
public class ServerParserConfiguration {

//    @Bean(name = HttpServerRequestParser.NAME)
//    HttpRequestParser myHttpRequestParser() {
//        return (request, context, span) -> {
//            // Span customization
//            span.tag("ServerRequest", "Tag");
//            Object unwrap = request.unwrap();
//            if (unwrap instanceof HttpServletRequest) {
//                HttpServletRequest req = (HttpServletRequest) unwrap;
//                // Span customization
//                span.tag("ServerRequestServlet", req.getMethod());
//            }
//        };
//    }
//
//    @Bean(name = HttpServerResponseParser.NAME)
//    HttpResponseParser myHttpResponseParser() {
//        return (response, context, span) -> {
//            // Span customization
//            span.tag("ServerResponse", "Tag");
//            Object unwrap = response.unwrap();
//            if (unwrap instanceof HttpServletResponse) {
//                HttpServletResponse resp = (HttpServletResponse) unwrap;
//                // Span customization
//                span.tag("ServerResponseServlet", String.valueOf(resp.getStatus()));
//            }
//        };
//    }

    @Bean
    Filter traceIdInResponseFilter(Tracer tracer) {
        return (request, response, chain) -> {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                HttpServletResponse resp = (HttpServletResponse) response;
                resp.addHeader("traceId", currentSpan.context().traceId());
            }
            chain.doFilter(request, response);
        };
    }


}

