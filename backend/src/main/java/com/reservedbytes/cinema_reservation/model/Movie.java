package com.reservedbytes.cinema_reservation.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor public class Movie  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String title;
    @Column(length=5000) private String description;
    private String genre;
    private int durationMinutes;
    private int releaseYear;
    @Column(length=2000) private String posterUrl;
    private boolean active=true;
}
