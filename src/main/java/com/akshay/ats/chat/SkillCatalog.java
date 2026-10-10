package com.akshay.ats.chat;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A built-in dictionary of skills. Each canonical skill has aliases
 * (e.g. "spring boot" also matches "springboot"). Used to pull skills out of
 * resume text and out of job postings so both sides speak the same vocabulary.
 */
@Component
public class SkillCatalog {

    private final Map<String, List<String>> aliases = new LinkedHashMap<>();
    private final Map<String, Pattern> patterns = new LinkedHashMap<>();

    public SkillCatalog() {
        // languages
        add("java", "java");
        add("javascript", "javascript", "js", "ecmascript");
        add("typescript", "typescript", "ts");
        add("python", "python");
        add("c++", "c++", "cpp");
        add("c#", "c#", "csharp");
        add("c", "c language", "c programming");
        add("go", "golang");
        add("kotlin", "kotlin");
        add("php", "php");
        add("ruby", "ruby");
        add("swift", "swift");
        add("r", "r programming", "r language");
        add("sql", "sql", "plsql", "pl/sql");
        add("html", "html", "html5");
        add("css", "css", "css3");

        // backend
        add("spring boot", "spring boot", "springboot");
        add("spring", "spring framework", "spring mvc", "spring security", "spring data");
        add("hibernate", "hibernate", "jpa");
        add("node.js", "node.js", "nodejs", "node js");
        add("express", "express.js", "expressjs");
        add("django", "django");
        add("flask", "flask");
        add("fastapi", "fastapi");
        add(".net", ".net", "dotnet", "asp.net");
        add("rest api", "rest api", "restful", "rest apis", "web services");
        add("microservices", "microservices", "microservice");
        add("graphql", "graphql");

        // frontend
        add("react", "react", "reactjs", "react.js");
        add("angular", "angular", "angularjs");
        add("vue", "vue", "vue.js", "vuejs");
        add("tailwind", "tailwind", "tailwindcss");
        add("bootstrap", "bootstrap");
        add("redux", "redux");

        // data
        add("mysql", "mysql");
        add("postgresql", "postgresql", "postgres");
        add("mongodb", "mongodb", "mongo");
        add("oracle", "oracle");
        add("redis", "redis");
        add("kafka", "kafka");
        add("machine learning", "machine learning", "ml");
        add("deep learning", "deep learning");
        add("data analysis", "data analysis", "data analytics");
        add("pandas", "pandas");
        add("numpy", "numpy");
        add("tensorflow", "tensorflow");
        add("pytorch", "pytorch");
        add("power bi", "power bi", "powerbi");
        add("tableau", "tableau");
        add("excel", "excel", "advanced excel");

        // devops / tools
        add("git", "git", "github", "gitlab");
        add("docker", "docker");
        add("kubernetes", "kubernetes", "k8s");
        add("aws", "aws", "amazon web services");
        add("azure", "azure");
        add("gcp", "gcp", "google cloud");
        add("jenkins", "jenkins");
        add("ci/cd", "ci/cd", "cicd", "ci cd");
        add("linux", "linux", "unix");
        add("maven", "maven");
        add("junit", "junit");
        add("selenium", "selenium");
        add("postman", "postman");
        add("jira", "jira");
        add("agile", "agile", "scrum");

        // non-tech
        add("communication", "communication skills", "communication");
        add("recruitment", "recruitment", "talent acquisition", "sourcing");
        add("sales", "sales", "business development");
        add("marketing", "digital marketing", "seo", "content marketing");
        add("customer support", "customer support", "customer service");
        add("accounting", "accounting", "tally", "gst");
        add("figma", "figma");
        add("ui/ux", "ui/ux", "ux design", "ui design");
    }

    private void add(String canonical, String... names) {
        List<String> all = new ArrayList<>(Arrays.asList(names));
        if (!all.contains(canonical)) all.add(canonical);
        aliases.put(canonical, all);
        String joined = String.join("|", all.stream().map(Pattern::quote).toList());
        // boundary that also treats + # . as part of a word, so "c" does not match inside "c++"
        patterns.put(canonical, Pattern.compile("(?<![a-z0-9+#.])(?:" + joined + ")(?![a-z0-9+#])"));
    }

    /** Canonical skills found in the given text. */
    public Set<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return found;
        String lower = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Pattern> e : patterns.entrySet()) {
            if (e.getValue().matcher(lower).find()) found.add(e.getKey());
        }
        return found;
    }

    public boolean contains(String text, String canonicalOrRaw) {
        Pattern p = patterns.get(canonicalOrRaw.toLowerCase(Locale.ROOT));
        String lower = text.toLowerCase(Locale.ROOT);
        if (p != null) return p.matcher(lower).find();
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(canonicalOrRaw.toLowerCase(Locale.ROOT)) + "(?![a-z0-9])")
                .matcher(lower).find();
    }

    /** Maps one skill token (as typed by a recruiter) to its canonical name, or returns it lowercased. */
    public String canonicalize(String token) {
        String t = token.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> e : aliases.entrySet()) {
            if (e.getValue().contains(t)) return e.getKey();
        }
        return t;
    }
}
