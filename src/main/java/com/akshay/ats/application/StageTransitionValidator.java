package com.akshay.ats.application;

import com.akshay.ats.common.InvalidStageTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for which stage transitions are legal in the hiring pipeline.
 *
 * Forward flow:  APPLIED -> SCREENING -> INTERVIEW -> OFFER -> HIRED
 * Backward flow: SCREENING -> APPLIED, INTERVIEW -> SCREENING  (sending a candidate back a step)
 * Rejection is handled separately via ApplicationStatus, not as a Stage transition.
 */
@Component
public class StageTransitionValidator {

    private static final Map<Stage, Set<Stage>> ALLOWED_TRANSITIONS = new EnumMap<>(Stage.class);

    static {
        ALLOWED_TRANSITIONS.put(Stage.APPLIED, EnumSet.of(Stage.SCREENING));
        ALLOWED_TRANSITIONS.put(Stage.SCREENING, EnumSet.of(Stage.INTERVIEW, Stage.APPLIED));
        ALLOWED_TRANSITIONS.put(Stage.INTERVIEW, EnumSet.of(Stage.OFFER, Stage.SCREENING));
        ALLOWED_TRANSITIONS.put(Stage.OFFER, EnumSet.of(Stage.HIRED));
        ALLOWED_TRANSITIONS.put(Stage.HIRED, EnumSet.noneOf(Stage.class)); // terminal
    }

    /**
     * Throws InvalidStageTransitionException if moving from {@code from} to {@code to} is not allowed.
     */
    public void validate(Stage from, Stage to) {
        if (from == to) {
            throw new InvalidStageTransitionException(
                    "Application is already at stage " + from);
        }

        Set<Stage> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowedNext.contains(to)) {
            throw new InvalidStageTransitionException(
                    "Cannot move from " + from + " to " + to +
                            ". Allowed next stages from " + from + " are: " + allowedNext);
        }
    }

    public boolean isValid(Stage from, Stage to) {
        if (from == to) return false;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
