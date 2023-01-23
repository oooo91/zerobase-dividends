package com.devideds.persist.entity;

import com.devideds.model.Dividend;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "DIVIDEND")
@Data
@ToString
@NoArgsConstructor
@Table(
        uniqueConstraints = { //복합 uniqeKey
            @UniqueConstraint(
                columnNames = {"companyId", "date"}
        )
    }
)
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private LocalDateTime date;

    private String dividend;

    public DividendEntity(Long companyId, Dividend dividend) {
        this.companyId = companyId;
        this.date = dividend.getDate();
        this.dividend = dividend.getDividend();
    }
}
