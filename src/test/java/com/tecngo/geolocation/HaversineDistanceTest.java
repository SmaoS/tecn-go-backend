package com.tecngo.geolocation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineDistanceTest {
    private final HaversineDistance distance = new HaversineDistance();

    @Test
    void calculatesKnownDistanceBetweenBogotaPoints() {
        double kilometers = distance.kilometers(4.7110, -74.0721, 4.6097, -74.0817);

        assertThat(kilometers).isBetween(11.0, 11.5);
    }
}
