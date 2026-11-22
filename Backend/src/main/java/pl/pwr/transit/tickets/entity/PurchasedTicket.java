package pl.pwr.transit.tickets.entity;

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
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.users.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "purchased_tickets")
@Getter
@Setter
@NoArgsConstructor
public class PurchasedTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_route_id")
    private Route validatedRoute;

    @Column(name = "validated_route_short_name")
    private String validatedRouteShortName;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
