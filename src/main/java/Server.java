import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class Server {

    public final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();


    public void listen(int port) throws IOException {

        ExecutorService threadPool = Executors.newFixedThreadPool(64);

        try (final var ServeSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = ServeSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                threadPool.execute(client);
            }
        } finally {
            threadPool.shutdown();
        }
    }

    public void addHandler(String method, String path, Handler handler){
        Map<String, Handler> map = new ConcurrentHashMap<>();
        if (handlers.containsKey(method)) {
            map = handlers.get(method);
        }
        map.put(path, handler);
        handlers.put(method, map);
    }

    private class ClientHandler extends Thread {
        private final BufferedInputStream in;
        private final BufferedOutputStream out;

        public ClientHandler(Socket socket) throws IOException {
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try{
                // лимит на request line + заголовки
                final int limit = 4096;

                in.mark(limit);
                final byte[] buffer = new byte[limit];
                final int read = in.read(buffer);

                // ищем request line
                final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
                final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    return;
                }

                // читаем request line
                final String[] parts = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (parts.length != 3) {
                    badRequest(out);
                    return;
                }

                // проверяем, валидный ли путь
                if (!parts[1].startsWith("/")) {
                    badRequest(out);
                    return;
                }

                // получили request line
                RequestLine requestLine = new RequestLine(parts[0], parts[1], parts[2]);
                // ищем заголовки
                final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final int headersStart = requestLineEnd + requestLineDelimiter.length;
                final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    return;
                }

                // отматываем на начало буфера
                in.reset();

                // пропускаем requestLine
                in.skip(headersStart);

                // получили заголовки
                final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
                final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

                // получили запрос, теперь проверим, есть ли у запроса тело
                Request request = new Request(requestLine, headers);
                //добавили query params
                request.setQueryParam(request.getQueryParams());
                System.out.printf("Параметры запроса: %s.\n",request.getQueryParams());
                System.out.printf("Значение параметра title: %s.",request.getQueryParam("title"));

                // для GET тело МОЖЕТ быть, но общепринято его игнорировать
                if (!requestLine.getMethod().equals("GET")) {
                    in.skip(headersDelimiter.length);
                    // вычитываем Content-Length, чтобы прочитать body
                    final Optional<String> contentLength = extractHeader(headers, "Content-Length");
                    if (contentLength.isPresent()) {
                        final int length = Integer.parseInt(contentLength.get());
                        final byte[] bodyBytes = in.readNBytes(length);

                        final String body = new String(bodyBytes);
                        request.setBody(body);
                    }
                }

                // получили handler
                Handler handler = handlers.get(request.getRequestLine().getMethod())
                        .get(request.getRequestLine().getPath());
                if (handler==null){
                    badRequest(out);
                    return;
                }
                handler.handle(request, out);

            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
    public static void getSetBody(BufferedInputStream in,String method,RequestLine requestLine){

    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

}


