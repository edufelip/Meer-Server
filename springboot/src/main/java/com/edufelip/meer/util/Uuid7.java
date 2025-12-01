package com.edufelip.meer.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Minimal UUID v7 generator (Unix time in ms + random), per draft RFC 4122bis.
 * Good enough for app-level IDs; not cryptographically strong.
 */
public final class Uuid7 {
    private Uuid7() {}

    public static UUID next() {
        long now = System.currentTimeMillis();
        long randA = ThreadLocalRandom.current().nextLong();
        long randB = ThreadLocalRandom.current().nextLong();

        long msb = (now & 0xFFFFFFFFFFFFL) << 16; // 48 bits of timestamp
        msb |= 0x7000; // version 7 in bits 12-15
        msb |= (randA & 0x0FFF); // 12 random bits

        long lsb = randB & 0x3FFFFFFFFFFFFFFFL; // clear variant
        lsb |= 0x8000000000000000L; // set IETF variant 10xx

        return new UUID(msb, lsb);
    }
}
