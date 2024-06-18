/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;


public enum OrderStatus {
	ApiPending,
	ApiCancelled,
	PreSubmitted,
	PendingCancel,
	Cancelled,
	Inactive,
	Submitted,
	Filled,
	PendingSubmit,
	Timeout,
	Unknown;

    public static OrderStatus get(String apiString) {
        for( OrderStatus type : values() ) {
            if( type.name().equalsIgnoreCase(apiString) ) {
                return type;
            }
        }
        return Unknown;
    }

    public boolean isCanceled() {
    	return this == ApiCancelled || this == PendingCancel || this == Cancelled;
    }    
    
	public boolean isActive() {
		return this == PreSubmitted || this == PendingCancel || this == Submitted || this == PendingSubmit;
	}

	public boolean isComplete() {
		return this == Cancelled || this == Filled;
	}

	boolean canCancel() {
		return this == ApiPending || this == PreSubmitted || this == Submitted || this == PendingSubmit;
	}
}
