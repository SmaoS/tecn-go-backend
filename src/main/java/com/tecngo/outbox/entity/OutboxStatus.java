package com.tecngo.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    DEAD
}
