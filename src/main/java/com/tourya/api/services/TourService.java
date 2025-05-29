package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.State;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.TourFaq;
import com.tourya.api.models.TourIncludesExcludes;
import com.tourya.api.models.TourMainAttraction;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourAddressMapper;
import com.tourya.api.models.mapper.TourFaqMapper;
import com.tourya.api.models.mapper.TourIncludesExcludesMapper;
import com.tourya.api.models.mapper.TourMainAttractionMapper;
import com.tourya.api.models.mapper.TourMapper;
import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.responses.TourDetailsResponse;
import com.tourya.api.models.responses.TourFaqResponse;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.request.TourAddressRequest;
import com.tourya.api.models.request.TourCreateRequest;
import com.tourya.api.models.request.TourFaqRequest;
import com.tourya.api.models.request.TourFullDataRequest;
import com.tourya.api.models.request.TourIncludesExcludesRequest;
import com.tourya.api.models.request.TourMainAttractionRequest;
import com.tourya.api.models.request.TourRequest;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourFaqRepository;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import com.tourya.api.repository.TourMainAttractionRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TourService {
    private final TourRepository tourRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourFaqRepository tourFaqRepository;
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

        List<TourAddressResponse> tourAddressResponseList = saveTourAddressListByTourId(tourFullDataRequest.getLocations(), tourNew);

        List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
        List<TourMainAttractionResponse>  tourMainAttractionResponseList  =  mainAttractionReplaceAllForTour(tourMainAttractionRequestList, tourNew);

        List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
        List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  includesExcludesReplaceAllForTour(tourIncludesRequest, tourNew, IncludeExcludeTypeEnum.INCLUDE);

        List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
        List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  includesExcludesReplaceAllForTour(tourExcludesRequest, tourNew, IncludeExcludeTypeEnum.EXCLUDE);

        List<TourFaqRequest> tourFaqRequestList = tourFullDataRequest.getFaq();
        List<TourFaqResponse> tourFaqResponseList = faqReplaceAllForTour(tourFaqRequestList, tourNew);

        return tourMapper.toTourFullDataResponse(tourNew, tourAddressResponseList,
                tourMainAttractionResponseList, tourIncludesResponseList, tourExcludesResponseList, tourFaqResponseList);
    }
    private TourFullDataResponse processUpdateTourFullData(Integer tourId, User user, TourFullDataRequest tourFullDataRequest){
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
        if(tour != null){
            tour.setName(tourFullDataRequest.getName());
            tour.setDescription(tourFullDataRequest.getDescription());
            tour.setDuration(tourFullDataRequest.getDuration());
            tour.setMaxPeople(tourFullDataRequest.getMaxPeople());
            tour.setHighlight(tourFullDataRequest.getHighlight());
            //update Tour
            Tour tourUpdate = tourRepository.save(tour);

            //replace tourAddressList
            List<TourAddressResponse> tourAddressResponseList = tourAddressReplaceAllForTour(tourFullDataRequest.getLocations(), tourUpdate);
            List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
            List<TourMainAttractionResponse>  tourMainAttractionResponseList  =  mainAttractionReplaceAllForTour(tourMainAttractionRequestList, tourUpdate);

            List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
            List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  includesExcludesReplaceAllForTour(tourIncludesRequest, tourUpdate, IncludeExcludeTypeEnum.INCLUDE);

            List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
            List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  includesExcludesReplaceAllForTour(tourExcludesRequest, tourUpdate, IncludeExcludeTypeEnum.EXCLUDE);

            List<TourFaqRequest> tourFaqRequestList = tourFullDataRequest.getFaq();
            List<TourFaqResponse> tourFaqResponseList = faqReplaceAllForTour(tourFaqRequestList, tourUpdate);

            return tourMapper.toTourFullDataResponse(tourUpdate, tourAddressResponseList,
                    tourMainAttractionResponseList, tourIncludesResponseList, tourExcludesResponseList, tourFaqResponseList);

        }else{
            throw new ResourceNotFoundException("Tour not found with id = "+tourId+" to providerId = "+provider.getId());
        }
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
    public TourFullDataResponse consultDataTourById(Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());
            if(tour != null){
                return tourMapper.toTourFullDataResponse(tour, consultDataTourAddressListByTourId(tourId),
                        getAllByTourMainAttractions(tourId), getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.INCLUDE),
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId));
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
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId));
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
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId));
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
                        getAllByTourIncludesExcludes(tourId, IncludeExcludeTypeEnum.EXCLUDE), getAllByTourFaqs(tourId));
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
}
