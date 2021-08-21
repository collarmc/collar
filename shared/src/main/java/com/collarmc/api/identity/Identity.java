package com.collarmc.api.identity;

import com.collarmc.security.PublicKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public interface Identity {
    @JsonIgnore
    UUID id();

    // TODO: move this to the identify packet to reduce size of frame
    @JsonIgnore
    PublicKey publicKey();

    // TODO: move this to the identify packet to reduce size of frame
    @JsonIgnore
    PublicKey signatureKey();
}
