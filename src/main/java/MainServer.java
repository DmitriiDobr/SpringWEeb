import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class MainServer {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        final List<String> validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
                "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js"
        );



        Handler defaultHandler = (request,responseStream)->{
            try {
                final var filePath = Path.of(".", "public", request.getRequestLine().getPath());
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);

                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();

            }catch (IOException e){
                e.printStackTrace();
            }

        };


        for (String validPath : validPaths) {
            if (!validPath.equals("/classic.html")) {
                server.addHandler("GET", validPath, defaultHandler);
            }
        }
        Handler classicHtml = (request, responseStream) -> {
            try {
                final var filePath = Path.of(".", "public", "/classic.html");
                final var mimeType = Files.probeContentType(filePath);
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
            }catch (IOException e){
                e.printStackTrace();
            }
        };
        server.addHandler("GET","/classic.html",classicHtml);

        System.out.println(server.handlers);
        server.listen(9999);
    }



}
