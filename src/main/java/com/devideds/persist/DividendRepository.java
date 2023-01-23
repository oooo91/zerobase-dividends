package com.devideds.persist;

import com.devideds.persist.entity.DividendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface DividendRepository extends JpaRepository<DividendEntity, Long> {
    List<DividendEntity> findAllByCompanyId(Long id);

    @Transactional
    void deleteAllByCompanyId(Long id);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDateTime date);
}
