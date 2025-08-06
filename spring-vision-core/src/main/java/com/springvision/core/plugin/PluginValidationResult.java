package com.springvision.core.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of plugin validation operations.
 *
 * <p>This class encapsulates the result of validating plugin parameters,
 * dependencies, or other plugin-related validations. It provides information
 * about whether the validation was successful and any error messages.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionPlugin
 */
public class PluginValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a new PluginValidationResult.
     *
     * @param valid whether the validation was successful
     * @param errors list of error messages
     * @param warnings list of warning messages
     */
    private PluginValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors != null ? errors : List.of());
        this.warnings = new ArrayList<>(warnings != null ? warnings : List.of());
    }

    /**
     * Creates a successful validation result.
     *
     * @return a successful validation result
     */
    public static PluginValidationResult success() {
        return new PluginValidationResult(true, List.of(), List.of());
    }

    /**
     * Creates a successful validation result with warnings.
     *
     * @param warnings the warning messages
     * @return a successful validation result with warnings
     */
    public static PluginValidationResult success(List<String> warnings) {
        return new PluginValidationResult(true, List.of(), warnings);
    }

    /**
     * Creates a failed validation result.
     *
     * @param error the error message
     * @return a failed validation result
     */
    public static PluginValidationResult failure(String error) {
        return new PluginValidationResult(false, List.of(error), List.of());
    }

    /**
     * Creates a failed validation result with multiple errors.
     *
     * @param errors the error messages
     * @return a failed validation result
     */
    public static PluginValidationResult failure(List<String> errors) {
        return new PluginValidationResult(false, errors, List.of());
    }

    /**
     * Creates a failed validation result with errors and warnings.
     *
     * @param errors the error messages
     * @param warnings the warning messages
     * @return a failed validation result with errors and warnings
     */
    public static PluginValidationResult failure(List<String> errors, List<String> warnings) {
        return new PluginValidationResult(false, errors, warnings);
    }

    /**
     * Checks if the validation was successful.
     *
     * @return true if validation was successful, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Checks if the validation failed.
     *
     * @return true if validation failed, false otherwise
     */
    public boolean isInvalid() {
        return !valid;
    }

    /**
     * Gets the error messages.
     *
     * @return an unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Gets the warning messages.
     *
     * @return an unmodifiable list of warning messages
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Gets the first error message.
     *
     * @return the first error message, or null if no errors
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * Gets the first warning message.
     *
     * @return the first warning message, or null if no warnings
     */
    public String getFirstWarning() {
        return warnings.isEmpty() ? null : warnings.get(0);
    }

    /**
     * Gets the number of errors.
     *
     * @return the number of errors
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets the number of warnings.
     *
     * @return the number of warnings
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Checks if there are any errors.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Checks if there are any warnings.
     *
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Combines this validation result with another.
     *
     * @param other the other validation result
     * @return a combined validation result
     */
    public PluginValidationResult combine(PluginValidationResult other) {
        Objects.requireNonNull(other, "Other validation result must not be null");

        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);

        List<String> combinedWarnings = new ArrayList<>(this.warnings);
        combinedWarnings.addAll(other.warnings);

        boolean combinedValid = this.valid && other.valid;

        return new PluginValidationResult(combinedValid, combinedErrors, combinedWarnings);
    }

    /**
     * Creates a new validation result with an additional error.
     *
     * @param error the additional error message
     * @return a new validation result with the additional error
     */
    public PluginValidationResult withError(String error) {
        List<String> newErrors = new ArrayList<>(this.errors);
        newErrors.add(error);
        return new PluginValidationResult(false, newErrors, this.warnings);
    }

    /**
     * Creates a new validation result with an additional warning.
     *
     * @param warning the additional warning message
     * @return a new validation result with the additional warning
     */
    public PluginValidationResult withWarning(String warning) {
        List<String> newWarnings = new ArrayList<>(this.warnings);
        newWarnings.add(warning);
        return new PluginValidationResult(this.valid, this.errors, newWarnings);
    }

    @Override
    public String toString() {
        if (valid) {
            if (warnings.isEmpty()) {
                return "PluginValidationResult{valid=true}";
            } else {
                return String.format("PluginValidationResult{valid=true, warnings=%d}", warnings.size());
            }
        } else {
            return String.format("PluginValidationResult{valid=false, errors=%d, warnings=%d}",
                               errors.size(), warnings.size());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        PluginValidationResult that = (PluginValidationResult) obj;
        return valid == that.valid &&
               Objects.equals(errors, that.errors) &&
               Objects.equals(warnings, that.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, warnings);
    }
}
