import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.sql.*;
import java.util.concurrent.*;

import org.apache.commons.dbcp2.*;

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

    while (!future.isDone()) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      resp.getWriter().print("X");
    }

    try {
      resp.getWriter().print(future.get());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  protected String serviceThatIsSlow() {
    try {
      Thread.sleep(50000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "This is an important message";
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
