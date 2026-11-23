package pl.pwr.transit.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "vehicle_positions")
@Getter
@Setter
@NoArgsConstructor
public class VehiclePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long id;

    @Column(name = "mpk_course_id", nullable = false)
    private Long mpkCourseId;

    @Column(name = "line", nullable = false)
    private String line;

    @Column(name = "vehicle_type_code", nullable = false)
    private String vehicleTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_code", insertable = false, updatable = false)
    private VehicleTypeEntity vehicleTypeRef;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public VehicleType getVehicleType() {
        return VehicleType.fromCode(vehicleTypeCode);
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleTypeCode = vehicleType.getCode();
    }
}
