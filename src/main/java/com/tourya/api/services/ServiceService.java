package com.tourya.api.services;


import com.tourya.api.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceService {
    private ServiceRepository serviceRepository;


}
