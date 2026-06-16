package com.tecngo.legal.dto;
import java.util.List;
public record LegalStatusResponse(boolean complete, List<LegalDocumentResponse> pending,
                                  List<LegalDocumentResponse> accepted,
                                  boolean acceptedCurrentVersion,
                                  boolean newRequiredVersionPending) {}
