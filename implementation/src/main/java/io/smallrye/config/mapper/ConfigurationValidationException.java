package io.smallrye.config.mapper;

import io.smallrye.common.constraint.Assert;

/**
 * An exception which is thrown when a configuration validation problem occurs.
 */
public class ConfigurationValidationException extends Exception {
    private static final long serialVersionUID = -2637730579475070264L;

    private final Problem[] problems;

    /**
     * Constructs a new {@code ConfigurationValidationException} instance.
     *
     * @param problems the reported problems
     */
    public ConfigurationValidationException(final Problem[] problems) {
        super(list("Configuration validation failed", problems));
        this.problems = problems;
    }

    private static String list(String msg, Problem[] problems) {
        StringBuilder b = new StringBuilder();
        b.append(msg).append(':');
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

    public static final class Problem {
        public static final Problem[] NO_PROBLEMS = new Problem[0];

        private final String message;

        public Problem(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
