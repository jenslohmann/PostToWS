package dk.jlo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/* FIXME Clean up this mess... Someday... Perhaps... */
public class PostToWS implements Runnable {

    private final String destinationUrl;
    private final int timeout;
    private final Logger logger;
    private final ExecutorService executorService;
    private BlockingQueue<String> bodies;
    private volatile boolean inputEnded = false;

    private PostToWS(String destinationUrl, int timeout) {
        this.destinationUrl = destinationUrl;
        this.timeout = timeout;
        this.logger = new Logger();
        bodies = new LinkedBlockingQueue<>(100);
        executorService = Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) throws Exception {
        final PostToWS postToWS = new PostToWS(args[0], 2000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));

        postToWS.executorService.execute(postToWS);
        postToWS.sendAll(reader);
    }

    private void sendAll(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty()) { // skip blank lines
                if (line.indexOf('<') > 0) {
                    String comment = line.substring(0, line.indexOf('<'));
                    logger.debug("<!-- " + comment + " -->");
                    line = line.substring(line.indexOf('<'));
                }
                while(!bodies.offer(line)) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }
        inputEnded = true;
    }

    @Override
    public void run() {
        while(!inputEnded || !bodies.isEmpty()) {
            try {
                String body = bodies.take();
                send(body);
            } catch (InterruptedException e) {
                //
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdownNow();
    }

    private void send(String body) throws IOException {
        final byte[] bodyBytes = body.getBytes();
        URL obj = new URL(destinationUrl);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setConnectTimeout(timeout);

        // request headers (add all headers needed)
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-length", String.valueOf(bodyBytes.length));
        connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        // Send post request
        connection.setDoOutput(true);
        connection.setReadTimeout(timeout);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(bodyBytes);
        outputStream.flush();
        logger.debug("Sending 'POST' request to URL : " + destinationUrl);
        int responseCode = connection.getResponseCode();
        logger.debug("responseCode:" + responseCode);
        InputStream in;
        if (responseCode >= 200 && responseCode < 300) {
            in = connection.getInputStream();
        } else {
            in = connection.getErrorStream();
        }
        logger.debug("Response Code : " + responseCode);
        Scanner s = new Scanner(in).useDelimiter("\\A");
        final String response = s.hasNext() ? s.next() : "";
        logger.debug(response);
        if (responseCode >= 300) {
            throw new IOException(response);
        }
        connection.disconnect();
    }

    class Logger {
        void debug(String toLog) {
//            System.out.println(toLog);
        }
    }
}
