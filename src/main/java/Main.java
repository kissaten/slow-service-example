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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    FutureTask<String> future =
        new FutureTask<String>(new Callable<String>() {
          public String call() {
            return serviceThatIsSlow();
          }});
    executor.execute(future);

    resp.addHeader("Transfer-Encoding", "chunked");
    resp.addHeader("Content-Type", "text/html");

    while (!future.isDone()) {
      try { Thread.sleep(10000); } catch (InterruptedException e) {}
      resp.getWriter().print(" ");
      resp.getWriter().flush();
    }

    try {
      resp.getWriter().print(future.get());
    } catch (Exception e) {
      resp.getWriter().print("<p>There was an error calling the service!</p>");
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
