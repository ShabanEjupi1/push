package mk.com.snt.kc.warehouse.view.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Forces HTTPS and sets the response headers that used to be added by the nginx
 * reverse proxy.
 *
 * <p>When nginx sat in front of GlassFish it did three things this application
 * never had to think about: redirect :80 to :443, add HSTS / anti-framing /
 * anti-sniffing headers, and mark the session cookie Secure. With GlassFish
 * terminating TLS itself (SERVE_MODE=glassfish in deploy.bat) nothing does that
 * any more, so it lives here. The cookie flags are the exception - those are set
 * by the container from WEB-INF/glassfish-web.xml, which can make "Secure"
 * conditional on the request in a way a filter cannot.
 *
 * <p>This deliberately does NOT use a {@code CONFIDENTIAL} security-constraint
 * in web.xml, which is the usual way to do this in Java EE. That mechanism
 * redirects on the container's view of the request, so behind a TLS-terminating
 * proxy - which forwards plain HTTP - it redirects every request forever. This
 * filter honours {@code X-Forwarded-Proto}, so the same build is correct whether
 * or not a proxy is in front of it.
 *
 * <p>Configured in web.xml; every parameter is optional:
 * <ul>
 *   <li>{@code redirectToHttps} - false turns the redirect off and leaves only
 *       the headers. Use it if TLS is ever moved back in front of the app.</li>
 *   <li>{@code httpsPort} - the port to redirect to. 443 is left out of the URL.</li>
 *   <li>{@code hstsMaxAge} - seconds; 0 disables the Strict-Transport-Security
 *       header. Note that a browser which has seen HSTS once will refuse plain
 *       HTTP to this host until it expires, so 0 does not undo it retroactively.</li>
 *   <li>{@code allowPlainLocalhost} - keep plain HTTP working for requests to
 *       localhost. This is what lets "http://127.0.0.1:8080/warehouse/" stay a
 *       usable answer to "is GlassFish itself serving, or is it the TLS layer
 *       that is broken?" - the first question worth asking when the site is
 *       down. Remote clients are unaffected.</li>
 * </ul>
 */
public class HttpsSecurityFilter implements Filter {

    private boolean redirectToHttps = true;
    private int httpsPort = 443;
    private long hstsMaxAge = 31536000L;
    private boolean allowPlainLocalhost = true;

    @Override
    public void init(FilterConfig config) throws ServletException {
        redirectToHttps = boolParam(config, "redirectToHttps", redirectToHttps);
        allowPlainLocalhost = boolParam(config, "allowPlainLocalhost", allowPlainLocalhost);
        httpsPort = (int) longParam(config, "httpsPort", httpsPort);
        hstsMaxAge = longParam(config, "hstsMaxAge", hstsMaxAge);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!isSecure(request) && redirectToHttps && !isLoopback(request)) {
            response.setHeader("Location", httpsUrlFor(request));
            // A 301 on a POST would arrive at the target as a GET with the body
            // thrown away - a form submit that silently loses its data. 307 keeps
            // the method and body. Only GET/HEAD are safe to permanently redirect.
            String method = request.getMethod();
            if ("GET".equals(method) || "HEAD".equals(method)) {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            } else {
                response.setStatus(307);
            }
            return;
        }

        addSecurityHeaders(request, response);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to release
    }

    /**
     * True when the browser's connection is encrypted - which is not the same as
     * this request arriving encrypted. Behind a TLS-terminating proxy the hop we
     * see is plain HTTP and only the forwarded header knows the difference.
     */
    private boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && "https".equalsIgnoreCase(proto.trim());
    }

    private boolean isLoopback(HttpServletRequest request) {
        if (!allowPlainLocalhost) {
            return false;
        }
        String name = request.getServerName();
        return "localhost".equalsIgnoreCase(name)
                || "127.0.0.1".equals(name)
                || "::1".equals(name)
                || "[::1]".equals(name);
    }

    private String httpsUrlFor(HttpServletRequest request) {
        StringBuilder url = new StringBuilder("https://");
        url.append(request.getServerName());
        if (httpsPort != 443) {
            url.append(':').append(httpsPort);
        }
        url.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null && query.length() > 0) {
            url.append('?').append(query);
        }
        return url.toString();
    }

    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        // HSTS over plain HTTP is ignored by browsers by specification, and
        // sending it there would only be misleading in a packet capture.
        if (hstsMaxAge > 0 && isSecure(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=" + hstsMaxAge);
        }
        // SAMEORIGIN rather than DENY: PrimeFaces uses iframes for file download
        // and print, and DENY breaks those.
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
    }

    private boolean boolParam(FilterConfig config, String name, boolean fallback) {
        String value = config.getInitParameter(name);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private long longParam(FilterConfig config, String name, long fallback) {
        String value = config.getInitParameter(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException notANumber) {
            // A typo here must not take the application down at startup; the
            // default is safe, so keep serving and leave a trace in server.log.
            config.getServletContext().log("HttpsSecurityFilter: init-param '" + name
                    + "' is not a number ('" + value + "'), using " + fallback);
            return fallback;
        }
    }
}
