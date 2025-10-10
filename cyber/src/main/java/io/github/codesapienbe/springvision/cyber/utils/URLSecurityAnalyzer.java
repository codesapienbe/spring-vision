package io.github.codesapienbe.springvision.cyber.utils;

import io.github.codesapienbe.springvision.cyber.models.QRCodeThreat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for analyzing URLs for security threats and suspicious patterns.
 *
 * <p>This analyzer checks for:
 * <ul>
 *   <li>Known phishing patterns and suspicious domains</li>
 *   <li>URL shorteners that may hide malicious destinations</li>
 *   <li>Suspicious TLDs and domain patterns</li>
 *   <li>Homograph attacks (unicode lookalike characters)</li>
 *   <li>Known malicious indicators</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class URLSecurityAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(URLSecurityAnalyzer.class);

    // Suspicious TLDs often used in phishing
    private static final List<String> SUSPICIOUS_TLDS = Arrays.asList(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".xyz", ".work", ".click", ".link"
    );

    // Common URL shorteners that can hide malicious destinations
    private static final List<String> URL_SHORTENERS = Arrays.asList(
        "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "t.co", "is.gd", "buff.ly"
    );

    // Suspicious keywords often found in phishing URLs
    private static final List<String> SUSPICIOUS_KEYWORDS = Arrays.asList(
        "verify", "account", "update", "login", "secure", "banking", "confirm",
        "suspended", "locked", "urgent", "click", "winner", "prize", "free"
    );

    private static final Pattern IP_ADDRESS_PATTERN =
        Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    private double sensitivity = 0.7; // Default sensitivity

    /**
     * Default constructor.
     */
    public URLSecurityAnalyzer() {
        logger.debug("Initialized URLSecurityAnalyzer");
    }

    /**
     * Analyzes a URL for security threats and returns a security score.
     *
     * @param url the URL to analyze
     * @return security score with threat details
     */
    public SecurityScore analyzeURL(String url) {
        SecurityScore score = new SecurityScore(url);
        List<String> threats = new ArrayList<>();

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            String query = uri.getQuery();

            // Check for IP address instead of domain name (suspicious)
            if (host != null && IP_ADDRESS_PATTERN.matcher(host).matches()) {
                threats.add("URL uses IP address instead of domain name");
                score.addRisk(0.3);
            }

            // Check for suspicious TLDs
            if (host != null) {
                for (String tld : SUSPICIOUS_TLDS) {
                    if (host.endsWith(tld)) {
                        threats.add("Suspicious top-level domain: " + tld);
                        score.addRisk(0.2);
                        break;
                    }
                }
            }

            // Check for URL shorteners
            if (host != null) {
                for (String shortener : URL_SHORTENERS) {
                    if (host.contains(shortener)) {
                        threats.add("URL shortener detected (destination hidden)");
                        score.addRisk(0.25);
                        break;
                    }
                }
            }

            // Check for HTTP instead of HTTPS
            if ("http".equals(uri.getScheme())) {
                threats.add("Insecure HTTP protocol (not HTTPS)");
                score.addRisk(0.15);
            }

            // Check for suspicious keywords
            String fullUrl = url.toLowerCase();
            for (String keyword : SUSPICIOUS_KEYWORDS) {
                if (fullUrl.contains(keyword)) {
                    threats.add("Suspicious keyword found: " + keyword);
                    score.addRisk(0.1);
                }
            }

            // Check for excessive subdomains (common in phishing)
            if (host != null) {
                long subdomainCount = host.chars().filter(ch -> ch == '.').count();
                if (subdomainCount > 3) {
                    threats.add("Excessive subdomains detected");
                    score.addRisk(0.15);
                }
            }

            // Check for suspicious path patterns
            if (path != null && (path.contains("..") || path.contains("///"))) {
                threats.add("Suspicious path traversal patterns");
                score.addRisk(0.2);
            }

            // Check for excessively long URL (often used to hide malicious content)
            if (url.length() > 200) {
                threats.add("Unusually long URL");
                score.addRisk(0.1);
            }

            // Check for @ symbol in URL (can be used to hide actual domain)
            if (url.contains("@")) {
                threats.add("URL contains @ symbol (potential domain masking)");
                score.addRisk(0.25);
            }

            // Check for unicode/homograph attacks
            if (host != null && containsNonAsciiCharacters(host)) {
                threats.add("Non-ASCII characters detected (potential homograph attack)");
                score.addRisk(0.3);
            }

        } catch (Exception e) {
            threats.add("Malformed URL");
            score.addRisk(0.4);
            logger.warn("Error analyzing URL: {}", url, e);
        }

        score.setThreats(threats);
        score.calculateSeverity(sensitivity);

        logger.debug("URL analysis complete: {} - Severity: {}", url, score.getSeverity());

        return score;
    }

    /**
     * Checks if a string contains non-ASCII characters.
     */
    private boolean containsNonAsciiCharacters(String text) {
        return !text.matches("\\A\\p{ASCII}*\\z");
    }

    /**
     * Sets the sensitivity level for threat detection.
     *
     * @param sensitivity value between 0.0 (lenient) and 1.0 (strict)
     */
    public void setSensitivity(double sensitivity) {
        if (sensitivity < 0.0 || sensitivity > 1.0) {
            throw new IllegalArgumentException("Sensitivity must be between 0.0 and 1.0");
        }
        this.sensitivity = sensitivity;
    }

    /**
     * Represents a security score for a URL.
     */
    public static class SecurityScore {
        private final String url;
        private double riskScore;
        private List<String> threats;
        private QRCodeThreat.ThreatSeverity severity;

        /**
         * Constructor for SecurityScore.
         *
         * @param url the URL being analyzed
         */
        public SecurityScore(String url) {
            this.url = url;
            this.riskScore = 0.0;
            this.threats = new ArrayList<>();
            this.severity = QRCodeThreat.ThreatSeverity.SAFE;
        }

        /**
         * Adds risk to the security score.
         *
         * @param risk the risk value to add
         */
        public void addRisk(double risk) {
            this.riskScore += risk;
        }

        /**
         * Calculates the severity based on the risk score and sensitivity.
         *
         * @param sensitivity the sensitivity level
         */
        public void calculateSeverity(double sensitivity) {
            // Adjust risk score based on sensitivity
            double adjustedRisk = riskScore * sensitivity;

            if (adjustedRisk >= 0.8) {
                this.severity = QRCodeThreat.ThreatSeverity.CRITICAL;
            } else if (adjustedRisk >= 0.6) {
                this.severity = QRCodeThreat.ThreatSeverity.HIGH;
            } else if (adjustedRisk >= 0.4) {
                this.severity = QRCodeThreat.ThreatSeverity.MEDIUM;
            } else if (adjustedRisk >= 0.2) {
                this.severity = QRCodeThreat.ThreatSeverity.LOW;
            } else {
                this.severity = QRCodeThreat.ThreatSeverity.SAFE;
            }
        }

        /**
         * Gets the URL.
         *
         * @return the URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * Gets the risk score.
         *
         * @return the risk score
         */
        public double getRiskScore() {
            return riskScore;
        }

        /**
         * Gets the list of threats.
         *
         * @return the list of threats
         */
        public List<String> getThreats() {
            return threats;
        }

        /**
         * Sets the list of threats.
         *
         * @param threats the list of threats to set
         */
        public void setThreats(List<String> threats) {
            this.threats = threats;
        }

        /**
         * Gets the severity.
         *
         * @return the severity
         */
        public QRCodeThreat.ThreatSeverity getSeverity() {
            return severity;
        }
    }
}
