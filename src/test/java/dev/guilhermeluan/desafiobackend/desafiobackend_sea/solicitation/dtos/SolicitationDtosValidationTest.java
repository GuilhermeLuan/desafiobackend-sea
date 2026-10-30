package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitationDtosValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String VALID_DESCRIPTION = "Instalação de um split na sala de estar";

    private static boolean valid(Object dto) {
        return validator.validate(dto).isEmpty();
    }

    // etapa 1

    @Test
    void basicDataAcceptsValidValues() {
        assertTrue(valid(new BasicDataRequest(ServiceType.INSTALLATION, "Instalar ar", VALID_DESCRIPTION)));
    }

    @Test
    void basicDataRequiresServiceType() {
        assertEquals(false, valid(new BasicDataRequest(null, "Instalar ar", VALID_DESCRIPTION)));
    }

    @Test
    void titleMustHaveBetweenThreeAndEightyCharactersAfterTrim() {
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "ab", VALID_DESCRIPTION)));
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "  ab  ", VALID_DESCRIPTION)));
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "a".repeat(81), VALID_DESCRIPTION)));
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "   ", VALID_DESCRIPTION)));
        assertTrue(valid(new BasicDataRequest(ServiceType.INSPECTION, "abc", VALID_DESCRIPTION)));
        assertTrue(valid(new BasicDataRequest(ServiceType.INSPECTION, "a".repeat(80), VALID_DESCRIPTION)));
    }

    @Test
    void descriptionMustHaveBetweenTwentyAndOneThousandCharactersAfterTrim() {
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "Vistoria", "a".repeat(19))));
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "Vistoria", "  " + "a".repeat(19) + "  ")));
        assertEquals(false, valid(new BasicDataRequest(ServiceType.INSPECTION, "Vistoria", "a".repeat(1001))));
        assertTrue(valid(new BasicDataRequest(ServiceType.INSPECTION, "Vistoria", "a".repeat(20))));
        assertTrue(valid(new BasicDataRequest(ServiceType.INSPECTION, "Vistoria", "a".repeat(1000))));
    }

    @Test
    void basicDataTrimsTitleAndDescription() {
        BasicDataRequest request = new BasicDataRequest(ServiceType.INSPECTION, "  Vistoria  ", "  " + VALID_DESCRIPTION + "  ");

        assertEquals("Vistoria", request.title());
        assertEquals(VALID_DESCRIPTION, request.description());
    }

    // etapa 2

    @Test
    void addressAcceptsCepWithOrWithoutHyphenAndNormalizesIt() {
        assertEquals("01310100", new AddressRequest("01310-100", "1578", null).cep());
        assertTrue(valid(new AddressRequest("01310-100", "1578", null)));
        assertTrue(valid(new AddressRequest("01310100", "12A", "Sala 12")));
    }

    @Test
    void addressRejectsMalformedCep() {
        assertEquals(false, valid(new AddressRequest("1234567", "10", null)));
        assertEquals(false, valid(new AddressRequest("abcdefgh", "10", null)));
        assertEquals(false, valid(new AddressRequest("", "10", null)));
        assertEquals(false, valid(new AddressRequest(null, "10", null)));
    }

    @Test
    void addressNumberIsRequiredWithUpToTwentyCharacters() {
        assertEquals(false, valid(new AddressRequest("01310100", " ", null)));
        assertEquals(false, valid(new AddressRequest("01310100", null, null)));
        assertEquals(false, valid(new AddressRequest("01310100", "1".repeat(21), null)));
        assertTrue(valid(new AddressRequest("01310100", "1".repeat(20), null)));
    }

    @Test
    void blankComplementBecomesNull() {
        assertNull(new AddressRequest("01310100", "10", "   ").complement());
    }

    // etapa 3

    private static ConfirmationRequest confirmation(Priority priority, LocalDate date, BigDecimal value, boolean terms) {
        return new ConfirmationRequest(priority, date, value, terms);
    }

    @Test
    void confirmationAcceptsValidValues() {
        assertTrue(valid(confirmation(Priority.MEDIUM, LocalDate.now().plusDays(1), new BigDecimal("250.00"), true)));
    }

    @Test
    void confirmationRequiresPriorityAndEstimatedValue() {
        assertEquals(false, valid(confirmation(null, LocalDate.now(), BigDecimal.ONE, true)));
        assertEquals(false, valid(confirmation(Priority.LOW, LocalDate.now(), null, true)));
    }

    @Test
    void preferredDateCannotBeInThePastButTodayIsAllowed() {
        assertEquals(false, valid(confirmation(Priority.LOW, LocalDate.now().minusDays(1), BigDecimal.ONE, true)));
        assertTrue(valid(confirmation(Priority.LOW, LocalDate.now(), BigDecimal.ONE, true)));
    }

    @Test
    void estimatedValueCannotBeNegativeButZeroIsAllowed() {
        assertEquals(false, valid(confirmation(Priority.LOW, LocalDate.now(), new BigDecimal("-0.01"), true)));
        assertTrue(valid(confirmation(Priority.LOW, LocalDate.now(), BigDecimal.ZERO, true)));
    }

    @Test
    void estimatedValueAcceptsAtMostTwoDecimals() {
        assertEquals(false, valid(confirmation(Priority.LOW, LocalDate.now(), new BigDecimal("10.123"), true)));
        assertTrue(valid(confirmation(Priority.LOW, LocalDate.now(), new BigDecimal("10.12"), true)));
    }

    @Test
    void termsMustBeAccepted() {
        assertEquals(false, valid(confirmation(Priority.LOW, LocalDate.now(), BigDecimal.ONE, false)));
    }
}
