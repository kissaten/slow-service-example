import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.*;

public class Main extends HttpServlet {

  private ExecutorService executor = Executors.newFixedThreadPool(10);

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  @Override
  protected void doGet(HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    FutureTask<String> service =
        new FutureTask<>(new Callable<String>() {
          public String call() {
            return serviceThatIsSlow();
          }});
    executor.execute(service);

    ScheduledFuture chunks = scheduler.scheduleAtFixedRate(new Runnable() {
        public void run() {
          try {
            resp.getWriter().print(" ");
            resp.getWriter().flush();
          } catch (IOException e) {
            // do nothing
          }
        }
      }, 10, 10, TimeUnit.SECONDS);

    resp.addHeader("Transfer-Encoding", "chunked");
    resp.addHeader("Content-Type", "text/html");

    String responseString = "";
    try {
      responseString = service.get();
    } catch (Exception e) {
      responseString = "<p>There was an error calling the service!</p>";
    } finally {
      chunks.cancel(true);
      resp.getWriter().print(responseString);
    }
  }

  protected String serviceThatIsSlow() {
    try { Thread.sleep(50000); } catch (InterruptedException e) {}
    return "<html><body><p>This is an important message</p></body></html>";
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
