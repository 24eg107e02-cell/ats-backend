package com.akshay.ats.chat;

import com.akshay.ats.application.Application;
import com.akshay.ats.application.ApplicationRepository;
import com.akshay.ats.job.Job;
import com.akshay.ats.job.JobRepository;
import com.akshay.ats.job.JobSpecifications;
import com.akshay.ats.job.JobStatus;
import com.akshay.ats.user.Role;
import com.akshay.ats.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based chatbot. No external AI service: answers come from keyword rules plus live data
 * from the database, and resume matching is a transparent skill-overlap score.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    // ---- response shapes ----
    public record JobCard(Long id, String title, String location, String employmentType,
                          int minExperience, String skills) {}

    public record ChatReply(String reply, List<JobCard> jobs, List<String> suggestions) {}

    public record JobMatch(Long jobId, String title, String location, String employmentType,
                           int minExperience, int matchPercent,
                           List<String> matchedSkills, List<String> missingSkills, String note) {}

    public record ResumeMatchResponse(List<String> detectedSkills, Integer estimatedYears,
                                      List<JobMatch> matches, String message) {}

    private static final int MAX_CARDS = 5;
    private static final Pattern YEARS = Pattern.compile("(\\d{1,2})(?:\\.\\d)?\\s*\\+?\\s*(?:years?|yrs?)");
    private static final List<String> DEFAULT_SUGGESTIONS =
            List.of("Show open jobs", "How do I apply?", "What are the hiring stages?", "Jobs for freshers");

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillCatalog skills;
    private final ResumeTextExtractor extractor;

    // =====================================================================
    //  Resume -> job matching
    // =====================================================================

    public ResumeMatchResponse matchResume(MultipartFile file) throws IOException {
        String text = extractor.extract(file);
        if (text == null || text.trim().length() < 30) {
            throw new IllegalArgumentException(
                    "I could not find readable text in this file. Scanned or image-only resumes are not supported.");
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> resumeSkills = skills.extract(lower);
        Integer years = estimateYears(lower);

        List<JobMatch> matches = new ArrayList<>();
        for (Job job : openJobs()) {
            JobMatch m = score(job, lower, years);
            if (m != null) matches.add(m);
        }
        matches.sort(Comparator.comparingInt(JobMatch::matchPercent).reversed()
                .thenComparing(m -> -m.matchedSkills().size()));
        List<JobMatch> top = matches.size() > MAX_CARDS ? matches.subList(0, MAX_CARDS) : matches;

        String message;
        if (resumeSkills.isEmpty()) {
            message = "I could not recognise any known skills in this resume. Try adding a Skills section with technologies and tools.";
        } else if (top.isEmpty()) {
            message = "I found " + resumeSkills.size() + " skills in your resume, but no open job matches them right now. Check back soon or browse all jobs.";
        } else {
            message = "I found " + resumeSkills.size() + " skills in your resume. Here are the best matches among open jobs:";
        }
        return new ResumeMatchResponse(new ArrayList<>(resumeSkills), years, new ArrayList<>(top), message);
    }

    private JobMatch score(Job job, String resumeLower, Integer years) {
        Set<String> required = requiredSkills(job);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String s : required) {
            (skills.contains(resumeLower, s) ? matched : missing).add(s);
        }

        boolean titleHit = job.getTitle() != null && resumeLower.contains(job.getTitle().toLowerCase(Locale.ROOT));
        if (matched.isEmpty() && !titleHit) return null;

        double pct = required.isEmpty() ? 0 : (matched.size() * 100.0) / required.size();
        if (titleHit) pct += 10;

        String note = null;
        if (years != null && job.getMinExperience() > years + 1) {
            pct *= 0.8;
            note = "Asks for " + job.getMinExperience() + "+ years; your resume suggests about " + years + ".";
        }
        int percent = (int) Math.min(100, Math.round(pct));
        if (percent <= 0) return null;

        return new JobMatch(job.getId(), job.getTitle(), job.getLocation(),
                job.getEmploymentType() == null ? null : job.getEmploymentType().name(),
                job.getMinExperience(), percent, matched, missing, note);
    }

    /** Skills a job asks for: the recruiter's skills field, or skills found in title and description if it is empty. */
    private Set<String> requiredSkills(Job job) {
        Set<String> req = new LinkedHashSet<>();
        if (job.getSkills() != null && !job.getSkills().isBlank()) {
            for (String token : job.getSkills().split("[,;|\\n]")) {
                if (!token.isBlank()) req.add(skills.canonicalize(token));
            }
        }
        if (req.isEmpty()) {
            req.addAll(skills.extract((job.getTitle() == null ? "" : job.getTitle()) + " "
                    + (job.getDescription() == null ? "" : job.getDescription())));
        }
        return req;
    }

    private Integer estimateYears(String lower) {
        Matcher m = YEARS.matcher(lower);
        int best = -1;
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v <= 30) best = Math.max(best, v);
        }
        return best < 0 ? null : best;
    }

    // =====================================================================
    //  Question answering
    // =====================================================================

    public ChatReply reply(String raw, User user) {
        String msg = raw.toLowerCase(Locale.ROOT).trim();

        if (matches(msg, "^(hi+|hello+|hey+|good (morning|afternoon|evening))\\b.*")) {
            return text("Hi! I can answer questions about open jobs, applying and hiring stages. "
                    + "You can also upload your resume with the paperclip and I will suggest jobs that fit you.");
        }
        if (has(msg, "thank", "thanks")) {
            return text("You are welcome! Ask me anything else about the jobs.");
        }
        if (has(msg, "my application", "my status", "application status", "status of my", "where is my application")) {
            return myApplications(user);
        }
        if (has(msg, "how to apply", "how do i apply", "how can i apply", "apply for", "applying")) {
            return text("To apply: 1) Create a candidate account and sign in. 2) Open a job from the Jobs page. "
                    + "3) Attach your resume (PDF or DOCX, up to 5 MB) and add an optional cover letter. 4) Press Apply. "
                    + "You can follow progress under My applications. You can apply to each job only once.");
        }
        if (has(msg, "stage", "process", "pipeline", "hiring flow", "screening", "what happens after")) {
            return text("Every application moves through: Applied, then Screening, then Interview, then Offer, then Hired. "
                    + "A recruiter can reject an application at any point, and you can withdraw yours. "
                    + "Each change is recorded in a history timeline you can see on your application.");
        }
        if (has(msg, "resume", "cv", "upload", "file format", "file size", "pdf")) {
            return text("Resumes can be PDF or DOCX, up to 5 MB. When you apply, you attach it to the job. "
                    + "If you upload it here in the chat with the paperclip, I will read it and suggest matching jobs. "
                    + "I do not store resumes uploaded to the chat.");
        }
        if (has(msg, "fresher", "entry level", "entry-level", "no experience", "graduate", "0 year")) {
            List<Job> j = openJobs().stream().filter(x -> x.getMinExperience() == 0).toList();
            return jobsReply("Jobs open to freshers (no experience needed):", j);
        }
        if (has(msg, "intern")) return byType("INTERN", "internships");
        if (has(msg, "part time", "part-time")) return byType("PART_TIME", "part-time jobs");
        if (has(msg, "contract")) return byType("CONTRACT", "contract jobs");
        if (has(msg, "full time", "full-time")) return byType("FULL_TIME", "full-time jobs");

        if (has(msg, "salary", "pay", "ctc", "package", "lpa")) {
            List<Job> paid = openJobs().stream().filter(x -> x.getMaxSalary() != null)
                    .sorted(Comparator.comparing(Job::getMaxSalary).reversed()).limit(3).toList();
            if (paid.isEmpty()) {
                return text("Salary ranges appear on a job page when the recruiter adds one. Open a job to check.");
            }
            return jobsReply("Salary is shown on each job page. These openings have the highest listed pay:", paid);
        }
        if (has(msg, "closing", "deadline", "last date", "expire")) {
            LocalDate today = LocalDate.now();
            List<Job> soon = openJobs().stream()
                    .filter(x -> x.getClosingDate() != null && !x.getClosingDate().isBefore(today)
                            && x.getClosingDate().isBefore(today.plusDays(15)))
                    .sorted(Comparator.comparing(Job::getClosingDate)).toList();
            return jobsReply("Jobs closing in the next 2 weeks:", soon);
        }

        // location: "jobs in hyderabad"
        for (Job j : openJobs()) {
            String city = firstPart(j.getLocation());
            if (city != null && city.length() >= 3 && msg.contains(city)) {
                String c = city;
                List<Job> inCity = openJobs().stream()
                        .filter(x -> x.getLocation() != null && x.getLocation().toLowerCase(Locale.ROOT).contains(c)).toList();
                return jobsReply("Open jobs in " + capitalize(c) + ":", inCity);
            }
        }

        // skills: "any java jobs?"
        Set<String> asked = skills.extract(msg);
        if (!asked.isEmpty()) {
            List<Job> withSkill = openJobs().stream()
                    .filter(x -> !Collections.disjoint(requiredSkills(x), asked)).toList();
            return jobsReply("Open jobs that use " + String.join(", ", asked) + ":", withSkill);
        }

        if (has(msg, "how many", "open jobs", "all jobs", "show jobs", "list jobs", "available jobs",
                "latest jobs", "new jobs", "any jobs", "vacanc", "openings", "hiring", "jobs")) {
            List<Job> all = openJobs();
            return jobsReply("There are " + all.size() + " open jobs right now. Here are the newest:", all);
        }
        if (has(msg, "help", "what can you do")) {
            return text("I can show open jobs, filter by location, skill or job type, explain how to apply and the hiring stages, "
                    + "and check your application status. Upload your resume with the paperclip and I will find jobs that match it.");
        }
        if (has(msg, "bye", "goodbye")) return text("Goodbye, and good luck with your applications!");

        return new ChatReply("I am not sure about that one. I can help with open jobs, applying, hiring stages and resume matching. "
                + "Try one of these:", List.of(), DEFAULT_SUGGESTIONS);
    }

    private ChatReply myApplications(User user) {
        if (user == null) return text("Please sign in as a candidate and I can show the status of your applications.");
        if (user.getRole() != Role.CANDIDATE) return text("Application status is available to candidate accounts. Recruiters can see applications on each job's pipeline board.");
        List<Application> apps = applicationRepository
                .findByCandidateId(user.getId(), PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "appliedAt"))).getContent();
        if (apps.isEmpty()) return text("You have not applied to any jobs yet. Ask me for open jobs, or upload your resume to see matches.");
        StringBuilder sb = new StringBuilder("Your latest applications:\n");
        for (Application a : apps) {
            sb.append("• ").append(a.getJob().getTitle()).append(": ")
                    .append(capitalize(a.getCurrentStage().name().toLowerCase(Locale.ROOT)));
            if (a.getStatus() != null && !"ACTIVE".equals(a.getStatus().name())) {
                sb.append(" (").append(a.getStatus().name().toLowerCase(Locale.ROOT)).append(")");
            }
            sb.append("\n");
        }
        return text(sb.toString().trim());
    }

    private ChatReply byType(String type, String noun) {
        List<Job> j = openJobs().stream()
                .filter(x -> x.getEmploymentType() != null && x.getEmploymentType().name().equals(type)).toList();
        return jobsReply("Open " + noun + ":", j);
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    private List<Job> openJobs() {
        return jobRepository.findAll(JobSpecifications.hasStatus(JobStatus.OPEN),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ChatReply text(String s) {
        return new ChatReply(s, List.of(), DEFAULT_SUGGESTIONS);
    }

    private ChatReply jobsReply(String intro, List<Job> jobs) {
        if (jobs.isEmpty()) {
            return new ChatReply("I could not find any open jobs for that right now. Try a different keyword.",
                    List.of(), DEFAULT_SUGGESTIONS);
        }
        List<JobCard> cards = jobs.stream().limit(MAX_CARDS).map(j -> new JobCard(
                j.getId(), j.getTitle(), j.getLocation(),
                j.getEmploymentType() == null ? null : j.getEmploymentType().name(),
                j.getMinExperience(), j.getSkills())).toList();
        return new ChatReply(intro, cards, DEFAULT_SUGGESTIONS);
    }

    private static boolean has(String msg, String... needles) {
        for (String n : needles) if (msg.contains(n)) return true;
        return false;
    }

    private static boolean matches(String msg, String regex) {
        return Pattern.compile(regex).matcher(msg).matches();
    }

    private static String firstPart(String location) {
        if (location == null || location.isBlank()) return null;
        return location.split(",")[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
