package com.classifiedsplatform.domain.exception;

import com.classifiedsplatform.domain.model.vo.ListingStatus;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(ListingStatus currentStatus, ListingStatus newStatus) {
        super(String.format("Cannot transition from %s to %s", currentStatus, newStatus));
    }
}