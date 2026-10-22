package com.reservedbytes.cinema_reservation.service;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import com.reservedbytes.cinema_reservation.model.Seat;
@Component @Profile("dev") public class DemoFixtures implements CommandLineRunner  {
    private final CinemaManagement c;
    private final Clock clock;
    public DemoFixtures(CinemaManagement c,Clock clock) {
        this.c=c;
        this.clock=clock;
    }
    @Transactional public void run(String...args) {
        for(int i=0;i<3;i++) {
            var m=c.saveMovie(null,new CinemaManagement.MovieInput(new String[] {
                "Beyond the Silence","The Last Light","After Hours"
            }
            [i],"A Deadlock cinema presentation.",new String[] {
                "Science fiction","Drama","Thriller"
            }
            [i],110+i*5,2026,"",true));
            if(i<2) {
                var h=c.saveHall(null,new CinemaManagement.HallInput(i==0?"Hall A":"Hall B",true));
                var seats=c.layout(h.getId(),new CinemaManagement.LayoutInput(8,10));
                for(var s:seats)if(s.getRowNumber()>=7)s.configure(Seat.SeatType.VIP,true);
                for(int day=1;day<=3;day++)c.saveScreening(null,new CinemaManagement.ScreeningInput(m.getId(),h.getId(),clock.instant().plusSeconds(day*86400L+i*7200L)));
            }
        }
    }
}
