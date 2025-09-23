package com.tourya.api.services;

import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ReservationStatusEnum;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.TourReservation;
import com.tourya.api.models.TourReservationDetail;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.TourScheduleConfigPrice;
import com.tourya.api.models.User;
import com.tourya.api.models.request.ReservationItemRequest;
import com.tourya.api.models.request.ReservationRequest;
import com.tourya.api.models.responses.ReservationDetailResponse;
import com.tourya.api.models.responses.TourReservationResponse;
import com.tourya.api.repository.TourReservationRepository;
import com.tourya.api.repository.TourScheduleConfigPriceRepository;
import com.tourya.api.repository.TourScheduleRepository;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourReservationService {

    private final TourReservationRepository tourReservationRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigPriceRepository tourScheduleConfigPriceRepository;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE) // Apply lock to all queries in this transaction if applicable
    public TourReservationResponse createReservation(ReservationRequest request, User connectedUser) {

        // 1. Lock and validate the schedule
        TourSchedule schedule = tourScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour schedule with ID " + request.getScheduleId() + " not found."));

        if (schedule.getStatus() != TourScheduleStatusEnum.AVAILABLE) {
            throw new OperationNotPermittedException("This tour schedule is not available for reservation.");
        }

        // 2. Validate capacity
        int totalQuantityRequested = request.getItems().stream().mapToInt(ReservationItemRequest::getQuantity).sum();

        if (!schedule.getIsUnlimitedCapacity()) {
            int availableCapacity = schedule.getMaxCapacity() - schedule.getReservedCapacity();
            if (totalQuantityRequested > availableCapacity) {
                throw new OperationNotPermittedException("Not enough capacity available. Only " + availableCapacity + " spots left.");
            }
        }

        // 3. Validate prices and calculate total amount
        List<Integer> priceIds = request.getItems().stream().map(ReservationItemRequest::getPriceId).collect(Collectors.toList());
        Map<Integer, TourScheduleConfigPrice> pricesById = tourScheduleConfigPriceRepository.findAllById(priceIds).stream()
                .collect(Collectors.toMap(TourScheduleConfigPrice::getId, price -> price));

        if (priceIds.size() != pricesById.size()) {
            throw new ResourceNotFoundException("One or more price IDs are invalid.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReservationItemRequest item : request.getItems()) {
            TourScheduleConfigPrice price = pricesById.get(item.getPriceId());
            // Security check: ensure the price belongs to the tour's config
            if (!price.getSlot().getConfig().getId().equals(schedule.getConfig().getId())) {
                throw new OperationNotPermittedException("Price ID " + price.getId() + " does not belong to this tour.");
            }
            totalAmount = totalAmount.add(price.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. Create reservation and details
        TourReservation reservation = new TourReservation();
        reservation.setSchedule(schedule);
        reservation.setUser(connectedUser);
        reservation.setClientName(request.getClientName());
        reservation.setClientEmail(request.getClientEmail());
        reservation.setClientPhone(request.getClientPhone());
        reservation.setPaymentMethod(request.getPaymentMethod());
        reservation.setStatus(ReservationStatusEnum.PENDING); // Start as PENDING until payment is confirmed
        reservation.setTotalAmount(totalAmount);
        reservation.setCurrency(request.getCurrency().toUpperCase());

        List<TourReservationDetail> details = request.getItems().stream().map(item -> {
            TourScheduleConfigPrice price = pricesById.get(item.getPriceId());
            TourReservationDetail detail = new TourReservationDetail();
            detail.setReservation(reservation);
            detail.setPrice(price);
            detail.setQuantity(item.getQuantity());
            detail.setPriceAtReservation(price.getPrice()); // Store historical price
            detail.setSubtotal(price.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return detail;
        }).collect(Collectors.toList());

        reservation.setDetails(details);

        // 5. Update schedule's reserved capacity
        schedule.setReservedCapacity(schedule.getReservedCapacity() + totalQuantityRequested);

        // 6. Save the reservation (cascades to details)
        TourReservation savedReservation = tourReservationRepository.save(reservation);

        // 7. Return response DTO
        return mapToReservationResponse(savedReservation);
    }

    @Transactional(readOnly = true)
    public TourReservationResponse getReservationById(Integer reservationId, User connectedUser) {
        TourReservation reservation = tourReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + reservationId + " not found."));

        // Security Check: User must be the owner of the reservation or an admin
        // TODO: Add admin role check if needed
        if (!reservation.getUser().getId().equals(connectedUser.getId())) {
            throw new OperationNotPermittedException("You do not have permission to view this reservation.");
        }

        return mapToReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public PageResponse<TourReservationResponse> getAllReservationsForUser(User connectedUser, Pageable pageable) {
        Page<TourReservation> reservationPage = tourReservationRepository.findByUserId(connectedUser.getId(), pageable);
        Page<TourReservationResponse> responsePage = reservationPage.map(this::mapToReservationResponse);

        return new PageResponse<>(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public TourReservationResponse cancelReservation(Integer reservationId, User connectedUser) {
        // 1. Find the reservation
        TourReservation reservation = tourReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + reservationId + " not found."));

        // 2. Security Check: User must be the owner
        if (!reservation.getUser().getId().equals(connectedUser.getId())) {
            throw new OperationNotPermittedException("You do not have permission to cancel this reservation.");
        }

        // 3. Business Logic: Check current status
        if (reservation.getStatus() == ReservationStatusEnum.CANCELED || reservation.getStatus() == ReservationStatusEnum.COMPLETED) {
            throw new OperationNotPermittedException("This reservation cannot be canceled as it is already " + reservation.getStatus().name().toLowerCase() + ".");
        }

        // 4. Business Logic: Check cancellation window (e.g., cannot cancel within 24 hours of the tour)
        // Note: This is a simple rule. A real-world app might have more complex cancellation policies.
        TourSchedule schedule = reservation.getSchedule();
        if (schedule.getScheduleDate().isBefore(java.time.LocalDate.now().plusDays(1))) {
            throw new OperationNotPermittedException("Cancellation is not allowed within 24 hours of the tour date.");
        }

        // 5. Release the capacity
        if (!schedule.getIsUnlimitedCapacity()) {
            int totalQuantityToRelease = reservation.getDetails().stream()
                    .mapToInt(TourReservationDetail::getQuantity)
                    .sum();
            schedule.setReservedCapacity(schedule.getReservedCapacity() - totalQuantityToRelease);
            tourScheduleRepository.save(schedule);
        }

        // 6. Update reservation status
        reservation.setStatus(ReservationStatusEnum.CANCELED);
        TourReservation savedReservation = tourReservationRepository.save(reservation);

        // TODO: Here you would trigger the refund process

        // 7. Return the updated reservation
        return mapToReservationResponse(savedReservation);
    }

    private TourReservationResponse mapToReservationResponse(TourReservation reservation) {
        List<ReservationDetailResponse> detailResponses = reservation.getDetails().stream()
                .map(detail -> ReservationDetailResponse.builder()
                        .detailId(detail.getId())
                        .ageType(detail.getPrice().getAgeType())
                        .quantity(detail.getQuantity())
                        .priceAtReservation(detail.getPriceAtReservation())
                        .subtotal(detail.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return TourReservationResponse.builder()
                .reservationId(reservation.getId().longValue())
                .scheduleId(reservation.getSchedule().getId().longValue())
                .tourId(reservation.getSchedule().getTour().getId().longValue())
                .tourName(reservation.getSchedule().getTour().getName())
                .status(reservation.getStatus().name())
                .totalAmount(reservation.getTotalAmount())
                .currency(reservation.getCurrency())
                .createdDate(reservation.getCreatedDate().atZone(ZoneId.systemDefault()))
                .details(detailResponses)
                .build();
    }
}
