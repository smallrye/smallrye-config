package io.smallrye.config.validation;

public class ConfigValueValidatorBuilder {
    private long max;
    private boolean hasMax;
    private long min;
    private boolean hasMin;

    public ConfigValueValidatorBuilder max(long value) {
        this.max = value;
        this.hasMax = true;
        return this;
    }

    public ConfigValueValidatorBuilder min(long value) {
        this.min = value;
        this.hasMin = true;
        return this;
    }

    public long getMax() {
        return max;
    }

    public boolean hasMax() {
        return hasMax;
    }

    public long getMin() {
        return min;
    }

    public boolean hasMin() {
        return hasMin;
    }
}
