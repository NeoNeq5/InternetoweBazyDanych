package pl.pwr.transit.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;

@Entity
@Table(name = "stop_times")
@Getter
@Setter
@NoArgsConstructor
public class StopTime {

    @EmbeddedId
    private StopTimeId id;

    @MapsId("tripId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "arrival_time")
    private Duration arrivalTime;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "departure_time")
    private Duration departureTime;
}
