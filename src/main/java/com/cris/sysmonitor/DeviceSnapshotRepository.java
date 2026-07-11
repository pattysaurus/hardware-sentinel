package com.cris.sysmonitor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceSnapshotRepository extends JpaRepository<DeviceSnapshot, Long> {
    List<DeviceSnapshot> findAllByOrderBySnapshotTimeDesc(Pageable pageable);
}