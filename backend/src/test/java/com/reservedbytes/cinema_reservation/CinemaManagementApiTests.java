package com.reservedbytes.cinema_reservation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import java.net.*; import java.net.http.*; import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CinemaManagementApiTests {
 @LocalServerPort int port; @Autowired ObjectMapper mapper;
 HttpResponse<String> request(String method,String path,String body)throws Exception{return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
 @Test void managementRestRoundTrip()throws Exception{
 var movie=request("POST","/movies","{\"title\":\"API movie\",\"genre\":\"Drama\",\"durationMinutes\":90,\"releaseYear\":2026,\"active\":true}");assertThat(movie.statusCode()).isEqualTo(201);long mid=mapper.readTree(movie.body()).get("id").asLong();
 var hall=request("POST","/halls","{\"name\":\"API hall\",\"active\":true}");assertThat(hall.statusCode()).isEqualTo(201);long hid=mapper.readTree(hall.body()).get("id").asLong();
 assertThat(request("GET","/movies",null).statusCode()).isEqualTo(200);assertThat(request("GET","/halls",null).statusCode()).isEqualTo(200);
 assertThat(request("PUT","/halls/"+hid+"/layout","{\"rows\":2,\"seatsPerRow\":3}").statusCode()).isEqualTo(200);
 String payload="{\"movieId\":"+mid+",\"hallId\":"+hid+",\"startsAt\":\""+Instant.now().plusSeconds(86400)+"\"}";
 var show=request("POST","/screenings",payload);assertThat(show.statusCode()).isEqualTo(201);long sid=mapper.readTree(show.body()).get("id").asLong();assertThat(mapper.readTree(show.body()).get("movie").get("title").asString()).isEqualTo("API movie");
 assertThat(request("POST","/screenings",payload).statusCode()).isEqualTo(409);
 assertThat(request("GET","/screenings?upcoming=true&movieId="+mid,null).statusCode()).isEqualTo(200);
 var seats=request("GET","/screenings/"+sid+"/availability",null);assertThat(seats.statusCode()).isEqualTo(200);assertThat(mapper.readTree(seats.body()).size()).isEqualTo(6);
 assertThat(request("DELETE","/movies/"+mid,null).statusCode()).isEqualTo(409);
 assertThat(request("POST","/screenings/"+sid+"/cancel",null).statusCode()).isEqualTo(200);
 }
}
