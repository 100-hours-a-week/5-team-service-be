package com.example.doktoribackend.vote.repository;

import com.example.doktoribackend.vote.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}
