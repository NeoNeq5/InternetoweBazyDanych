package pl.pwr.transit.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join entity for the many-to-many relation between {@link Route} and {@link Stop}.
 * Direction is part of the key because a stop can belong to both directions of a line.
 */
@Entity
@Table(name = "route_stops")
@IdClass(RouteStopId.class)
@Getter
@Setter
@NoArgsConstructor
public class RouteStop {

    @Id
    @Column(name = "route_id")
    private String routeId;

    @Id
    @Column(name = "stop_id")
    private String stopId;

    @Id
    @Column(name = "direction_id")
    private short directionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", insertable = false, updatable = false)
    private Stop stop;

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "headsign")
    private String headsign;
}
