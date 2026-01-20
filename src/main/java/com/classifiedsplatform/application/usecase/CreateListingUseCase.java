package com.classifiedsplatform.application.usecase;

import com.classifiedsplatform.application.port.in.CreateListingCommand;
import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.domain.model.Listing;
import com.classifiedsplatform.domain.model.vo.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateListingUseCase {

    private final ListingRepository listingRepository;

    public CreateListingUseCase(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Listing execute(CreateListingCommand command) {
        Money price = Money.of(command.priceAmount(), command.priceCurrency());

        Listing listing = Listing.create(
                command.title(),
                command.description(),
                price,
                command.category()
        );

        return listingRepository.save(listing);
    }
}