package com.tourya.api.services;

import com.tourya.api.models.responses.TourTagResponse;
import com.tourya.api.repository.TourTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourTagService {

    private final TourTagRepository repository;

    public List<TourTagResponse> getAllTags() {
        return repository.getAllTourTags()
                .stream()
                .map(row -> new TourTagResponse(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3]
                ))
                .collect(Collectors.toList());
    }
}
