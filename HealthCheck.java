import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class HealthCheck {

    private static final int STATUS_OK = 200;

    public static void main(String[] args)
        throws InterruptedException, IOException {

        var client = HttpClient.newHttpClient();
        var request = HttpRequest
            .newBuilder()
            .uri(URI.create("http://localhost:8080/actuator/health"))
            .header("accept", "application/json")
            .build();
        var response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != STATUS_OK || !response
            .body()
            .contains("UP")) {
            throw new RuntimeException("Healthcheck failed");
        }
    }
}
