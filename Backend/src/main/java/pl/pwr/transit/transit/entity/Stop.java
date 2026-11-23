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
@Table(name = "stops")
@Getter
@Setter
@NoArgsConstructor
public class Stop {

    @Id
    @Column(name = "stop_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_station")
    private Stop parentStation;

    @Column(name = "stop_name", nullable = false)
    private String name;

    @Column(name = "stop_lat", nullable = false)
    private double lat;

    @Column(name = "stop_lon", nullable = false)
    private double lon;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_import_id", nullable = false)
    private GtfsImport lastImport;

    @OneToMany(mappedBy = "stop", fetch = FetchType.LAZY)
    private Set<RouteStop> routeLinks = new HashSet<>();
}
