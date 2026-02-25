package com.example.doktoribackend.vote.repository;

import com.example.doktoribackend.vote.domain.VoteCast;
import com.example.doktoribackend.vote.domain.VoteCastId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteCastRepository extends JpaRepository<VoteCast, VoteCastId> {
}
