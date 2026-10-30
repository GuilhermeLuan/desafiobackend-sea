package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageTest {

    @Test
    void restrictedCoverageCoversOnlyItsUfs() {
        Coverage coverage = Coverage.of(Set.of("SP", "DF"));

        assertTrue(coverage.covers("SP"));
        assertTrue(coverage.covers("DF"));
        assertFalse(coverage.covers("RJ"));
    }

    @Test
    void restrictedCoverageIsNotUnrestricted() {
        assertFalse(Coverage.of(Set.of("SP")).isUnrestricted());
    }

    @Test
    void unrestrictedCoverageCoversEveryUf() {
        Coverage coverage = Coverage.unrestricted();

        assertTrue(coverage.isUnrestricted());
        assertTrue(coverage.covers("SP"));
        assertTrue(coverage.covers("TO"));
    }

    @Test
    void emptyCoverageCoversNothingAndIsNotUnrestricted() {
        Coverage coverage = Coverage.of(Set.of());

        assertFalse(coverage.isUnrestricted());
        assertFalse(coverage.covers("SP"));
    }

    @Test
    void nullUfSetIsTreatedAsEmptyCoverageNotAsUnrestricted() {
        Coverage coverage = Coverage.of(null);

        assertFalse(coverage.isUnrestricted());
        assertFalse(coverage.covers("SP"));
    }

    @Test
    void comparisonIgnoresCase() {
        Coverage coverage = Coverage.of(Set.of("sp"));

        assertTrue(coverage.covers("SP"));
        assertTrue(coverage.covers("sp"));
    }

    @Test
    void nullOrBlankUfIsNeverCovered() {
        assertFalse(Coverage.of(Set.of("SP")).covers(null));
        assertFalse(Coverage.of(Set.of("SP")).covers(" "));
        assertFalse(Coverage.unrestricted().covers(null));
    }

    @Test
    void ufsReturnsNormalizedCoverage() {
        assertEquals(Set.of("SP", "DF"), Coverage.of(Set.of("sp", "DF")).ufs());
    }

    @Test
    void ufsIsNotAffectedByLaterChangesToTheSourceSet() {
        Set<String> source = new HashSet<>(Set.of("SP"));
        Coverage coverage = Coverage.of(source);

        source.add("RJ");

        assertFalse(coverage.covers("RJ"));
        assertEquals(Set.of("SP"), coverage.ufs());
    }

    @Test
    void ufsCannotBeModified() {
        Set<String> ufs = Coverage.of(Set.of("SP")).ufs();

        assertThrows(UnsupportedOperationException.class, () -> ufs.add("RJ"));
    }

    @Test
    void unrestrictedCoverageHasNoUfList() {
        assertThrows(IllegalStateException.class, () -> Coverage.unrestricted().ufs());
    }
}
