import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.*;

public class Main extends HttpServlet {

  private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(100);

  private ExecutorService executor = Executors.newFixedThreadPool(100);

  @Override
  protected void doGet(HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    Long start = System.currentTimeMillis();

    response.addHeader("Transfer-Encoding", "chunked");
    response.addHeader("Content-Type", "text/html");

    final AsyncContext async = request.startAsync();
    async.setTimeout(90000);

    final ScheduledFuture chunkBlower = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
      public void run() {
        try {
          async.getResponse().getWriter().print(" ");
          async.getResponse().getWriter().flush();
        } catch (IOException e) {
          // do nothing
        }
      }
    }, 15, 15, TimeUnit.SECONDS);

    executor.execute(new Runnable() {
      public void run() {
        Long start = System.currentTimeMillis();
        String responseString = "";
        try {
          responseString = serviceThatIsSlow();
        } catch (Exception e) {
          responseString = "<p>There was an error calling the service!</p>";
        } finally {
          chunkBlower.cancel(true);
          try {
            response.getWriter().println("<p>Time spent async: " + (System.currentTimeMillis() - start) + "ms</p>");
            async.getResponse().getWriter().print(responseString);
            async.getResponse().getWriter().flush();
          } catch (IOException e) {
            // do nothing
          }
          async.complete();
        }
      }
    });

    response.getWriter().println("<p>Time spent blocking: " + (System.currentTimeMillis() - start) + "ms</p>");
    response.flushBuffer();
  }

  protected String serviceThatIsSlow() {
    try { Thread.sleep(50000); } catch (InterruptedException e) { /* do nothing */ }
    return "<p>This is an important message</p>";
  }

  public static void main(String[] args) throws Exception{
    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }
}
