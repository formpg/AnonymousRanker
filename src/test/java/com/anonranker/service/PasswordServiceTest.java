package com.anonranker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashAndMatchRoundTrip() {
        String hash = passwordService.hash("hunter2");

        assertThat(passwordService.matches("hunter2", hash)).isTrue();
        assertThat(passwordService.matches("wrong", hash)).isFalse();
    }

    @Test
    void sameRawPasswordProducesDifferentHashes() {
        String hash1 = passwordService.hash("hunter2");
        String hash2 = passwordService.hash("hunter2");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
