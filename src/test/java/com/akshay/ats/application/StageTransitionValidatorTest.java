package com.akshay.ats.application;

import com.akshay.ats.common.InvalidStageTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StageTransitionValidatorTest {

    private final StageTransitionValidator validator = new StageTransitionValidator();

    @ParameterizedTest(name = "{0} -> {1} is legal")
    @CsvSource({
            "APPLIED, SCREENING",
            "SCREENING, INTERVIEW",
            "SCREENING, APPLIED",
            "INTERVIEW, OFFER",
            "INTERVIEW, SCREENING",
            "OFFER, HIRED"
    })
    @DisplayName("Legal transitions do not throw")
    void legalTransitionsAreAllowed(Stage from, Stage to) {
        assertThat(validator.isValid(from, to)).isTrue();
        // should not throw
        validator.validate(from, to);
    }

    @ParameterizedTest(name = "{0} -> {1} is illegal")
    @CsvSource({
            "APPLIED, INTERVIEW",
            "APPLIED, OFFER",
            "APPLIED, HIRED",
            "SCREENING, OFFER",
            "SCREENING, HIRED",
            "INTERVIEW, APPLIED",
            "INTERVIEW, HIRED",
            "OFFER, APPLIED",
            "OFFER, SCREENING",
            "OFFER, INTERVIEW",
            "HIRED, APPLIED",
            "HIRED, OFFER"
    })
    @DisplayName("Illegal transitions throw InvalidStageTransitionException")
    void illegalTransitionsAreRejected(Stage from, Stage to) {
        assertThat(validator.isValid(from, to)).isFalse();
        assertThatThrownBy(() -> validator.validate(from, to))
                .isInstanceOf(InvalidStageTransitionException.class);
    }

    @Test
    @DisplayName("HIRED is a terminal stage with no outgoing transitions")
    void hiredIsTerminal() {
        for (Stage target : Stage.values()) {
            assertThat(validator.isValid(Stage.HIRED, target)).isFalse();
        }
    }

    @Test
    @DisplayName("Transitioning to the same stage is rejected")
    void sameStageTransitionIsRejected() {
        for (Stage stage : Stage.values()) {
            assertThatThrownBy(() -> validator.validate(stage, stage))
                    .isInstanceOf(InvalidStageTransitionException.class);
        }
    }

    @Test
    @DisplayName("Cannot skip directly from APPLIED to OFFER")
    void cannotSkipApplicationStages() {
        assertThatThrownBy(() -> validator.validate(Stage.APPLIED, Stage.OFFER))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("Cannot move from APPLIED to OFFER");
    }
}
