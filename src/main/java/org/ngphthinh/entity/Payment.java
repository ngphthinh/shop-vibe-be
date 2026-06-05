package org.ngphthinh.entity;

import jakarta.persistence.*;
import lombok.*;
import org.ngphthinh.enums.PaymentStatus;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 50)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column( unique = true, length = 100)
    private String transactionId;

    @Column( nullable = false, updatable = false)
    private LocalDateTime createdAt;
}