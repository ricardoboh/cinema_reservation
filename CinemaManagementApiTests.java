package com.reservedbytes.cinema_reservation;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CinemaManagementApiTests {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    HttpResponse<String> request(String method,String path,String body)throws Exception {
        try(var client=HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
                .header("Content-Type","application/json")
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test void managementPublishesImmutableScreeningAndReturnsOnlyViews()throws Exception {
        var movie=request("POST","/movies","{\"title\":\"API movie\"}");
        assertThat(movie.statusCode()).isEqualTo(201);
        var movieJson=mapper.readTree(movie.body());
        assertThat(movieJson.size()).isEqualTo(2);
        long mid=movieJson.get("id").asLong();
        var hall=request("POST","/halls","{\"name\":\"API hall\"}");
        assertThat(hall.statusCode()).isEqualTo(201);
        long hid=mapper.readTree(hall.body()).get("id").asLong();
        var seats=request("PUT","/halls/"+hid+"/layout","{\"rows\":1,\"seatsPerRow\":3}");
        assertThat(seats.statusCode()).isEqualTo(200);
        long seatId=mapper.readTree(seats.body()).get(0).get("id").asLong();
        assertThat(request("PUT","/halls/"+hid+"/seats/"+seatId,"{\"seatType\":\"VIP\"}").statusCode()).isEqualTo(200);
        String input="{\"movieId\":"+mid+",\"hallId\":"+hid+",\"startsAt\":\""+Instant.now().plusSeconds(7200)+"\"}";
        var screening=request("POST","/screenings",input);
        assertThat(screening.statusCode()).isEqualTo(201);
        long sid=mapper.readTree(screening.body()).get("id").asLong();
        assertThat(mapper.readTree(screening.body()).has("endsAt")).isFalse();
        var storedBeforeEdit=request("GET","/screenings/"+sid,null).body();
        assertThat(request("PUT","/screenings/"+sid,input).statusCode()).isEqualTo(405);
        assertThat(request("PUT","/halls/"+hid+"/layout","{\"rows\":2,\"seatsPerRow\":2}").statusCode()).isEqualTo(409);
        assertThat(request("GET","/screenings/"+sid,null).body()).isEqualTo(storedBeforeEdit);
        var availability=mapper.readTree(request("GET","/screenings/"+sid+"/availability",null).body());
        assertThat(availability.size()).isEqualTo(3);
        for(var seat:availability){
            assertThat(seat.size()).isEqualTo(5);
            assertThat(seat.has("status")).isTrue();
            assertThat(seat.has("availability")).isFalse();
            assertThat(seat.get("status").asText()).isEqualTo("AVAILABLE");
        }
        assertThat(request("DELETE","/movies/"+mid,null).statusCode()).isEqualTo(409);
        assertThat(request("GET","/movies/"+mid,null).statusCode()).isEqualTo(200);
        assertThat(request("GET","/halls/"+hid+"/seats",null).statusCode()).isEqualTo(200);
        assertThat(request("GET","/screenings?upcoming=true&movieId="+mid,null).statusCode()).isEqualTo(200);
    }
    @Test void unspecifiedLifecycleExtensionsAreAbsentFromOpenApi()throws Exception {
        var paths=mapper.readTree(request("GET","/v3/api-docs",null).body()).get("paths");
        assertThat(paths.has("/movies/{id}/deactivate")).isFalse();
        assertThat(paths.has("/screenings/{id}/cancel")).isFalse();
        assertThat(paths.get("/screenings/{id}").has("put")).isFalse();
        assertThat(request("GET","/screenings/999999",null).statusCode()).isEqualTo(404);
        assertThat(request("GET","/halls/999999/seats",null).statusCode()).isEqualTo(404);
    }
}
