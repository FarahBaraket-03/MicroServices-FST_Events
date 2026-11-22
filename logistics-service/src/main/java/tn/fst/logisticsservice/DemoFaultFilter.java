package tn.fst.logisticsservice;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.demo-faults-enabled", havingValue = "true")
public class DemoFaultFilter implements Filter {
  private final AtomicReference<String> mode = new AtomicReference<>("none");

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    if (req.getRequestURI().equals("/demo/fault")) {
      if (req.getMethod().equals("POST")) {
        String value = req.getParameter("mode");
        if (!java.util.Set.of("none", "slow", "invalid", "error")
            .contains(value == null ? "" : value)) {
          res.sendError(400);
          return;
        }
        mode.set(value);
      } else if (!req.getMethod().equals("GET")) {
        res.sendError(405);
        return;
      }
      res.setContentType("application/json");
      res.getWriter().write("{\"mode\":\"" + mode.get() + "\"}");
      return;
    }
    if (req.getRequestURI().startsWith("/logistics") && req.getMethod().equals("GET")) {
      switch (mode.get()) {
        case "slow":
          try {
            Thread.sleep(6000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
          }
          break;
        case "invalid":
          res.setContentType("application/json");
          res.getWriter().write("[{\"invalid\":true}]");
          return;
        case "error":
          res.sendError(503, "Injected dependency failure");
          return;
        default:
          break;
      }
    }
    chain.doFilter(request, response);
  }
}
