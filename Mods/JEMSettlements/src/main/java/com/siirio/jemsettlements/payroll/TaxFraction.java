package com.siirio.jemsettlements.payroll;

import java.math.BigInteger;

/** Exact daily income arithmetic; daily remainder is persisted with the town. */
public record TaxFraction(long numerator, long denominator) {
    public static final TaxFraction ZERO = new TaxFraction(0, 1);
    public TaxFraction {
        if (numerator < 0 || denominator <= 0) throw new IllegalArgumentException("Invalid rational price");
        long gcd = BigInteger.valueOf(numerator).gcd(BigInteger.valueOf(denominator)).longValueExact();
        numerator /= gcd; denominator /= gcd;
    }
    public TaxFraction add(TaxFraction other) {
        BigInteger a = BigInteger.valueOf(numerator), b = BigInteger.valueOf(denominator);
        BigInteger c = BigInteger.valueOf(other.numerator), d = BigInteger.valueOf(other.denominator);
        BigInteger n = a.multiply(d).add(c.multiply(b)), den = b.multiply(d), gcd = n.gcd(den);
        return new TaxFraction(n.divide(gcd).longValueExact(), den.divide(gcd).longValueExact());
    }
    public TaxFraction multiply(long factor) {
        if (factor < 0) throw new IllegalArgumentException("Negative quantity");
        BigInteger n = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(factor));
        BigInteger d = BigInteger.valueOf(denominator), gcd = n.gcd(d);
        return new TaxFraction(n.divide(gcd).longValueExact(), d.divide(gcd).longValueExact());
    }
    public TaxFraction divide(long factor) {
        if (factor <= 0) throw new IllegalArgumentException("Invalid divisor");
        BigInteger n = BigInteger.valueOf(numerator), d = BigInteger.valueOf(denominator).multiply(BigInteger.valueOf(factor));
        BigInteger gcd = n.gcd(d);
        return new TaxFraction(n.divide(gcd).longValueExact(), d.divide(gcd).longValueExact());
    }
    public long floor() { return numerator / denominator; }
    public long ceil() { return Math.addExact(floor(), numerator % denominator == 0 ? 0 : 1); }
    public TaxFraction remainder() { return new TaxFraction(numerator % denominator, denominator); }
}
