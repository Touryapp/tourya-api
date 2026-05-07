package com.tourya.api.jobs;

import com.tourya.api.constans.enums.AccountPayableStatusEnum;
import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import com.tourya.api.models.AccountPayable;
import com.tourya.api.models.ProviderPayoutOrder;
import com.tourya.api.models.ProviderPayoutOrderReservation;
import com.tourya.api.repository.AccountPayableRepository;
import com.tourya.api.repository.ProviderPayoutOrderRepository;
import com.tourya.api.repository.ProviderPayoutOrderReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderPayoutOrderJob {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private final AccountPayableRepository accountPayableRepository;
    private final ProviderPayoutOrderRepository payoutOrderRepository;
    private final ProviderPayoutOrderReservationRepository payoutOrderReservationRepository;

    /**
     * Lunes y Jueves a las 7:00am (UTC): crea órdenes para pagar al día siguiente (martes/viernes).
     * - Lunes: paga servicios ejecutados jueves-domingo.
     * - Jueves: paga servicios ejecutados lunes-miércoles.
     *
     * Además exige que la reserva ya esté disponible para pago: payout_available_date <= payDate (reservation_date + 2 días).
     */
    @Scheduled(cron = "0 0 7 ? * MON,THU", zone = "America/Bogota")
    @Transactional
    public void createProviderPayoutOrders() {
        LocalDate today = LocalDate.now(ZONE);
        DayOfWeek dow = today.getDayOfWeek();

        if (dow != DayOfWeek.MONDAY && dow != DayOfWeek.THURSDAY) return;

        LocalDate payDate = today.plusDays(1); // martes o viernes

        DateRange executionRange = (dow == DayOfWeek.MONDAY)
                ? executionRangeThursdayToSunday(today)
                : executionRangeMondayToWednesday(today);

        List<AccountPayable> candidates = accountPayableRepository.findEligibleForPayoutOrder(
                AccountPayableStatusEnum.PENDING,
                payDate,
                executionRange.start,
                executionRange.end
        );

        // Excluir reservas ya asociadas
        List<AccountPayable> eligible = candidates.stream()
                .filter(ap -> !payoutOrderReservationRepository.existsByReservationId(ap.getReservationId()))
                .toList();

        if (eligible.isEmpty()) {
            log.info("ProviderPayoutOrderJob: no hay cuentas por pagar elegibles. today={}, payDate={}, range={}..{}",
                    today, payDate, executionRange.start, executionRange.end);
            return;
        }

        Map<Integer, List<AccountPayable>> byProvider = eligible.stream()
                .collect(Collectors.groupingBy(AccountPayable::getProviderId));

        int createdOrders = 0;
        int linkedReservations = 0;

        for (Map.Entry<Integer, List<AccountPayable>> entry : byProvider.entrySet()) {
            Integer providerId = entry.getKey();
            List<AccountPayable> items = entry.getValue().stream()
                    .sorted(Comparator.comparing(AccountPayable::getReservationId))
                    .toList();

            BigDecimal total = items.stream()
                    .map(AccountPayable::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ProviderPayoutOrder order = ProviderPayoutOrder.builder()
                    .providerId(providerId)
                    .createdAt(OffsetDateTime.now(ZONE))
                    .payDate(payDate)
                    .status(ProviderPayoutOrderStatusEnum.PENDING)
                    .amountTotal(total)
                    .build();

            order = payoutOrderRepository.save(order);
            createdOrders++;

            for (AccountPayable ap : items) {
                ProviderPayoutOrderReservation link = new ProviderPayoutOrderReservation();
                link.setPayoutOrderId(order.getId());
                link.setReservationId(ap.getReservationId());
                link.setAccountPayableId(ap.getId());
                link.setAmount(ap.getAmount() != null ? ap.getAmount() : BigDecimal.ZERO);
                payoutOrderReservationRepository.save(link);
                linkedReservations++;
            }
        }

        log.info("ProviderPayoutOrderJob: creadas {} órdenes, asociadas {} reservas. today={}, payDate={}, range={}..{}",
                createdOrders, linkedReservations, today, payDate, executionRange.start, executionRange.end);
    }

    private static DateRange executionRangeThursdayToSunday(LocalDate monday) {
        // “los martes se pagarán los servicios que se ejecutaron de jueves a domingo”
        // El cron corre el lunes, así que usamos la semana anterior: Jueves(-4) .. Domingo(-1)
        LocalDate start = monday.minusDays(4); // jueves anterior
        LocalDate end = monday.minusDays(1);   // domingo anterior
        return new DateRange(start, end);
    }

    private static DateRange executionRangeMondayToWednesday(LocalDate thursday) {
        // “los viernes se pagarán los servicios que se ejecutaron del lunes al miércoles”
        // El cron corre el jueves, así que es la misma semana: Lunes(-3) .. Miércoles(-1)
        LocalDate start = thursday.minusDays(3); // lunes
        LocalDate end = thursday.minusDays(1);   // miércoles
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}

