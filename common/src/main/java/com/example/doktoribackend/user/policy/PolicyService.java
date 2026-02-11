package com.example.doktoribackend.user.policy;

import com.example.doktoribackend.reading.repository.ReadingGenreRepository;
import com.example.doktoribackend.user.policy.dto.ReadingPolicyResponse;
import com.example.doktoribackend.user.repository.ReadingPurposeRepository;
import com.example.doktoribackend.user.repository.ReadingVolumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final ReadingVolumeRepository readingVolumeRepository;
    private final ReadingPurposeRepository readingPurposeRepository;
    private final ReadingGenreRepository readingGenreRepository;

    @Transactional(readOnly = true)
    public List<ReadingPolicyResponse> getReadingVolumePolicies() {
        return readingVolumeRepository.findAllByDeletedAtIsNullOrderByPriorityAsc().stream()
                .map(volume -> new ReadingPolicyResponse(
                        volume.getId(),
                        volume.getCode(),
                        volume.getName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingPolicyResponse> getReadingPurposePolicies() {
        return readingPurposeRepository.findAllByDeletedAtIsNullOrderByPriorityAsc().stream()
                .map(purpose -> new ReadingPolicyResponse(
                        purpose.getId(),
                        purpose.getCode(),
                        purpose.getName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadingPolicyResponse> getReadingGenrePolicies() {
        return readingGenreRepository.findAllByDeletedAtIsNullOrderByPriorityAsc().stream()
                .map(genre -> new ReadingPolicyResponse(
                        genre.getId(),
                        genre.getCode(),
                        genre.getName()
                ))
                .toList();
    }
}
