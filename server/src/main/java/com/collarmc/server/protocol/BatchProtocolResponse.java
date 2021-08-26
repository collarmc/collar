package com.collarmc.server.protocol;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.protocol.ProtocolResponse;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class BatchProtocolResponse extends ProtocolResponse {
    public final Map<ProtocolResponse, ClientIdentity> responses;

    public BatchProtocolResponse(Map<ProtocolResponse, ClientIdentity> responses) {
        this.responses = responses;
    }

    public BatchProtocolResponse() {
        this(new LinkedHashMap<>());
    }

    public BatchProtocolResponse add(ClientIdentity identity, ProtocolResponse response) {
        responses.put(response, identity);
        return this;
    }

    public BatchProtocolResponse concat(BatchProtocolResponse response) {
        responses.putAll(response.responses);
        return this;
    }

    public static BatchProtocolResponse one(ClientIdentity identity, ProtocolResponse response) {
        return new BatchProtocolResponse(ImmutableMap.of(response, identity));
    }

    public Optional<BatchProtocolResponse> optional() {
        return responses.isEmpty() ? Optional.empty() : Optional.of(this);
    }
}
