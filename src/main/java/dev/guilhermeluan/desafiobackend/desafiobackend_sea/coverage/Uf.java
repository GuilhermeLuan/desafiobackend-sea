package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import java.util.Arrays;

public enum Uf {
    AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA,
    PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO;

    public static boolean isValid(String value) {
        return value != null && Arrays.stream(values()).anyMatch(uf -> uf.name().equals(value));
    }
}
