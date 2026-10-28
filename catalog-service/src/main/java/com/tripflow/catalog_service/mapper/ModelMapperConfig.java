package com.tripflow.catalog_service.mapper;

import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.model.Trip;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Trip -> TripResponseDTO:
        // availableSpots nel DTO = posti totali - posti già prenotati
        // totalSpots nel DTO     = posti totali dichiarati dall'organizzatore
        Converter<Trip, TripResponseDTO> tripConverter = ctx -> {
            Trip src = ctx.getSource();
            TripResponseDTO dst = ctx.getDestination();

            int booked = src.getBookedSpots() != null ? src.getBookedSpots() : 0;

            dst.setAvailableSpots(src.getAvailableSpots() - booked);
            dst.setTotalSpots(src.getAvailableSpots());

            return dst;
        };

        modelMapper.createTypeMap(Trip.class, TripResponseDTO.class)
                .setPostConverter(tripConverter);

        return modelMapper;
    }
}
