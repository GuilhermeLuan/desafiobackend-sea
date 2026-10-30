package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UfTest {

    @Test
    void hasTheTwentySevenBrazilianUfs() {
        assertEquals(27, Uf.values().length);
    }

    @Test
    void acceptsOnlyUppercaseUfAcronyms() {
        assertTrue(Uf.isValid("SP"));
        assertTrue(Uf.isValid("TO"));
        assertFalse(Uf.isValid("sp"));
        assertFalse(Uf.isValid("XX"));
        assertFalse(Uf.isValid(" SP"));
        assertFalse(Uf.isValid(""));
        assertFalse(Uf.isValid(null));
    }
}
