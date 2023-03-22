import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Request {

    private RequestLine requestLine;
    private List<String> headers;
    private String body;
    private List<NameValuePair> queryParam;

    public Request(RequestLine requestLine, List<String> headers){
        this.requestLine=requestLine;
        this.headers=headers;
    }
    public Request(RequestLine requestLine, List<String> headers,String body){
        this.requestLine=requestLine;
        this.headers=headers;
        this.body=body;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public RequestLine getRequestLine() {
        return requestLine;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public  String getQueryParam(String name){
        return this.queryParam.stream().filter(o->o.getName().equals(name)).findFirst().get().getValue();
    }

    public void setQueryParam(List<NameValuePair> queryParam){
        this.queryParam=queryParam;
    }

    public  List<NameValuePair> getQueryParams() throws URISyntaxException {
        final URI uri = new URI(requestLine.getPath());
        return URLEncodedUtils.parse(uri, String.valueOf(StandardCharsets.UTF_8));
    }


    @Override
    public String toString() {
        return "Request{" +
                "requestLine=" + requestLine +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }
}
