package org.petspa.petcaresystem.serviceAppointment.service.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.petspa.petcaresystem.authenuser.model.payload.AuthenUser;
import org.petspa.petcaresystem.authenuser.model.response.ResponseAPI;
import org.petspa.petcaresystem.authenuser.repository.AuthenUserRepository;
import org.petspa.petcaresystem.authenuser.service.AuthenUserService;
import org.petspa.petcaresystem.enums.Gender;
import org.petspa.petcaresystem.enums.Status;
import org.petspa.petcaresystem.serviceAppointment.model.Combo;
import org.petspa.petcaresystem.serviceAppointment.model.ServiceType;
import org.petspa.petcaresystem.serviceAppointment.model.Services;
import org.petspa.petcaresystem.serviceAppointment.model.request.ServiceRatingRequestDTO;
import org.petspa.petcaresystem.serviceAppointment.model.request.ServiceRevenueRequestDTO;
import org.petspa.petcaresystem.serviceAppointment.model.response.RevenueResponseAPI;
import org.petspa.petcaresystem.serviceAppointment.model.response.ServiceResponseAPI;
import org.petspa.petcaresystem.serviceAppointment.repository.ComboRepository;
import org.petspa.petcaresystem.serviceAppointment.repository.ServiceTypeRepository;
import org.petspa.petcaresystem.serviceAppointment.repository.ServicesRepository;
import org.petspa.petcaresystem.serviceAppointment.service.ServiceAndComboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ServiceAndComboServiceImpl implements ServiceAndComboService{

    private static final String format_pattern = "yyyy-MM-dd HH:mm";
    private static final String logging_message = "An error occurred:";
    private static final Logger logger = LoggerFactory.getLogger(AuthenUserService.class);

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private ComboRepository comboRepository;

    //Services service
    @Override
    public Collection<Services> findAllService() {
        return servicesRepository.findAll();
    }

    @Override
    public Services findServiceById(Long serviceId) {
        return servicesRepository.findById(serviceId).orElse(null);
    }

    @Override
    public Services saveService(Services service) {
        return servicesRepository.save(service);
    }

    public Services saveServiceNoType(Services service, Long typeId) {
        ServiceType serviceType = serviceTypeRepository.findById(typeId).orElse(null);
        Collection<ServiceType> list = new ArrayList<ServiceType>();
        list.add(serviceType);
        service.setTypeOfService(list);
        return servicesRepository.save(service);
    }

    @Override
    public Services updateService(Services service) {
        return servicesRepository.save(service);
    }

    @Override
    public Services deleteService(Long serviceId) {
        Services service = servicesRepository.findById(serviceId).orElse(null);
        service.setStatus(Status.INACTIVE);
        return servicesRepository.save(service);
    }

    @Override
    public List<Services> searchService(String searchTerm) {
       return servicesRepository.searchByServiceName(searchTerm);
    }

    @Override
    public ServiceResponseAPI searchServiceTEST(String searchTerm, String typeList, float minPrice, float maxPrice, Status status, String orderBy, String order) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format_pattern);
        String timeStamp = localDateTime.format(formatter);
        String message = "Get user successfully";
        int statusCode = HttpStatus.OK.value();
        HttpStatus statusValue = HttpStatus.OK;
        List<String> stringList = new ArrayList<>();
        String[] splitArray = typeList.split(",");
        for (int i = 0; i < splitArray.length; i++) {
            splitArray[i] = splitArray[i].trim();
            stringList.add(splitArray[i]);
        }
        List<Services> serviceList = new ArrayList<>();
        try {
            String sql = "SELECT DISTINCT s.service_id, s.service_name, s.description, s.discount_percent, s.price, s.status FROM pet_spa.services s JOIN pet_spa.type_service ts ON s.service_id = ts.service_id JOIN pet_spa.service_type st ON ts.service_type_id = st.service_type_id WHERE s.service_name LIKE :searchTerm AND ((:minPrice = 0 OR :maxPrice = 0) OR s.price BETWEEN :minPrice AND :maxPrice) AND (:status IS NULL OR s.status = :status)";
            if (stringList != null && !stringList.isEmpty()) {
                // String inClause = String.join(",", Collections.nCopies(typeList.size(), "?"));
                String test = " AND st.type_name IN (";
                String test2 = "";
                for(int i = 1;i<=stringList.size();i++){
                    if(i == 1){
                        test2 += "?"+String.valueOf(i);
                    }
                    else{
                        test2 += ",?"+String.valueOf(i);
                    }
                }
                test += test2 + ")";
                sql += test;
            }
            String sql2 = " ORDER BY " + orderBy + " " + order;
            sql += sql2;
            System.out.println("sql statement: " + sql);
            Query query = entityManager.createNativeQuery(sql, Services.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            int paramIndex = 1; // Start index for parameters
            if (stringList != null && !stringList.isEmpty()) {
                for (String type : stringList) {
                    query.setParameter(paramIndex++, type);
                }
            }
            query.setParameter("minPrice", 0);
            query.setParameter("maxPrice", 0);
            if(minPrice != 0) {
                query.setParameter("minPrice", minPrice);
            }
            if(maxPrice != 0){
                query.setParameter("maxPrice", maxPrice);
            }
            query.setParameter("status", null);
            if (status != null) {
                query.setParameter("status", status.toString().toUpperCase());
            }
            serviceList = query.getResultList();
        }catch (Exception e){
            logger.error(this.logging_message, e);
            message = "Something went wrong, server error!";
            statusValue = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ServiceResponseAPI(timeStamp, message, statusCode, statusValue, serviceList);
   }

   @Override
   public ServiceResponseAPI findAverageRatingService(Long service_id) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format_pattern);
        String timeStamp = localDateTime.format(formatter);
        String message = "Get user successfully";
        int statusCode = HttpStatus.OK.value();
        HttpStatus statusValue = HttpStatus.OK;
        ServiceRatingRequestDTO serviceRatingRequestDTO = new ServiceRatingRequestDTO();
        try {
            String sql = "SELECT AVG(r.rating) AS average_rating FROM pet_spa.services s JOIN pet_spa.service_booked sb ON s.service_id = sb.service_id JOIN pet_spa.appointment a ON sb.appointment_id = a.appointment_id JOIN pet_spa.review r ON a.review_id = r.review_id WHERE s.service_id = :service_id GROUP BY s.service_id LIMIT 1";
            Query query = entityManager.createNativeQuery(sql, Float.class);
            query.setParameter("service_id", service_id);
            String sql2 = "SELECT * FROM pet_spa.services WHERE service_id = :id LIMIT 1";
            Query query2 = entityManager.createNamedQuery(sql2, Services.class);
            query2.setParameter("id", service_id);
            serviceRatingRequestDTO.setServices((Services) query2.getSingleResult());
            serviceRatingRequestDTO.setRating((Float) query.getSingleResult());
        } catch (Exception e){
            logger.error(this.logging_message, e);
            message = "Something went wrong, server error!";
            statusValue = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ServiceResponseAPI(timeStamp, message, statusCode, statusValue, serviceRatingRequestDTO);
   }

   @Override
   public RevenueResponseAPI findServiceRevenue(LocalDate startTime, LocalDate endTime) {

        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format_pattern);
        String timeStamp = localDateTime.format(formatter);
        String message = "Get user successfully";
        int statusCode = HttpStatus.OK.value();
        HttpStatus statusValue = HttpStatus.OK;
        List<ServiceRevenueRequestDTO> serviceRevenueRequestDTO = new ArrayList<>();
        try {
            serviceRevenueRequestDTO = servicesRepository.findServiceRevenue(startTime, endTime);
        } catch (Exception e){
            logger.error(this.logging_message, e);
            message = "Something went wrong, server error!";
            statusValue = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new RevenueResponseAPI(timeStamp, message, statusCode, statusValue, serviceRevenueRequestDTO);
   }

   @Override
   public RevenueResponseAPI findMostUsedService(LocalDate startTime, LocalDate endTime) {

        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format_pattern);
        String timeStamp = localDateTime.format(formatter);
        String message = "Get user successfully";
        int statusCode = HttpStatus.OK.value();
        HttpStatus statusValue = HttpStatus.OK;
        List<ServiceRevenueRequestDTO> serviceRevenueRequestDTO = new ArrayList<>();
        try {
            serviceRevenueRequestDTO = servicesRepository.findMostUsedService(startTime, endTime);
        } catch (Exception e){
            logger.error(this.logging_message, e);
            message = "Something went wrong, server error!";
            statusValue = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new RevenueResponseAPI(timeStamp, message, statusCode, statusValue, serviceRevenueRequestDTO);
   }

    //Type service
    @Override
    public List<Services> searchServiceByType(String typeName) {
        List<ServiceType> serviceType = serviceTypeRepository.findByTypeName(typeName);
        String typeId = String.valueOf(serviceType.get(0).getServiceTypeId());

        List<Long> serviceId = servicesRepository.searchByServiceType(typeId);

        List<Services> serviceList = new ArrayList<>();
        for(int i = 0; i<serviceId.size(); i++) {
            serviceList.add(servicesRepository.findById(serviceId.get(i)).orElse(null));
        }
        return serviceList;
    }

    @Override
    public Collection<ServiceType> findAllServiceType() {
        return serviceTypeRepository.findAll();
    }

    @Override
    public ServiceType saveServiceType(ServiceType serviceType) {
        return serviceTypeRepository.save(serviceType);
    }

    //Combo service
    @Override
    public Collection<Combo> findAllCombo() {
        return comboRepository.findAll();
    }

    @Override
    public Combo findComboById(Long comboId) {
        return comboRepository.findById(comboId).orElse(null);
    }

    @Override
    public Combo saveCombo(Combo combo) {
        return comboRepository.save(combo);
    }

    @Override
    public Combo updateCombo(Combo combo) {
        return comboRepository.save(combo);
    }

    @Override
    public Combo deleteCombo(Long comboId) {
        Combo combo = comboRepository.findById(comboId).orElse(null);
        combo.setStatus(Status.INACTIVE);
        return comboRepository.save(combo);
    }

}
