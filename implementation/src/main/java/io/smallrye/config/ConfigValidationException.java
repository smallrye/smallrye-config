package io.smallrye.config;

import java.io.Serializable;
import java.util.Optional;

import io.smallrye.common.constraint.Assert;

/**
 * An exception which is thrown when a configuration validation problem occurs.
 */
public class ConfigValidationException extends RuntimeException {
    private static final long serialVersionUID = -2637730579475070264L;

    private final Problem[] problems;

    /**
     * Constructs a new {@code ConfigurationValidationException} instance.
     *
     * @param problems the reported problems
     */
    public ConfigValidationException(final Problem[] problems) {
        super(list(problems));
        this.problems = problems;
    }

    private static String list(Problem[] problems) {
        StringBuilder b = new StringBuilder();
        b.append("Configuration validation failed").append(':');
        for (int i = 0; i < problems.length; i++) {
            Problem problem = problems[i];
            Assert.checkNotNullArrayParam("problems", i, problem);
            b.append(System.lineSeparator());
            b.append("\t");
            b.append(problem.getMessage());
        }
        return b.toString();
    }

    public int getProblemCount() {
        return problems.length;
    }

    public Problem getProblem(int index) {
        return problems[index];
    }

    public static final class Problem implements Serializable {
        public static final Problem[] NO_PROBLEMS = new Problem[0];
        private static final long serialVersionUID = 5984436393578154541L;

        private final String message;
        transient private final RuntimeException exception;

        public Problem(final String message) {
            this.message = message;
            this.exception = null;
        }

        Problem(final RuntimeException exception) {
            this.message = exception.getMessage();
            this.exception = exception;
        }

        public String getMessage() {
            return message;
        }

        Optional<RuntimeException> getException() {
            return Optional.ofNullable(exception);
        }
    }
}
