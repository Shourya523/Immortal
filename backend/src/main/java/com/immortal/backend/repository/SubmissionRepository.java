package com.immortal.backend.repository;

import com.immortal.backend.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByMatchId(Long matchId);
    List<Submission> findByUserId(Long userId);
}
