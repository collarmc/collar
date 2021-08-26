package com.collarmc.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "t")
public abstract class ProtocolRequest {}
