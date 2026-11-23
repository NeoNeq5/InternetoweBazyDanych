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

@Entity
@Table(name = "route_shapes")
@IdClass(RouteShapeId.class)
@Getter
@Setter
@NoArgsConstructor
public class RouteShape {

    @Id
    @Column(name = "route_id")
    private String routeId;

    @Id
    @Column(name = "direction_id")
    private short directionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    private Route route;

    @Column(name = "points_json", nullable = false, columnDefinition = "text")
    private String pointsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_import_id")
    private GtfsImport lastImport;
}
