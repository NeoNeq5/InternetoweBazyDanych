package pl.pwr.transit.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ServiceDateId implements Serializable {

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;
}
