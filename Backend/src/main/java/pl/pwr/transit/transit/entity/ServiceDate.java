package pl.pwr.transit.transit.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_dates")
@Getter
@Setter
@NoArgsConstructor
public class ServiceDate {

    @EmbeddedId
    private ServiceDateId id;
}
