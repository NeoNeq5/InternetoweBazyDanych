package pl.pwr.transit.tickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pl.pwr.transit.users.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "penalties")
@Getter
@Setter
@NoArgsConstructor
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "penalty_number", nullable = false, unique = true)
    private String penaltyNumber;

    @Column(name = "offender_full_name")
    private String offenderFullName;

    @Column(name = "offender_document_id")
    private String offenderDocumentId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PenaltyStatus status;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
}
