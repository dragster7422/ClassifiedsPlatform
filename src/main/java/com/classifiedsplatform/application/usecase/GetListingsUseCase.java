package com.classifiedsplatform.application.usecase;

import com.classifiedsplatform.application.port.in.GetListingsQuery;
import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.domain.model.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetListingsUseCase {

    private final ListingRepository listingRepository;

    public GetListingsUseCase(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Page<Listing> execute(GetListingsQuery query) {
        Pageable pageable = createPageable(query);

        return listingRepository.findByFilters(
                query.query(),
                query.category(),
                query.status(),
                query.minPrice(),
                query.maxPrice(),
                pageable
        );
    }

    private Pageable createPageable(GetListingsQuery query) {
        Sort sort = createSort(query.sortBy(), query.sortDirection());
        return PageRequest.of(query.page(), query.size(), sort);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        String field = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }
}