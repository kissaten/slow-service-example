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

  @Override
  protected void doGet(HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    response.addHeader("Transfer-Encoding", "chunked");
    response.addHeader("Content-Type", "text/html");

    final AsyncContext async = request.startAsync();
    async.setTimeout(90000);

    async.addListener(new AsyncListener() {
      @Override
      public void onComplete(AsyncEvent event) throws IOException {
        System.out.println("Async complete");
      }

      @Override
      public void onError(AsyncEvent event) {
        System.out.println("Async error");
        System.out.println(event.getThrowable());
      }

      @Override
      public void onStartAsync(AsyncEvent event) {
        System.out.println("Async starting");
      }

      @Override
      public void onTimeout(AsyncEvent event) {
        System.out.println("Async timeout");
      }
    });

    final ScheduledFuture chunkBlower = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
      public void run() {
        try {
          async.getResponse().getWriter().print(" ");
          async.getResponse().getWriter().flush();
        } catch (IOException e) {
          // do nothing
        }
      }
    }, 15, 15, TimeUnit.SECONDS);

    Executors.newSingleThreadExecutor().execute(new Runnable() {
      public void run() {
        String responseString = "";
        try {
          responseString = serviceThatIsSlow();
        } catch (Exception e) {
          responseString = "<p>There was an error calling the service!</p>";
        } finally {
          chunkBlower.cancel(true);
          try {
            async.getResponse().getWriter().print(responseString);
            async.getResponse().getWriter().flush();
          } catch (IOException e) {
            // do nothing
          }
          async.complete();
        }
      }
    });
  }

  protected String serviceThatIsSlow() {
    try { Thread.sleep(50000); } catch (InterruptedException e) { /* do nothing */ }
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
