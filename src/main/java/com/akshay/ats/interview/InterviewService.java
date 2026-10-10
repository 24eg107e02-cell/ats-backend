package com.akshay.ats.interview;

import com.akshay.ats.application.Application;
import com.akshay.ats.application.ApplicationRepository;
import com.akshay.ats.common.InvalidStageTransitionException;
import com.akshay.ats.common.ResourceNotFoundException;
import com.akshay.ats.interview.dto.FeedbackRequest;
import com.akshay.ats.interview.dto.InterviewRequest;
import com.akshay.ats.interview.dto.InterviewResponse;
import com.akshay.ats.user.User;
import com.akshay.ats.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public InterviewResponse schedule(Long applicationId, InterviewRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        User interviewer = userRepository.findById(request.interviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found with id: " + request.interviewerId()));

        if (!request.scheduledEnd().isAfter(request.scheduledStart())) {
            throw new IllegalArgumentException("scheduledEnd must be after scheduledStart");
        }

        // Conflict detection: reject if the interviewer already has an overlapping scheduled interview
        List<Interview> overlaps = interviewRepository.findOverlapping(
                interviewer.getId(), request.scheduledStart(), request.scheduledEnd());
        if (!overlaps.isEmpty()) {
            throw new InvalidStageTransitionException(
                    "Interviewer already has a scheduled interview overlapping this time slot");
        }

        Interview interview = Interview.builder()
                .application(application)
                .interviewer(interviewer)
                .scheduledStart(request.scheduledStart())
                .scheduledEnd(request.scheduledEnd())
                .mode(request.mode())
                .locationOrLink(request.locationOrLink())
                .status(InterviewStatus.SCHEDULED)
                .build();

        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse reschedule(Long id, InterviewRequest request) {
        Interview interview = findEntity(id);

        List<Interview> overlaps = interviewRepository.findOverlapping(
                request.interviewerId(), request.scheduledStart(), request.scheduledEnd());
        overlaps.removeIf(i -> i.getId().equals(id)); // ignore itself
        if (!overlaps.isEmpty()) {
            throw new InvalidStageTransitionException(
                    "Interviewer already has a scheduled interview overlapping this time slot");
        }

        interview.setScheduledStart(request.scheduledStart());
        interview.setScheduledEnd(request.scheduledEnd());
        interview.setMode(request.mode());
        interview.setLocationOrLink(request.locationOrLink());

        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse submitFeedback(Long id, FeedbackRequest request) {
        Interview interview = findEntity(id);
        interview.setFeedback(request.feedback());
        interview.setRating(request.rating());
        interview.setStatus(InterviewStatus.COMPLETED);
        return InterviewResponse.from(interview);
    }

    @Transactional
    public void cancel(Long id) {
        Interview interview = findEntity(id);
        interview.setStatus(InterviewStatus.CANCELLED);
    }

    private Interview findEntity(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }
}
