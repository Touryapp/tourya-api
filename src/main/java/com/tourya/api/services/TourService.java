package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.UserRoleType;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.*;
import com.tourya.api.models.request.*;
import com.tourya.api.models.responses.*;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {
    private final TourRepository tourRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourFaqRepository tourFaqRepository;
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final TourItineraryRepository tourItineraryRepository;
    private final TourGalleryRepository tourGalleryRepository;
    private final TourTagsRepository tourTagsRepository;
    private final ReviewRepository reviewRepository;
    private final TourCategoryService tourCategoryService;
    private final ProviderService providerService;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;
    private final TourMapper tourMapper;
    private final TourMainAttractionMapper tourMainAttractionMapper;
    private final TourIncludesExcludesMapper tourIncludesExcludesMapper;
    private final TourAddressMapper tourAddressMapper;
    private final TourFaqMapper tourFaqMapper;
    private final TourItineraryMapper tourItineraryMapper;
    private final TourCancellationPolicyMapper tourCancellationPolicyMapper;
    private final TourGalleryMapper tourGalleryMapper;


    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    /** Copia defensiva para que Hibernate detecte cambios en columnas JSON mapeadas con {@code AttributeConverter}. */
    private static TranslatedField copyTranslatedField(TranslatedField src) {
        if (src == null) {
            return null;
        }
        return new TranslatedField(src.getEs(), src.getEn(), src.getPt());
    }

    private TourCategory getTourCategory(Integer id){
        TourCategory tourCategory = tourCategoryService.findById(id);
        if(tourCategory != null){
            return tourCategory;
        }else{
            throw new ResourceNotFoundException("No tourCategory found for the id= " +id);
        }
    }

    public PageResponse<TourResponse> findAllByUser(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Provider provider = providerService.findByUserAndStatusActive(user);
            Page<Tour> allTours = tourRepository.findAllByProviderId(provider.getId(), pageable);

            List<TourResponse> toursResponse = allTours.stream()
                    .map(tourMapper::toTourResponse)
                    .toList();

            for(TourResponse tourResponse :toursResponse){
                List<TourGallery> tourGalleries = tourGalleryRepository.findByTourIdAndOrderIndex(tourResponse.getId(), 1);
                if(!tourGalleries.isEmpty()){
                    tourResponse.setProfilePicture(tourGalleryMapper.toTourGalleryResponse(tourGalleries.get(0)));
                }
            }


            return new PageResponse<>(
                    toursResponse,
                    allTours.getNumber(),
                    allTours.getSize(),
                    allTours.getTotalElements(),
                    allTours.getTotalPages(),
                    allTours.isFirst(),
                    allTours.isLast()
            );

        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public PageResponse<TourResponse> findAll(int page, int size, TourStatusEnum status, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<Tour> allTours = tourRepository.findAllTour(status, pageable);

            List<TourResponse> toursResponse = allTours.stream()
                    .map(tourMapper::toTourResponse)
                    .toList();

            for(TourResponse tourResponse :toursResponse){
                List<TourGallery> tourGalleries = tourGalleryRepository.findByTourIdAndOrderIndex(tourResponse.getId(), 1);
                if(!tourGalleries.isEmpty()){
                    tourResponse.setProfilePicture(tourGalleryMapper.toTourGalleryResponse(tourGalleries.get(0)));
                }
            }

            return new PageResponse<>(
                    toursResponse,
                    allTours.getNumber(),
                    allTours.getSize(),
                    allTours.getTotalElements(),
                    allTours.getTotalPages(),
                    allTours.isFirst(),
                    allTours.isLast()
            );

        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    public Tour getTourByIdAndProviderId(Integer id, Integer providerId){
        return tourRepository.findTourByIdAndProviderId(id, providerId);
    }
    private State getState(Integer stateId){
        State state = stateService.findById(stateId);
        if(state != null){
            return state;
        }else{
            throw new ResourceNotFoundException("No state found with the id = "+ stateId);
        }
    }

    private Country getCountry(Integer countryId){
        Country country = countryService.findById(countryId);
        if(country != null){
            return country;
        }else{
            throw new ResourceNotFoundException("No country found with the id = "+ countryId);
        }
    }

    private City getCity(Integer cityId){
        City city = cityService.findById(cityId);
        if(city != null){
            return city;
        }else{
            throw new ResourceNotFoundException("No city found with the id = "+ cityId);
        }
    }

    @Transactional
    public TourFullDataResponse saveCreateOrUpdateFullData(TourFullDataRequest tourFullDataRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            if(tourFullDataRequest.getId() == null){
                return processCreateTourFullData(user, tourFullDataRequest);
            }else{
                return processUpdateTourFullData(tourFullDataRequest.getId(), user, tourFullDataRequest);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private TourFullDataResponse processCreateTourFullData(User user, TourFullDataRequest tourFullDataRequest){
        Provider provider = providerService.findByUserAndStatusActive(user);
        TourCategory tourCategory = getTourCategory(tourFullDataRequest.getTourCategoryId());
        //Save tour
        Tour tour = tourMapper.toTour(tourFullDataRequest);
        tour.setProvider(provider);
        tour.setTourCategory(tourCategory);
        tour.setStatus(TourStatusEnum.CREATED);
        Tour tourNew =  tourRepository.save(tour);

        // Sync tags (opcional)
        if (tourFullDataRequest.getTagIds() != null) {
            tourTagsRepository.replaceTourTags(tourNew.getId(), tourFullDataRequest.getTagIds());
        }

        List<TourAddressResponse> tourAddressResponseList = createOrUpdateTourAddressesForTour(tourFullDataRequest.getLocations(), tourNew);

        List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
        List<TourMainAttractionResponse>  tourMainAttractionResponseList  =  createOrUpdateTourMainAttractionsForTour(tourMainAttractionRequestList, tourNew);

        List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
        List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  createOrUpdateTourIncludesExcludesForTour(tourIncludesRequest, tourNew, IncludeExcludeTypeEnum.INCLUDE);

        List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
        List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  createOrUpdateTourIncludesExcludesForTour(tourExcludesRequest, tourNew, IncludeExcludeTypeEnum.EXCLUDE);

        List<TourFaqRequest> tourFaqRequestList = tourFullDataRequest.getFaq();
        List<TourFaqResponse> tourFaqResponseList = createOrUpdateTourFaqsForTour(tourFaqRequestList, tourNew);

        List<TourItineraryRequest> tourItineraryRequestList = tourFullDataRequest.getItineraries();
        List<TourItineraryResponse>  tourItineraryResponseList  =  createOrUpdateTourItinerariesForTour(tourItineraryRequestList, tourNew);

        List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList = tourFullDataRequest.getCancellationPolicies();
        List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList =  createOrUpdateTourCancellationPoliciesForTour(tourCancellationPolicyRequestList, tourNew);

        List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourNew.getId());
        return tourMapper.toTourFullDataResponse(
                tourNew,
                tourAddressResponseList,
                tourMainAttractionResponseList,
                tourIncludesResponseList,
                tourExcludesResponseList,
                tourFaqResponseList,
                tourItineraryResponseList,
                tourCancellationPolicyResponseList,
                null,
                tagIds
        );
    }
    private TourFullDataResponse processUpdateTourFullData(Integer tourId, User user, TourFullDataRequest tourFullDataRequest){
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
        validateProcessUpdateTourFullData(tourFullDataRequest);
        if(tour != null){
            TourCategory tourCategory = getTourCategory(tourFullDataRequest.getTourCategoryId());
            tour.setTourCategory(tourCategory);
            tour.setName(copyTranslatedField(tourFullDataRequest.getName()));
            tour.setDescription(copyTranslatedField(tourFullDataRequest.getDescription()));
            tour.setDuration(tourFullDataRequest.getDuration());
            tour.setMaxPeople(tourFullDataRequest.getMaxPeople());
            tour.setPriceType(tourFullDataRequest.getPriceType());
            tour.setIsUnlimitedCapacity(tourFullDataRequest.getIsUnlimitedCapacity());
            tour.setSubCategory(tourFullDataRequest.getSubCategory());
            tour.setDurationEnum(tourFullDataRequest.getDurationEnum());
            tour.setTimeOfDay(
                    tourFullDataRequest.getTimeOfDay() != null
                            ? tourFullDataRequest.getTimeOfDay().stream()
                                    .map(com.tourya.api.constans.enums.TourTimeOfDayEnum::getValue)
                                    .toArray(String[]::new)
                            : null
            );
            tour.setHighlight(tourFullDataRequest.getHighlight());
            tour.setMinAge(tourFullDataRequest.getMinAge());
            tour.setRating(tourFullDataRequest.getRating());
            //update Tour
            Tour tourUpdate = tourRepository.saveAndFlush(tour);

            // Sync tags (solo si el cliente los envía)
            if (tourFullDataRequest.getTagIds() != null) {
                tourTagsRepository.replaceTourTags(tourUpdate.getId(), tourFullDataRequest.getTagIds());
            }

            List<TourAddressResponse> tourAddressResponseList = processUpdateTourFullDataLocations(tourFullDataRequest, tourUpdate);
            List<TourMainAttractionResponse>  tourMainAttractionResponseList  = processUpdateTourFullDataMainAttractions(tourFullDataRequest, tourUpdate);
            List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  processUpdateTourFullDataIncludes(tourFullDataRequest, tourUpdate);
            List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  processUpdateTourFullDataExcludes(tourFullDataRequest, tourUpdate);
            List<TourFaqResponse> tourFaqResponseList = processUpdateTourFullDataFaq(tourFullDataRequest, tourUpdate);
            List<TourItineraryResponse>  tourItineraryResponseList  =  processUpdateTourFullDataItinerary(tourFullDataRequest, tourUpdate);
            List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList =  processUpdateTourFullDataCancellationPolicy(tourFullDataRequest, tourUpdate);

            List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourUpdate.getId());
            return tourMapper.toTourFullDataResponse(
                    tourUpdate,
                    tourAddressResponseList,
                    tourMainAttractionResponseList,
                    tourIncludesResponseList,
                    tourExcludesResponseList,
                    tourFaqResponseList,
                    tourItineraryResponseList,
                    tourCancellationPolicyResponseList,
                    getAllByTourGallery(tourUpdate.getId()),
                    tagIds
            );

        }else{
            throw new ResourceNotFoundException("Tour not found with id = "+tourId+" to providerId = "+provider.getId());
        }
    }
    private void validateProcessUpdateTourFullData(TourFullDataRequest tourFullDataRequest){
        if(tourFullDataRequest.getModifiedArrayList() == null){
            throw new OperationNotPermittedException("Cannot modify the Tour, ModifiedArrayList cannot be empty.");
        }
    }
    private List<TourAddressResponse> processUpdateTourFullDataLocations(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedLocations()){
            return createOrUpdateTourAddressesForTour(tourFullDataRequest.getLocations(), tourUpdate);
        }
        return consultDataTourAddressListByTourId(tourUpdate.getId());
    }
    @Transactional
    public List<TourAddressResponse> createOrUpdateTourAddressesForTour(List<TourAddressRequest> incomingAddressRequests, Tour tour){

        List<TourAddress> existingAddresses = tourAddressRepository.findByTourId(tour.getId());

        Map<Integer, TourAddress> existingAddressesMap = existingAddresses.stream()
                .filter(addr -> addr.getId() != null)
                .collect(Collectors.toMap(TourAddress::getId, Function.identity()));

        Set<Integer> existingAddressIds = existingAddressesMap.keySet();

        Set<Integer> incomingAddressIds = incomingAddressRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourAddressRequest::getId)
                .collect(Collectors.toSet());

        List<TourAddress> addressesToSave = new ArrayList<>();
        List<Integer> addressesToDeleteIds = new ArrayList<>();

        for(TourAddressRequest request : incomingAddressRequests){
            TourAddress tourAddress;
            Country country = getCountry(request.getCountryId());
            State state = getState(request.getStateId());
            City city = getCity(request.getCityId());


            if(request.getId() != null && existingAddressesMap.containsKey(request.getId())){
                tourAddress = existingAddressesMap.get(request.getId());
                tourAddressMapper.updateTourAddressFromRequest(request, tourAddress);
                tourAddress.setCountry(country);
                tourAddress.setState(state);
                tourAddress.setCity(city);
            } else {
                tourAddress = tourAddressMapper.toTourAddress(request);
                tourAddress.setTour(tour);
                tourAddress.setCountry(country);
                tourAddress.setState(state);
                tourAddress.setCity(city);
            }
            addressesToSave.add(tourAddress);
        }

        for(Integer existingId : existingAddressIds){
            if(!incomingAddressIds.contains(existingId)){
                addressesToDeleteIds.add(existingId);
            }
        }

        if (!addressesToDeleteIds.isEmpty()) {
            tourAddressRepository.deleteAllById(addressesToDeleteIds);
        }

        List<TourAddress> savedAddresses = tourAddressRepository.saveAll(addressesToSave);

        return savedAddresses.stream()
                .map(tourAddressMapper::toTourAddressResponse)
                .toList();
    }
    private List<TourMainAttractionResponse> processUpdateTourFullDataMainAttractions(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedMainAttractions()){
            return createOrUpdateTourMainAttractionsForTour(tourFullDataRequest.getMainAttractions(), tourUpdate);
        }
        return getAllByTourMainAttractions(tourUpdate.getId());
    }
    @Transactional
    public List<TourMainAttractionResponse> createOrUpdateTourMainAttractionsForTour(List<TourMainAttractionRequest> incomingAttractionRequests, Tour tour){
        List<TourMainAttraction> existingAttractions = tourMainAttractionRepository.findByTourId(tour.getId());

        Map<Integer, TourMainAttraction> existingAttractionsMap = existingAttractions.stream()
                .filter(attr -> attr.getId() != null)
                .collect(Collectors.toMap(TourMainAttraction::getId, Function.identity()));

        Set<Integer> existingAttractionIds = existingAttractionsMap.keySet();

        Set<Integer> incomingAttractionIds = incomingAttractionRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourMainAttractionRequest::getId)
                .collect(Collectors.toSet());

        List<TourMainAttraction> attractionsToSave = new ArrayList<>();
        List<Integer> attractionsToDeleteIds = new ArrayList<>();

        for(TourMainAttractionRequest request : incomingAttractionRequests){
            TourMainAttraction tourMainAttraction;

            if(request.getId() != null && existingAttractionsMap.containsKey(request.getId())){
                tourMainAttraction = existingAttractionsMap.get(request.getId());
                tourMainAttractionMapper.updateTourMainAttractionFromRequest(request, tourMainAttraction);
            } else {
                tourMainAttraction = tourMainAttractionMapper.toTourMainAttraction(request);
                tourMainAttraction.setTour(tour);
            }
            attractionsToSave.add(tourMainAttraction);
        }

        for(Integer existingId : existingAttractionIds){
            if(!incomingAttractionIds.contains(existingId)){
                attractionsToDeleteIds.add(existingId);
            }
        }

        if (!attractionsToDeleteIds.isEmpty()) {
            tourMainAttractionRepository.deleteAllById(attractionsToDeleteIds);
        }

        List<TourMainAttraction> savedAttractions = tourMainAttractionRepository.saveAll(attractionsToSave);

        return savedAttractions.stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }
    private List<TourIncludesExcludesResponse> processUpdateTourFullDataIncludes(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedIncludes()){
            return createOrUpdateTourIncludesExcludesForTour(tourFullDataRequest.getIncludes(), tourUpdate, IncludeExcludeTypeEnum.INCLUDE);
        }
        return getAllByTourIncludesExcludes(tourUpdate.getId(), IncludeExcludeTypeEnum.INCLUDE);
    }
    private List<TourIncludesExcludesResponse> processUpdateTourFullDataExcludes(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedExcludes()){
            return createOrUpdateTourIncludesExcludesForTour(tourFullDataRequest.getExcludes(), tourUpdate, IncludeExcludeTypeEnum.EXCLUDE);
        }
        return getAllByTourIncludesExcludes(tourUpdate.getId(), IncludeExcludeTypeEnum.EXCLUDE);
    }
    @Transactional
    public List<TourIncludesExcludesResponse> createOrUpdateTourIncludesExcludesForTour(
            List<TourIncludesExcludesRequest> incomingRequests, Tour tour, IncludeExcludeTypeEnum type) {

        List<TourIncludesExcludes> existingItems = tourIncludesExcludesRepository.findByTourIdAndType(tour.getId(), type);

        Map<Integer, TourIncludesExcludes> existingItemsMap = existingItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TourIncludesExcludes::getId, Function.identity()));

        Set<Integer> existingItemIds = existingItemsMap.keySet();

        Set<Integer> incomingItemIds = incomingRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourIncludesExcludesRequest::getId)
                .collect(Collectors.toSet());

        List<TourIncludesExcludes> itemsToSave = new ArrayList<>();
        List<Integer> itemsToDeleteIds = new ArrayList<>();

        for(TourIncludesExcludesRequest request : incomingRequests){
            TourIncludesExcludes tourIncludesExcludes;

            if(request.getId() != null && existingItemsMap.containsKey(request.getId())){
                tourIncludesExcludes = existingItemsMap.get(request.getId());
                tourIncludesExcludesMapper.updateTourIncludesExcludesFromRequest(request, tourIncludesExcludes);
            } else {
                tourIncludesExcludes = tourIncludesExcludesMapper.toTourIncludesExcludes(request);
                tourIncludesExcludes.setTour(tour);
                tourIncludesExcludes.setType(type);
            }
            itemsToSave.add(tourIncludesExcludes);
        }

        for(Integer existingId : existingItemIds){
            if(!incomingItemIds.contains(existingId)){
                itemsToDeleteIds.add(existingId);
            }
        }

        if (!itemsToDeleteIds.isEmpty()) {
            tourIncludesExcludesRepository.deleteAllById(itemsToDeleteIds);
        }

        List<TourIncludesExcludes> savedItems = tourIncludesExcludesRepository.saveAll(itemsToSave);

        return savedItems.stream()
                .filter(item -> item.getType() == type)
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .toList();
    }
    private List<TourFaqResponse> processUpdateTourFullDataFaq(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedFaq()){
            return createOrUpdateTourFaqsForTour(tourFullDataRequest.getFaq(), tourUpdate);
        }
        return getAllByTourFaqs(tourUpdate.getId());
    }
    @Transactional
    public List<TourFaqResponse> createOrUpdateTourFaqsForTour(List<TourFaqRequest> incomingFaqRequests, Tour tour){

        List<TourFaq> existingFaqs = tourFaqRepository.findByTourId(tour.getId());

        Map<Integer, TourFaq> existingFaqsMap = existingFaqs.stream()
                .filter(faq -> faq.getId() != null)
                .collect(Collectors.toMap(TourFaq::getId, Function.identity()));

        Set<Integer> existingFaqIds = existingFaqsMap.keySet();

        Set<Integer> incomingFaqIds = incomingFaqRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourFaqRequest::getId)
                .collect(Collectors.toSet());

        List<TourFaq> faqsToSave = new ArrayList<>();
        List<Integer> faqsToDeleteIds = new ArrayList<>();

        for(TourFaqRequest request : incomingFaqRequests){
            TourFaq tourFaq;

            if(request.getId() != null && existingFaqsMap.containsKey(request.getId())){
                tourFaq = existingFaqsMap.get(request.getId());
                tourFaqMapper.updateTourFaqFromRequest(request, tourFaq);
            } else {
                tourFaq = tourFaqMapper.toTourFaq(request);
                tourFaq.setTour(tour);
            }
            faqsToSave.add(tourFaq);
        }

        for(Integer existingId : existingFaqIds){
            if(!incomingFaqIds.contains(existingId)){
                faqsToDeleteIds.add(existingId);
            }
        }

        if (!faqsToDeleteIds.isEmpty()) {
            tourFaqRepository.deleteAllById(faqsToDeleteIds);
        }

        List<TourFaq> savedFaqs = tourFaqRepository.saveAll(faqsToSave);

        return savedFaqs.stream()
                .map(tourFaqMapper::toTourFaqResponse)
                .toList();
    }
    private List<TourItineraryResponse> processUpdateTourFullDataItinerary(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedItineraries()){
            return createOrUpdateTourItinerariesForTour(tourFullDataRequest.getItineraries(), tourUpdate);
        }
        return getAllByTourItineraries(tourUpdate.getId());
    }
    @Transactional
    public List<TourItineraryResponse> createOrUpdateTourItinerariesForTour(List<TourItineraryRequest> incomingItineraryRequests, Tour tour){

        List<TourItinerary> existingItineraries = tourItineraryRepository.findByTourId(tour.getId());

        Map<Integer, TourItinerary> existingItinerariesMap = existingItineraries.stream()
                .filter(itinerary -> itinerary.getId() != null)
                .collect(Collectors.toMap(TourItinerary::getId, Function.identity()));

        Set<Integer> existingItineraryIds = existingItinerariesMap.keySet();

        Set<Integer> incomingItineraryIds = incomingItineraryRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourItineraryRequest::getId)
                .collect(Collectors.toSet());

        List<TourItinerary> itinerariesToSave = new ArrayList<>();
        List<Integer> itinerariesToDeleteIds = new ArrayList<>();

        for(TourItineraryRequest request : incomingItineraryRequests){
            TourItinerary tourItinerary;

            if(request.getId() != null && existingItinerariesMap.containsKey(request.getId())){
                tourItinerary = existingItinerariesMap.get(request.getId());
                tourItineraryMapper.updateTourItineraryFromRequest(request, tourItinerary);
            } else {
                tourItinerary = tourItineraryMapper.toTourItinerary(request);
                tourItinerary.setTour(tour);
            }
            itinerariesToSave.add(tourItinerary);
        }

        for(Integer existingId : existingItineraryIds){
            if(!incomingItineraryIds.contains(existingId)){
                itinerariesToDeleteIds.add(existingId);
            }
        }

        if (!itinerariesToDeleteIds.isEmpty()) {
            tourItineraryRepository.deleteAllById(itinerariesToDeleteIds);
        }

        List<TourItinerary> savedItineraries = tourItineraryRepository.saveAll(itinerariesToSave);

        return savedItineraries.stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .toList();
    }
    private List<TourCancellationPolicyResponse> processUpdateTourFullDataCancellationPolicy(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        if(tourFullDataRequest.getModifiedArrayList() != null && tourFullDataRequest.getModifiedArrayList().isUpdatedCancellationPolicies()){
            return createOrUpdateTourCancellationPoliciesForTour(tourFullDataRequest.getCancellationPolicies(), tourUpdate);
        }
        return getAllByTourCancellationPolicy(tourUpdate.getId());
    }
    @Transactional
    public List<TourCancellationPolicyResponse> createOrUpdateTourCancellationPoliciesForTour(List<TourCancellationPolicyRequest> incomingPolicyRequests, Tour tour){

        List<TourCancellationPolicy> existingPolicies = tourCancellationPolicyRepository.findByTourId(tour.getId());

        Map<Integer, TourCancellationPolicy> existingPoliciesMap = existingPolicies.stream()
                .filter(policy -> policy.getId() != null)
                .collect(Collectors.toMap(TourCancellationPolicy::getId, Function.identity()));

        Set<Integer> existingPolicyIds = existingPoliciesMap.keySet();

        Set<Integer> incomingPolicyIds = incomingPolicyRequests.stream()
                .filter(req -> req.getId() != null)
                .map(TourCancellationPolicyRequest::getId)
                .collect(Collectors.toSet());

        List<TourCancellationPolicy> policiesToSave = new ArrayList<>();
        List<Integer> policiesToDeleteIds = new ArrayList<>();

        for(TourCancellationPolicyRequest request : incomingPolicyRequests){
            TourCancellationPolicy tourCancellationPolicy;

            if(request.getId() != null && existingPoliciesMap.containsKey(request.getId())){
                tourCancellationPolicy = existingPoliciesMap.get(request.getId());
                tourCancellationPolicyMapper.updateTourCancellationPolicyFromRequest(request, tourCancellationPolicy);
            } else {
                tourCancellationPolicy = tourCancellationPolicyMapper.toTourCancellationPolicy(request);
                tourCancellationPolicy.setTour(tour);
            }
            policiesToSave.add(tourCancellationPolicy);
        }

        for(Integer existingId : existingPolicyIds){
            if(!incomingPolicyIds.contains(existingId)){
                policiesToDeleteIds.add(existingId);
            }
        }

        if (!policiesToDeleteIds.isEmpty()) {
            tourCancellationPolicyRepository.deleteAllById(policiesToDeleteIds);
        }

        List<TourCancellationPolicy> savedPolicies = tourCancellationPolicyRepository.saveAll(policiesToSave);

        return savedPolicies.stream()
                .map(tourCancellationPolicyMapper::toTourCancellationPolicyResponse)
                .toList();
    }

    public TourFullDataResponse consultDataTourById(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
            if(tour != null){
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tour,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        null,
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId+" to providerId = "+provider.getId());
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public TourFullDataResponse consultDataTourByIdToAdmin(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Tour> optionalTour = tourRepository.findById(tourId);
            if(optionalTour.isPresent()){
                Tour tour = optionalTour.get();
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tour,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        getAllByTourGallery(tourId),
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public TourFullDataResponse acceptTourByIdToAdmin(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Tour> optionalTour = tourRepository.findById(tourId);
            if(optionalTour.isPresent()){
                Tour tour = optionalTour.get();
                tour.setStatus(TourStatusEnum.ACCEPTED);
                Tour tourUpdate = tourRepository.save(tour);
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tourUpdate,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        null,
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public TourFullDataResponse returnedTourByIdToAdmin(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Tour> optionalTour = tourRepository.findById(tourId);
            if(optionalTour.isPresent()){
                Tour tour = optionalTour.get();
                tour.setStatus(TourStatusEnum.RETURNED);
                Tour tourUpdate = tourRepository.save(tour);
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tourUpdate,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        null,
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    public TourFullDataResponse submitTourByIdToProvider(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Optional<Tour> optionalTour = tourRepository.findById(tourId);
            if(optionalTour.isPresent()){
                Tour tour = optionalTour.get();
                tour.setStatus(TourStatusEnum.SUBMITTED);
                Tour tourUpdate = tourRepository.save(tour);
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tourUpdate,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        null,
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    public TourFullDataResponse cancelTourByIdToAdmin(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Tour> optionalTour = tourRepository.findById(tourId);
            if(optionalTour.isPresent()){
                Tour tour = optionalTour.get();
                tour.setStatus(TourStatusEnum.CANCELLED);
                Tour tourUpdate = tourRepository.save(tour);
                List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
                return tourMapper.toTourFullDataResponse(
                        tourUpdate,
                        consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE),
                        getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),
                        getAllByTourCancellationPolicy(tourId),
                        null,
                        tagIds
                );
            }else{
                throw new ResourceNotFoundException("Tour not found with id = "+tourId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public List<TourAddressResponse> consultDataTourAddressListByTourId(Integer tourId){
        return tourAddressRepository.findByTourId(tourId).stream()
                .map(tourAddressMapper::toTourAddressResponse)
                .toList();
    }
    private List<TourMainAttractionResponse> getAllByTourMainAttractions(Integer tourId) {
        return tourMainAttractionRepository.findByTourId(tourId)
                .stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }
    private List<TourIncludesExcludesResponse> getAllByTourIncludesExcludes(Integer tourId, IncludeExcludeTypeEnum type) {
        if(type != null){
            return tourIncludesExcludesRepository.findByTourIdAndType(tourId, type).stream()
                    .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                    .toList();
        }else{
            return tourIncludesExcludesRepository.findByTourId(tourId).stream()
                    .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                    .toList();
        }
    }
    private List<TourFaqResponse> getAllByTourFaqs(Integer tourId) {
        return tourFaqRepository.findByTourId(tourId)
                .stream()
                .map(tourFaqMapper::toTourFaqResponse)
                .toList();
    }
    private List<TourItineraryResponse> getAllByTourItineraries(Integer tourId) {
        return tourItineraryRepository.findByTourId(tourId)
                .stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .toList();
    }
    private List<TourCancellationPolicyResponse> getAllByTourCancellationPolicy(Integer tourId) {
        return tourCancellationPolicyRepository.findByTourId(tourId)
                .stream()
                .map(tourCancellationPolicyMapper::toTourCancellationPolicyResponse)
                .toList();
    }
    private List<TourGalleryResponse> getAllByTourGallery(Integer tourId) {
        return tourGalleryRepository.findByTourId(tourId)
                .stream()
                .map(tourGalleryMapper::toTourGalleryResponse)
                .toList();
    }

    // ------------------- NEW UNIFIED METHODS -------------------

    /**
     * Determines the user's role and calls the builder method.
     * This acts as a dispatcher.
     */
    public TourFullDataResponse getTourDetailsById(Integer tourId, @Nullable Authentication connectedUser) {
        UserRoleType roleType = UserRoleType.PUBLIC;
        User user = null;

        if (connectedUser != null) {
            user = (User) connectedUser.getPrincipal();
            List<Role> roles = user.getRoles();

            if (Utils.isAdmin(roles)) {
                roleType = UserRoleType.ADMIN;
            } else if (Utils.isProvider(roles)) {
                roleType = UserRoleType.PROVIDER;
            }
        }

        return buildTourResponse(tourId, roleType, user);
    }

    /**
     * Fetches and builds the complete Tour response based on the user's role.
     * This is the core builder logic.
     */
    private TourFullDataResponse buildTourResponse(Integer tourId, UserRoleType roleType, @Nullable User user) {
        Tour tour;

        switch (roleType) {
            case ADMIN:
                tour = tourRepository.findById(tourId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tour not found with id = " + tourId));
                break;

            case PROVIDER:
                Provider provider = providerService.findByUserAndStatusActive(user);
                tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
                if (tour == null) {
                    throw new ResourceNotFoundException("Tour not found with id = " + tourId + " for this provider.");
                }
                break;

            case PUBLIC:
            default:
                // For public, we only show accepted tours.
                tour = tourRepository.findTourByIdAndStatus(tourId, TourStatusEnum.ACCEPTED)
                        .orElseThrow(() -> new ResourceNotFoundException("Tour not found or is not available with id = " + tourId));
                break;
        }

        // Fetch all related data
        List<TourAddressResponse> locations = consultDataTourAddressListByTourId(tourId);
        List<TourMainAttractionResponse> mainAttractions = getAllByTourMainAttractions(tourId);
        List<TourIncludesExcludesResponse> includes = getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE);
        List<TourIncludesExcludesResponse> excludes = getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE);
        List<TourFaqResponse> faqs = getAllByTourFaqs(tourId);
        List<TourItineraryResponse> itineraries = getAllByTourItineraries(tourId);
        List<TourCancellationPolicyResponse> cancellationPolicies = getAllByTourCancellationPolicy(tourId);
        //List<TourGalleryResponse> galleries = (roleType == UserRoleType.ADMIN) ? getAllByTourGallery(tourId) : null;
        List<TourGalleryResponse> galleries = getAllByTourGallery(tourId);

        List<Integer> tagIds = tourTagsRepository.getTagIdsByTourId(tourId);
        TourFullDataResponse resp = tourMapper.toTourFullDataResponse(tour, locations, mainAttractions, includes, excludes, faqs, itineraries, cancellationPolicies, galleries, tagIds);

        // Promedio de reseñas publicadas (1 decimal)
        java.math.BigDecimal avg = reviewRepository.avgPublishedRatingByTourId(tourId);
        if (avg != null) {
            try {
                resp.setRating(avg.setScale(1, java.math.RoundingMode.HALF_UP));
            } catch (Exception ignored) {
            }
        }
        return resp;
    }
}
