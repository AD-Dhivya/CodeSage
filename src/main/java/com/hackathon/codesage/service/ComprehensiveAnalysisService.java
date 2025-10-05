package com.hackathon.codesage.service;

import com.hackathon.codesage.model.CodeIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ComprehensiveAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(ComprehensiveAnalysisService.class);
    
    /**
     * Analyze code for all types of issues: Security, Performance, Code Quality, Architecture, Clean Code
     */
    public List<CodeIssue> analyzeCodeComprehensively(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        log.info("🔍 Starting comprehensive code analysis for: {}", fileName);
        
        // Security Analysis
        issues.addAll(analyzeSecurity(code, language, fileName));
        
        // Performance Analysis
        issues.addAll(analyzePerformance(code, language, fileName));
        
        // Code Quality Analysis
        issues.addAll(analyzeCodeQuality(code, language, fileName));
        
        // Architecture Analysis
        issues.addAll(analyzeArchitecture(code, language, fileName));
        
        // Clean Code Analysis
        issues.addAll(analyzeCleanCode(code, language, fileName));
        
        log.info("✅ Comprehensive analysis completed: {} issues found", issues.size());
        return issues;
    }
    
    /**
     * Security Analysis
     */
    private List<CodeIssue> analyzeSecurity(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Hardcoded credentials
        if (hasHardcodedCredentials(code)) {
            issues.add(CodeIssue.builder()
                    .type("Hardcoded Credentials")
                    .severity("CRITICAL")
                    .location("Hardcoded values detected")
                    .description("Hardcoded passwords, API keys, or secrets found in code")
                    .recommendation("Use environment variables or secure configuration management")
                    .category("Security")
                    .explanation("Hardcoded credentials are a major security risk. If this code is committed to version control, anyone with access to the repository can see your secrets. This can lead to unauthorized access to your systems and data breaches.")
                    .example("String password = \"123456\"; // ❌ BAD")
                    .bestPractice("String password = System.getenv(\"DB_PASSWORD\"); // ✅ GOOD")
                    .learningResource("https://owasp.org/www-community/vulnerabilities/Use_of_hard-coded_credentials")
                    .build());
        }
        
        // SQL Injection
        if (hasSqlInjection(code)) {
            issues.add(CodeIssue.builder()
                    .type("SQL Injection")
                    .severity("CRITICAL")
                    .location("SQL query construction")
                    .description("SQL injection vulnerability detected")
                    .recommendation("Use prepared statements or parameterized queries")
                    .category("Security")
                    .explanation("SQL injection occurs when user input is directly concatenated into SQL queries. Attackers can manipulate the input to execute malicious SQL commands, potentially accessing, modifying, or deleting your database.")
                    .example("String query = \"SELECT * FROM users WHERE id = \" + userId; // ❌ BAD")
                    .bestPractice("PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\"); stmt.setString(1, userId); // ✅ GOOD")
                    .learningResource("https://owasp.org/www-community/attacks/SQL_Injection")
                    .build());
        }
        
        return issues;
    }
    
    /**
     * Performance Analysis
     */
    private List<CodeIssue> analyzePerformance(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // N+1 Query Problem
        if (hasNPlusOneQueries(code)) {
            issues.add(CodeIssue.builder()
                    .type("N+1 Query Problem")
                    .severity("HIGH")
                    .location("Database queries in loop")
                    .description("N+1 query problem detected - multiple database calls in loop")
                    .recommendation("Use batch loading or JOIN queries to fetch all data at once")
                    .category("Performance")
                    .explanation("The N+1 query problem occurs when you execute one query to get N records, then execute N additional queries to get related data. This can severely impact performance, especially with large datasets.")
                    .example("for (User user : users) { user.getOrders(); } // ❌ BAD - N+1 queries")
                    .bestPractice("List<Order> orders = orderService.findByUsers(users); // ✅ GOOD - Single query")
                    .learningResource("https://www.baeldung.com/hibernate-n-plus-1-problem")
                    .build());
        }
        
        // Memory Leaks
        if (hasMemoryLeaks(code)) {
            issues.add(CodeIssue.builder()
                    .type("Potential Memory Leak")
                    .severity("MEDIUM")
                    .location("Resource management")
                    .description("Potential memory leak detected")
                    .recommendation("Ensure proper resource cleanup using try-with-resources")
                    .category("Performance")
                    .explanation("Memory leaks occur when objects are not properly garbage collected, leading to increased memory usage over time. This can eventually cause OutOfMemoryError and application crashes.")
                    .example("FileInputStream fis = new FileInputStream(file); // ❌ BAD - Not closed")
                    .bestPractice("try (FileInputStream fis = new FileInputStream(file)) { ... } // ✅ GOOD - Auto-closed")
                    .learningResource("https://www.oracle.com/java/technologies/javase/troubleshooting-memory.html")
                    .build());
        }
        
        return issues;
    }
    
    /**
     * Code Quality Analysis
     */
    private List<CodeIssue> analyzeCodeQuality(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Long Methods
        if (hasLongMethods(code)) {
            issues.add(CodeIssue.builder()
                    .type("Long Method")
                    .severity("MEDIUM")
                    .location("Method length")
                    .description("Method is too long and does multiple things")
                    .recommendation("Break down into smaller, single-purpose methods")
                    .category("CodeQuality")
                    .explanation("Long methods are hard to understand, test, and maintain. They often violate the Single Responsibility Principle and make code more prone to bugs.")
                    .example("public void processUserData() { /* 50+ lines of code */ } // ❌ BAD")
                    .bestPractice("public void processUserData() { validateUser(); saveUser(); sendNotification(); } // ✅ GOOD")
                    .learningResource("https://refactoring.guru/smells/long-method")
                    .build());
        }
        
        // Code Duplication
        if (hasCodeDuplication(code)) {
            issues.add(CodeIssue.builder()
                    .type("Code Duplication")
                    .severity("MEDIUM")
                    .location("Repeated code blocks")
                    .description("Duplicate code detected")
                    .recommendation("Extract common functionality into reusable methods")
                    .category("CodeQuality")
                    .explanation("Code duplication makes maintenance harder. When you need to fix a bug or add a feature, you have to remember to update all the duplicated code. This often leads to inconsistencies and bugs.")
                    .example("// Same validation logic repeated in multiple methods // ❌ BAD")
                    .bestPractice("private boolean isValidEmail(String email) { ... } // ✅ GOOD - Reusable method")
                    .learningResource("https://refactoring.guru/smells/duplicate-code")
                    .build());
        }
        
        return issues;
    }
    
    /**
     * Architecture Analysis
     */
    private List<CodeIssue> analyzeArchitecture(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Tight Coupling
        if (hasTightCoupling(code)) {
            issues.add(CodeIssue.builder()
                    .type("Tight Coupling")
                    .severity("MEDIUM")
                    .location("Class dependencies")
                    .description("Classes are tightly coupled")
                    .recommendation("Use dependency injection and interfaces to reduce coupling")
                    .category("Architecture")
                    .explanation("Tight coupling makes code hard to test, maintain, and extend. Changes in one class can break other classes, making the system fragile.")
                    .example("public class UserService { private DatabaseConnection db = new DatabaseConnection(); } // ❌ BAD")
                    .bestPractice("public class UserService { private DatabaseConnection db; public UserService(DatabaseConnection db) { this.db = db; } } // ✅ GOOD")
                    .learningResource("https://en.wikipedia.org/wiki/Coupling_(computer_programming)")
                    .build());
        }
        
        // Missing Abstractions
        if (hasMissingAbstractions(code)) {
            issues.add(CodeIssue.builder()
                    .type("Missing Abstraction")
                    .severity("LOW")
                    .location("Concrete implementations")
                    .description("Missing abstraction layer")
                    .recommendation("Create interfaces or abstract classes for better flexibility")
                    .category("Architecture")
                    .explanation("Missing abstractions make code inflexible and hard to extend. Without proper abstractions, you can't easily swap implementations or add new features.")
                    .example("public class EmailService { public void send(String message) { ... } } // ❌ BAD")
                    .bestPractice("public interface NotificationService { void send(String message); } // ✅ GOOD")
                    .learningResource("https://en.wikipedia.org/wiki/Abstraction_(computer_science)")
                    .build());
        }
        
        return issues;
    }
    
    /**
     * Clean Code Analysis
     */
    private List<CodeIssue> analyzeCleanCode(String code, String language, String fileName) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Poor Naming
        if (hasPoorNaming(code)) {
            issues.add(CodeIssue.builder()
                    .type("Poor Variable Naming")
                    .severity("LOW")
                    .location("Variable and method names")
                    .description("Variable or method names are not descriptive")
                    .recommendation("Use descriptive names that explain the purpose")
                    .category("CleanCode")
                    .explanation("Poor naming makes code hard to understand. Good names act as documentation and make the code self-explanatory.")
                    .example("int x = 5; // ❌ BAD - What does x represent?")
                    .bestPractice("int userAge = 5; // ✅ GOOD - Clear purpose")
                    .learningResource("https://clean-code-developer.com/grades/grade-1-red/meaningful-names/")
                    .build());
        }
        
        // Magic Numbers
        if (hasMagicNumbers(code)) {
            issues.add(CodeIssue.builder()
                    .type("Magic Numbers")
                    .severity("LOW")
                    .location("Hardcoded numeric values")
                    .description("Magic numbers without explanation")
                    .recommendation("Use named constants to explain the meaning")
                    .category("CleanCode")
                    .explanation("Magic numbers are hardcoded numeric values that appear in code without explanation. They make code hard to understand and maintain.")
                    .example("if (age > 18) { ... } // ❌ BAD - What does 18 represent?")
                    .bestPractice("private static final int LEGAL_DRIVING_AGE = 18; if (age > LEGAL_DRIVING_AGE) { ... } // ✅ GOOD")
                    .learningResource("https://refactoring.guru/smells/magic-numbers")
                    .build());
        }
        
        return issues;
    }
    
    // Helper methods for pattern detection
    private boolean hasHardcodedCredentials(String code) {
        Pattern pattern = Pattern.compile("(password|secret|key|token)\\s*=\\s*[\"'][^\"']{3,}[\"']", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(code).find();
    }
    
    private boolean hasSqlInjection(String code) {
        Pattern pattern = Pattern.compile("\"SELECT.*?\"\\s*\\+|\"INSERT.*?\"\\s*\\+|\"UPDATE.*?\"\\s*\\+|\"DELETE.*?\"\\s*\\+");
        return pattern.matcher(code).find();
    }
    
    private boolean hasNPlusOneQueries(String code) {
        Pattern pattern = Pattern.compile("for\\s*\\(.*\\).*\\{.*\\..*\\..*\\}");
        return pattern.matcher(code).find();
    }
    
    private boolean hasMemoryLeaks(String code) {
        Pattern pattern = Pattern.compile("new\\s+\\w+Stream\\s*\\(.*\\)(?!.*try\\s*\\()");
        return pattern.matcher(code).find();
    }
    
    private boolean hasLongMethods(String code) {
        String[] lines = code.split("\n");
        int methodLines = 0;
        boolean inMethod = false;
        
        for (String line : lines) {
            if (line.trim().startsWith("public ") || line.trim().startsWith("private ") || line.trim().startsWith("protected ")) {
                if (line.contains("(") && line.contains(")")) {
                    inMethod = true;
                    methodLines = 0;
                }
            }
            if (inMethod) {
                methodLines++;
                if (line.trim().equals("}")) {
                    if (methodLines > 20) return true;
                    inMethod = false;
                }
            }
        }
        return false;
    }
    
    private boolean hasCodeDuplication(String code) {
        // Simple duplication detection - look for repeated patterns
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length - 3; i++) {
            String block1 = lines[i] + lines[i+1] + lines[i+2];
            for (int j = i + 3; j < lines.length - 3; j++) {
                String block2 = lines[j] + lines[j+1] + lines[j+2];
                if (block1.equals(block2) && block1.trim().length() > 20) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasTightCoupling(String code) {
        Pattern pattern = Pattern.compile("new\\s+\\w+\\s*\\(.*\\)");
        return pattern.matcher(code).find();
    }
    
    private boolean hasMissingAbstractions(String code) {
        return code.contains("class ") && !code.contains("interface ") && !code.contains("abstract ");
    }
    
    private boolean hasPoorNaming(String code) {
        Pattern pattern = Pattern.compile("\\b[a-z]{1,2}\\b|\\b[a-z]\\d+\\b");
        return pattern.matcher(code).find();
    }
    
    private boolean hasMagicNumbers(String code) {
        Pattern pattern = Pattern.compile("\\b\\d{2,}\\b");
        return pattern.matcher(code).find();
    }
}
