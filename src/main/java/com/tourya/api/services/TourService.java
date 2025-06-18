package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    private final TourCategoryService tourCategoryService;
    private final ProviderService providerService;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;
    private final S3Service s3Service;
    private final TourMapper tourMapper;
    private final TourMainAttractionMapper tourMainAttractionMapper;
    private final TourIncludesExcludesMapper tourIncludesExcludesMapper;
    private final TourAddressMapper tourAddressMapper;
    private final TourFaqMapper tourFaqMapper;
    private final TourItineraryMapper tourItineraryMapper;
    private final TourGalleryMapper tourGalleryMapper;
    private final TourCancellationPolicyMapper tourCancellationPolicyMapper;


    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    public TourResponse save(TourRequest tourRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            TourCategory tourCategory = getTourCategory(tourRequest.getTourCategoryId());
            Tour tour = tourMapper.toTour(tourRequest);
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            return tourMapper.toTourResponse(tourRepository.save(tour));
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
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
    @Transactional
    public TourDetailsResponse saveCreateBasicData(TourCreateRequest tourCreateRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            TourCategory tourCategory = getTourCategory(tourCreateRequest.getTourCategoryId());
            Tour tour = tourMapper.toTour(tourCreateRequest);
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            tour.setStatus(TourStatusEnum.CREATED);
            Tour tourNew =  tourRepository.save(tour);

            List<TourAddressResponse> tourAddressResponseList = saveTourAddressListByTourId(tourCreateRequest.getLocations(), tourNew);
            TourDetailsResponse tourDetailsResponse =  new TourDetailsResponse();
            tourDetailsResponse.setId(tourNew.getId());
            tourDetailsResponse.setName(tourNew.getName());
            tourDetailsResponse.setDescription(tourNew.getDescription());
            tourDetailsResponse.setDuration(tourNew.getDuration());
            tourDetailsResponse.setMaxPeople(tourNew.getMaxPeople());
            tourDetailsResponse.setHighlight(tourNew.getHighlight());
            tourDetailsResponse.setLocations(tourAddressResponseList);
            return  tourDetailsResponse;
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private List<TourAddressResponse> saveTourAddressListByTourId(List<TourAddressRequest> tourAddressRequestList,
                                                                  Tour tour){
        List<TourAddress> tourAddressList = new ArrayList<>();
        for(TourAddressRequest tourAddressRequest : tourAddressRequestList){
            TourAddress tourAddress = tourAddressMapper.toTourAddress(tourAddressRequest);
            Country country = getCountry(tourAddressRequest.getCountryId());
            State state = getState(tourAddressRequest.getStateId());
            City city = getCity(tourAddressRequest.getCityId());
            tourAddress.setTour(tour);
            tourAddress.setCountry(country);
            tourAddress.setState(state);
            tourAddress.setCity(city);
            tourAddressList.add(tourAddress);
        }
        return tourAddressRepository.saveAll(tourAddressList).stream()
                    .map(tourAddressMapper::toTourAddressResponse)
                    .toList();

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
    public TourFullDataResponse saveCreateOrUpdateFullData(List<MultipartFile> files, TourFullDataRequest tourFullDataRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            if(tourFullDataRequest.getId() == null){
                return processCreateTourFullData(files, user, tourFullDataRequest);
            }else{
                return processUpdateTourFullData(files, tourFullDataRequest.getId(), user, tourFullDataRequest);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private TourFullDataResponse processCreateTourFullData(List<MultipartFile> files,User user, TourFullDataRequest tourFullDataRequest){
        Provider provider = providerService.findByUserAndStatusActive(user);
        TourCategory tourCategory = getTourCategory(tourFullDataRequest.getTourCategoryId());
        //Save tour
        Tour tour = tourMapper.toTour(tourFullDataRequest);
        tour.setProvider(provider);
        tour.setTourCategory(tourCategory);
        tour.setStatus(TourStatusEnum.CREATED);
        Tour tourNew =  tourRepository.save(tour);

        List<TourAddressResponse> tourAddressResponseList = saveTourAddressListByTourId(tourFullDataRequest.getLocations(), tourNew);

        List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
        List<TourMainAttractionResponse>  tourMainAttractionResponseList  =  mainAttractionReplaceAllForTour(tourMainAttractionRequestList, tourNew);

        List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
        List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  includesExcludesReplaceAllForTour(tourIncludesRequest, tourNew, IncludeExcludeTypeEnum.INCLUDE);

        List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
        List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  includesExcludesReplaceAllForTour(tourExcludesRequest, tourNew, IncludeExcludeTypeEnum.EXCLUDE);

        List<TourFaqRequest> tourFaqRequestList = tourFullDataRequest.getFaq();
        List<TourFaqResponse> tourFaqResponseList = faqReplaceAllForTour(tourFaqRequestList, tourNew);

        List<TourItineraryRequest> tourItineraryRequestList = tourFullDataRequest.getItineraries();
        List<TourItineraryResponse>  tourItineraryResponseList  =  itineraryReplaceAllForTour(tourItineraryRequestList, tourNew);

        List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList = tourFullDataRequest.getCancellationPolicies();
        List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList =  cancellationPoliciesReplaceAllForTour(tourCancellationPolicyRequestList, tourNew);

        List<TourGalleryRequest> tourGalleryRequestList = tourFullDataRequest.getGalleries();
        List<TourGalleryResponse>  tourGalleryResponseList  =  galleryReplaceAllForTour(files, tourGalleryRequestList, tourNew);


        return tourMapper.toTourFullDataResponse(tourNew, tourAddressResponseList,
                tourMainAttractionResponseList, tourIncludesResponseList, tourExcludesResponseList, tourFaqResponseList,  tourItineraryResponseList, tourGalleryResponseList, tourCancellationPolicyResponseList);
    }
    private TourFullDataResponse processUpdateTourFullData(List<MultipartFile> files, Integer tourId, User user, TourFullDataRequest tourFullDataRequest){
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
        validateProcessUpdateTourFullData(tourFullDataRequest);
        if(tour != null){
            tour.setName(tourFullDataRequest.getName());
            tour.setDescription(tourFullDataRequest.getDescription());
            tour.setDuration(tourFullDataRequest.getDuration());
            tour.setMaxPeople(tourFullDataRequest.getMaxPeople());
            tour.setHighlight(tourFullDataRequest.getHighlight());
            //update Tour
            Tour tourUpdate = tourRepository.save(tour);

            List<TourAddressResponse> tourAddressResponseList = processUpdateTourFullDataLocations(tourFullDataRequest, tourUpdate);
            List<TourMainAttractionResponse>  tourMainAttractionResponseList  = processUpdateTourFullDataMainAttractions(tourFullDataRequest, tourUpdate);
            List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  processUpdateTourFullDataIncludes(tourFullDataRequest, tourUpdate);
            List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  processUpdateTourFullDataExcludes(tourFullDataRequest, tourUpdate);
            List<TourFaqResponse> tourFaqResponseList = processUpdateTourFullDataFaq(tourFullDataRequest, tourUpdate);
            List<TourItineraryResponse>  tourItineraryResponseList  =  processUpdateTourFullDataItinerary(tourFullDataRequest, tourUpdate);
            List<TourGalleryResponse>  tourGalleryResponseList  =  processUpdateTourFullDataGallery(files, tourFullDataRequest, tourUpdate);
            List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList =  processUpdateTourFullDataCancellationPolicy(tourFullDataRequest, tourUpdate);
            return tourMapper.toTourFullDataResponse(tourUpdate, tourAddressResponseList,
                    tourMainAttractionResponseList, tourIncludesResponseList, tourExcludesResponseList, tourFaqResponseList,  tourItineraryResponseList, tourGalleryResponseList, tourCancellationPolicyResponseList);

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
        List<TourAddressResponse> tourAddressResponseList = consultDataTourAddressListByTourId(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedLocations()){
            tourAddressResponseList = tourAddressReplaceAllForTour(tourFullDataRequest.getLocations(), tourUpdate);
        }
        return tourAddressResponseList;
    }
    private List<TourMainAttractionResponse> processUpdateTourFullDataMainAttractions(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourMainAttractionResponse>  tourMainAttractionResponseList = getAllByTourMainAttractions(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedMainAttractions()){
            List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
            tourMainAttractionResponseList  =  mainAttractionReplaceAllForTour(tourMainAttractionRequestList, tourUpdate);
        }
        return tourMainAttractionResponseList;
    }
    private List<TourIncludesExcludesResponse> processUpdateTourFullDataIncludes(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourIncludesExcludesResponse>  tourIncludesResponseList = getAllByTourIncludesExcludes(tourUpdate.getId(), IncludeExcludeTypeEnum.INCLUDE);
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedIncludes()){
            List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
            tourIncludesResponseList  =  includesExcludesReplaceAllForTour(tourIncludesRequest, tourUpdate, IncludeExcludeTypeEnum.INCLUDE);
        }
        return tourIncludesResponseList;
    }
    private List<TourIncludesExcludesResponse> processUpdateTourFullDataExcludes(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourIncludesExcludesResponse>  tourExcludesResponseList = getAllByTourIncludesExcludes(tourUpdate.getId(), IncludeExcludeTypeEnum.EXCLUDE);
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedExcludes()){
            List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
            tourExcludesResponseList  =  includesExcludesReplaceAllForTour(tourExcludesRequest, tourUpdate, IncludeExcludeTypeEnum.EXCLUDE);
        }
        return tourExcludesResponseList;
    }
    private List<TourFaqResponse> processUpdateTourFullDataFaq(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourFaqResponse> tourFaqResponseList = getAllByTourFaqs(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedFaq()){
            List<TourFaqRequest> tourFaqRequestList = tourFullDataRequest.getFaq();
            tourFaqResponseList = faqReplaceAllForTour(tourFaqRequestList, tourUpdate);
        }
        return tourFaqResponseList;
    }
    private List<TourItineraryResponse> processUpdateTourFullDataItinerary(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourItineraryResponse>  tourItineraryResponseList = getAllByTourItineraries(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedItineraries()){
            List<TourItineraryRequest> tourItineraryRequestList = tourFullDataRequest.getItineraries();
            tourItineraryResponseList  =  itineraryReplaceAllForTour(tourItineraryRequestList, tourUpdate);
        }
        return tourItineraryResponseList;
    }
    private List<TourGalleryResponse> processUpdateTourFullDataGallery(List<MultipartFile> files, TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourGalleryResponse>  tourGalleryResponseList = getAllByTourGalleries(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedGalleries()){
            List<TourGalleryRequest> tourGalleryRequestList = tourFullDataRequest.getGalleries();
            tourGalleryResponseList  =  galleryReplaceAllForTour(files, tourGalleryRequestList, tourUpdate);
        }
        return tourGalleryResponseList;
    }
    private List<TourCancellationPolicyResponse> processUpdateTourFullDataCancellationPolicy(TourFullDataRequest tourFullDataRequest, Tour tourUpdate){
        List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList = getAllByTourCancellationPolicy(tourUpdate.getId());
        if(tourFullDataRequest.getModifiedArrayList().isUpdatedCancellationPolicies()){
            List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList = tourFullDataRequest.getCancellationPolicies();
            tourCancellationPolicyResponseList =  cancellationPoliciesReplaceAllForTour(tourCancellationPolicyRequestList, tourUpdate);
        }
        return tourCancellationPolicyResponseList;
    }
    private List<TourAddressResponse> tourAddressReplaceAllForTour(List<TourAddressRequest> tourAddressRequestList,
                                                                  Tour tour){

        List<TourAddress> tourAddressDeleteList = tourAddressRepository.findByTourId(tour.getId());
        tourAddressRepository.deleteAll(tourAddressDeleteList);

        List<TourAddress> tourAddressList = new ArrayList<>();
        for(TourAddressRequest tourAddressRequest : tourAddressRequestList){
            TourAddress tourAddress = tourAddressMapper.toTourAddress(tourAddressRequest);
            Country country = getCountry(tourAddressRequest.getCountryId());
            State state = getState(tourAddressRequest.getStateId());
            City city = getCity(tourAddressRequest.getCityId());
            tourAddress.setTour(tour);
            tourAddress.setCountry(country);
            tourAddress.setState(state);
            tourAddress.setCity(city);
            tourAddressList.add(tourAddress);
        }
        return tourAddressRepository.saveAll(tourAddressList).stream()
                .map(tourAddressMapper::toTourAddressResponse)
                .toList();

    }
    private List<TourMainAttractionResponse> mainAttractionReplaceAllForTour(List<TourMainAttractionRequest> requests,
                                                              Tour tour) {

        tourMainAttractionRepository.deleteByTourId(tour.getId());

        List<TourMainAttraction> newAttractions = requests.stream()
                .map(req -> {
                    TourMainAttraction entity = tourMainAttractionMapper.toTourMainAttraction(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        return tourMainAttractionRepository.saveAll(newAttractions).stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }
    private List<TourIncludesExcludesResponse> includesExcludesReplaceAllForTour(List<TourIncludesExcludesRequest> requests,
                                                                Tour tour,
                                                                IncludeExcludeTypeEnum type) {


        List<TourIncludesExcludes>  list = tourIncludesExcludesRepository.findByTourIdAndType(tour.getId(), type);
        tourIncludesExcludesRepository.deleteAll(list);

        List<TourIncludesExcludes> newList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes entity = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        tourIncludesExcludesRepository.saveAll(newList);
        return tourIncludesExcludesRepository.findByTourIdAndType(tour.getId(), type).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .toList();
    }
    private List<TourFaqResponse> faqReplaceAllForTour(List<TourFaqRequest> requests,
                                                       Tour tour) {

        tourFaqRepository.deleteByTourId(tour.getId());

        List<TourFaq> newList = requests.stream()
                .map(req -> {
                    TourFaq entity = tourFaqMapper.toTourFaq(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        return tourFaqRepository.saveAll(newList).stream()
                .map(tourFaqMapper::toTourFaqResponse)
                .toList();
    }

    private List<TourItineraryResponse> itineraryReplaceAllForTour(List<TourItineraryRequest> requests,
                                                                             Tour tour) {

        tourItineraryRepository.deleteByTourId(tour.getId());

        List<TourItinerary> newList = requests.stream()
                .map(req -> {
                    TourItinerary entity = tourItineraryMapper.toTourItinerary(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        return tourItineraryRepository.saveAll(newList).stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .toList();
    }
    private List<TourCancellationPolicyResponse> cancellationPoliciesReplaceAllForTour(List<TourCancellationPolicyRequest> requests,
                                                       Tour tour) {

        tourCancellationPolicyRepository.deleteByTourId(tour.getId());

        List<TourCancellationPolicy> newList = requests.stream()
                .map(req -> {
                    TourCancellationPolicy entity = tourCancellationPolicyMapper.toTourCancellationPolicy(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        return tourCancellationPolicyRepository.saveAll(newList).stream()
                .map(tourCancellationPolicyMapper::toTourCancellationPolicyResponse)
                .toList();
    }

    private List<TourGalleryResponse> galleryReplaceAllForTour(List<MultipartFile> files, List<TourGalleryRequest> requests,
                                                               Tour tour) {

        if(files.size() == 1 && files.get(0).isEmpty() && requests.isEmpty()){
            tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tour.getId()).forEach(gallery -> {
                s3Service.deleteFile(gallery.getImageUrl());
                tourGalleryRepository.delete(gallery);
            });

            return new ArrayList<>();
        }else{
            validateFilesAndMetadata(files, requests);
            tourGalleryRepository.findByTourIdOrderByOrderIndexAsc(tour.getId()).forEach(gallery -> {
                s3Service.deleteFile(gallery.getImageUrl());
                tourGalleryRepository.delete(gallery);
            });

            List<TourGallery> savedGalleries = IntStream.range(0, files.size())
                    .mapToObj(i -> buildGalleryEntity(files.get(i), requests.get(i), tour))
                    .map(tourGalleryRepository::save)
                    .toList();

            return savedGalleries.stream()
                    .map(tourGalleryMapper::toTourGalleryResponse)
                    .toList();
        }

    }

    public TourFullDataResponse consultDataTourById(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
            if(tour != null){
                return tourMapper.toTourFullDataResponse(tour, consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId), getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),getAllByTourGalleries(tourId), getAllByTourCancellationPolicy(tourId));
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
                return tourMapper.toTourFullDataResponse(tour, consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId), getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),getAllByTourGalleries(tourId), getAllByTourCancellationPolicy(tourId));
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
                return tourMapper.toTourFullDataResponse(tourUpdate, consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId), getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),getAllByTourGalleries(tourId), getAllByTourCancellationPolicy(tourId));
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
                return tourMapper.toTourFullDataResponse(tourUpdate, consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId), getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId),
                        getAllByTourItineraries(tourId),getAllByTourGalleries(tourId), getAllByTourCancellationPolicy(tourId));
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
    private List<TourGalleryResponse> getAllByTourGalleries(Integer tourId) {
        return tourGalleryRepository.findByTourId(tourId)
                .stream()
                .map(tourGalleryMapper::toTourGalleryResponse)
                .toList();
    }
    private void validateFilesAndMetadata(List<MultipartFile> files, List<TourGalleryRequest> requests) {
        if (files.size() != requests.size()) {
            throw new OperationNotPermittedException("Each uploaded file must match a metadata entry.");
        }
    }
    private Tour getTourOrThrow(Integer tourId, Integer providerId) {
        Tour tour = getTourByIdAndProviderId(tourId, providerId);
        if (tour == null) {
            throw new ResourceNotFoundException("No tour with this ID was found for this provider.");
        }
        return tour;
    }
    private TourGallery buildGalleryEntity(MultipartFile file, TourGalleryRequest request, Tour tour) {
        try {
            TourGallery entity = tourGalleryMapper.toTourGallery(request);
            entity.setTour(tour);
            entity.setCreatedDate(LocalDateTime.now());
            entity.setImageUrl(s3Service.uploadFile("tours/" + tour.getId(), file));
            return entity;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
}
