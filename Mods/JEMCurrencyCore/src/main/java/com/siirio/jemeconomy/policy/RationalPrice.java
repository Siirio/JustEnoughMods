package com.siirio.jemeconomy.policy;

import java.math.BigInteger;

/** Exact checked price arithmetic. Rounding belongs to the complete transaction. */
public record RationalPrice(long numerator, long denominator) {
    public static final RationalPrice ZERO = new RationalPrice(0, 1);
    public RationalPrice {
        if (numerator < 0 || denominator <= 0) throw new IllegalArgumentException("Invalid rational price");
        long gcd = BigInteger.valueOf(numerator).gcd(BigInteger.valueOf(denominator)).longValueExact();
        numerator /= gcd; denominator /= gcd;
    }
    public RationalPrice add(RationalPrice other) {
        BigInteger a = BigInteger.valueOf(numerator), b = BigInteger.valueOf(denominator);
        BigInteger c = BigInteger.valueOf(other.numerator), d = BigInteger.valueOf(other.denominator);
        BigInteger n = a.multiply(d).add(c.multiply(b)), den = b.multiply(d), gcd = n.gcd(den);
        return new RationalPrice(n.divide(gcd).longValueExact(), den.divide(gcd).longValueExact());
    }
    public RationalPrice multiply(long factor) {
        if (factor < 0) throw new IllegalArgumentException("Negative quantity");
        BigInteger n = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(factor));
        BigInteger d = BigInteger.valueOf(denominator), gcd = n.gcd(d);
        return new RationalPrice(n.divide(gcd).longValueExact(), d.divide(gcd).longValueExact());
    }
    public RationalPrice divide(long factor) {
        if (factor <= 0) throw new IllegalArgumentException("Invalid divisor");
        BigInteger n = BigInteger.valueOf(numerator), d = BigInteger.valueOf(denominator).multiply(BigInteger.valueOf(factor));
        BigInteger gcd = n.gcd(d);
        return new RationalPrice(n.divide(gcd).longValueExact(), d.divide(gcd).longValueExact());
    }
    public long floor() { return numerator / denominator; }
    public long ceil() { return Math.addExact(floor(), numerator % denominator == 0 ? 0 : 1); }
    public RationalPrice remainder() { return new RationalPrice(numerator % denominator, denominator); }
}
