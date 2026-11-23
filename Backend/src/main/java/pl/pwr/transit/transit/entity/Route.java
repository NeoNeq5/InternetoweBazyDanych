package pl.pwr.transit.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
public class Route {

    @Id
    @Column(name = "route_id")
    private String id;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @Column(name = "long_name")
    private String longName;

    @Column(name = "vehicle_type_code", nullable = false)
    private String vehicleTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_code", insertable = false, updatable = false)
    private VehicleTypeEntity vehicleTypeRef;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_import_id", nullable = false)
    private GtfsImport lastImport;

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    private Set<RouteStop> stopLinks = new HashSet<>();

    public VehicleType getVehicleType() {
        return VehicleType.fromCode(vehicleTypeCode);
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleTypeCode = vehicleType.getCode();
    }
}
