package com.cris.sysmonitor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeightmentReadingRepository extends JpaRepository<WeightmentReading, Long> {
    List<WeightmentReading> findAllByOrderByReadingTimeDesc(Pageable pageable);
}
