package com.classifiedsplatform.application.usecase;

import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.domain.exception.ListingNotFoundException;
import com.classifiedsplatform.domain.model.Listing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetListingDetailsUseCase {

    private final ListingRepository listingRepository;

    public GetListingDetailsUseCase(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Listing execute(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
    }
}