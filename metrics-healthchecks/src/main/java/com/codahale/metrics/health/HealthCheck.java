package com.codahale.metrics.health;

import com.codahale.metrics.Clock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A health check for a component of your application.
 */
public abstract class HealthCheck {

    /**
     * The result of a {@link HealthCheck} being run. It can be healthy (with an optional message and optional details)
     * or unhealthy (with either an error message or a thrown exception and optional details).
     */
    public static class Result {
        private static final DateTimeFormatter DATE_FORMAT_PATTERN =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        private static final int PRIME = 31;
        private static final String DEFAULT_NESTED_DETAILS_NAME = "details";

        /**
         * Returns a healthy {@link Result} with no additional message.
         *
         * @return a healthy {@link Result} with no additional message
         */
        public static Result healthy() {
            return new Result(true, null, null);
        }

        /**
         * Returns a healthy {@link Result} with an additional message.
         *
         * @param message an informative message
         * @return a healthy {@link Result} with an additional message
         */
        public static Result healthy(String message) {
            return new Result(true, message, null);
        }

        /**
         * Returns a healthy {@link Result} with a formatted message.
         * <p>
         * Message formatting follows the same rules as {@link String#format(String, Object...)}.
         *
         * @param message a message format\\
         * @param args    the arguments apply to the message format
         * @return a healthy {@link Result} with an additional message
         * @see String#format(String, Object...)
         */
        public static Result healthy(String message, Object... args) {
            return healthy(String.format(message, args));
        }

        /**
         * Returns an unhealthy {@link Result} with the given message.
         *
         * @param message an informative message describing how the health check failed
         * @return an unhealthy {@link Result} with the given message
         */
        public static Result unhealthy(String message) {
            return new Result(false, message, null);
        }

        /**
         * Returns an unhealthy {@link Result} with a formatted message.
         * <p>
         * Message formatting follows the same rules as {@link String#format(String, Object...)}.
         *
         * @param message a message format
         * @param args    the arguments apply to the message format
         * @return an unhealthy {@link Result} with an additional message
         * @see String#format(String, Object...)
         */
        public static Result unhealthy(String message, Object... args) {
            return unhealthy(String.format(message, args));
        }

        /**
         * Returns an unhealthy {@link Result} with the given error.
         *
         * @param error an exception thrown during the health check
         * @return an unhealthy {@link Result} with the given {@code error}
         */
        public static Result unhealthy(Throwable error) {
            return new Result(false, error.getMessage(), error);
        }


        /**
         * Returns a new {@link ResultBuilder}
         *
         * @return the {@link ResultBuilder}
         */
        public static ResultBuilder builder() {
            return new ResultBuilder();
        }

        private final boolean healthy;
        private final String message;
        private final Throwable error;
        private final Map<String, Object> details;
        private final String nestedDetailsName;
        private final Map<String, Object> nestedDetails;
        private final long time;

        private long duration; // Calculated field

        private Result(boolean isHealthy, String message, Throwable error) {
            this(isHealthy, message, error, null, DEFAULT_NESTED_DETAILS_NAME, null, Clock.defaultClock());
        }

        private Result(ResultBuilder builder) {
            this(builder.healthy, builder.message, builder.error, builder.details,
                    builder.nestedDetailsName, builder.nestedDetails, builder.clock);
        }

        private Result(boolean isHealthy, String message, Throwable error, Map<String, Object> details,
                       String nestedDetailsName, Map<String, Object> nestedDetails, Clock clock) {
            this.healthy = isHealthy;
            this.message = message;
            this.error = error;
            this.details = details == null ? null : Collections.unmodifiableMap(details);
            this.nestedDetailsName = nestedDetailsName;
            this.nestedDetails = nestedDetails == null ? null : Collections.unmodifiableMap(nestedDetails);
            this.time = clock.getTime();
        }

        /**
         * Returns {@code true} if the result indicates the component is healthy; {@code false}
         * otherwise.
         *
         * @return {@code true} if the result indicates the component is healthy
         */
        public boolean isHealthy() {
            return healthy;
        }

        /**
         * Returns any additional message for the result, or {@code null} if the result has no
         * message.
         *
         * @return any additional message for the result, or {@code null}
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns any exception for the result, or {@code null} if the result has no exception.
         *
         * @return any exception for the result, or {@code null}
         */
        public Throwable getError() {
            return error;
        }

        /**
         * Returns the timestamp when the result was created as a formatted String.
         *
         * @return a formatted timestamp
         */
        public String getTimestamp() {
            Instant currentInstant = Instant.ofEpochMilli(time);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(currentInstant, ZoneId.systemDefault());
            return DATE_FORMAT_PATTERN.format(zonedDateTime);
        }

        /**
         * Returns the time when the result was created, in milliseconds since Epoch
         *
         * @return the time when the result was created
         */
        public long getTime() {
            return time;
        }

        /**
         * Returns the duration in milliseconds that the healthcheck took to run
         *
         * @return the duration
         */
        public long getDuration() {
            return duration;
        }

        /**
         * Sets the duration in milliseconds. This will indicate the time it took to run the individual healthcheck
         *
         * @param duration The duration in milliseconds
         */
        public void setDuration(long duration) {
            this.duration = duration;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public String getNestedDetailsName() {
            return nestedDetailsName;
        }

        public Map<String, Object> getNestedDetails() {
            return nestedDetails;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Result result = (Result) o;
            return healthy == result.healthy &&
                    Objects.equals(error, result.error) &&
                    Objects.equals(message, result.message) &&
                    time == result.time;
        }

        @Override
        public int hashCode() {
            int result = healthy ? 1 : 0;
            result = PRIME * result + (message != null ? message.hashCode() : 0);
            result = PRIME * result + (error != null ? error.hashCode() : 0);
            result = PRIME * result + (Long.hashCode(time));
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("Result{isHealthy=");
            builder.append(healthy);
            if (message != null) {
                builder.append(", message=").append(message);
            }
            if (error != null) {
                builder.append(", error=").append(error);
            }
            builder.append(", duration=").append(duration);
            builder.append(", timestamp=").append(getTimestamp());
            appendDetails(builder, details, "");
            appendDetails(builder, nestedDetails, nestedDetailsName + ".");
            builder.append('}');
            return builder.toString();
        }

        private static void appendDetails(StringBuilder builder, Map<String, Object> map, String detailPrefix) {
            if (map == null || map.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Object> e : map.entrySet()) {
                builder.append(", ");
                builder.append(detailPrefix)
                        .append(e.getKey())
                        .append("=")
                        .append(e.getValue());
            }
        }
    }

    /**
     * This a convenient builder for an {@link HealthCheck.Result}. It can be health (with optional message and detail)
     * or unhealthy (with optional message, error and detail)
     */
    public static class ResultBuilder {
        private boolean healthy;
        private String message;
        private Throwable error;
        private Map<String, Object> details;
        private String nestedDetailsName;
        private Map<String, Object> nestedDetails;
        private Clock clock;

        protected ResultBuilder() {
            this.healthy = true;
            this.details = new LinkedHashMap<>();
            this.nestedDetailsName = Result.DEFAULT_NESTED_DETAILS_NAME;
            this.nestedDetails = new LinkedHashMap<>();
            this.clock = Clock.defaultClock();
        }

        /**
         * Configure an healthy result
         *
         * @return this builder with healthy status
         */
        public ResultBuilder healthy() {
            this.healthy = true;
            return this;
        }

        /**
         * Configure an unhealthy result
         *
         * @return this builder with unhealthy status
         */
        public ResultBuilder unhealthy() {
            this.healthy = false;
            return this;
        }

        /**
         * Configure an unhealthy result with an {@code error}
         *
         * @param error the error
         * @return this builder with the given error
         */
        public ResultBuilder unhealthy(Throwable error) {
            this.error = error;
            return this.unhealthy().withMessage(error.getMessage());
        }

        /**
         * Set an optional message
         *
         * @param message an informative message
         * @return this builder with the given {@code message}
         */
        public ResultBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Set an optional formatted message
         * <p>
         * Message formatting follows the same rules as {@link String#format(String, Object...)}.
         *
         * @param message a message format
         * @param args    the arguments apply to the message format
         * @return this builder with the given formatted {@code message}
         * @see String#format(String, Object...)
         */
        public ResultBuilder withMessage(String message, Object... args) {
            return withMessage(String.format(message, args));
        }

        /**
         * Add an optional top-level detail
         *
         * @param key  a key for this detail
         * @param data an object representing the detail data
         * @return this builder with the given detail added
         */
        public ResultBuilder withDetail(String key, Object data) {
            if (this.details == null) {
                this.details = new LinkedHashMap<>();
            }
            this.details.put(key, data);
            return this;
        }

        /**
         * Specify the name given to the nested details map.
         *
         * @param name the nested details name
         * @return this builder with the given nested details name
         */
        public ResultBuilder withNestedDetailsName(String name) {
            this.nestedDetailsName = name;
            return this;
        }

        /**
         * Add an optional nested detail
         *
         * @param key  a key for this nested detail
         * @param data an object representing the nested detail data
         * @return this builder with the given nested detail added
         */
        public ResultBuilder withNestedDetail(String key, Object data) {
            if (this.nestedDetails == null) {
                this.nestedDetails = new LinkedHashMap<>();
            }
            this.nestedDetails.put(key, data);
            return this;
        }

        /**
         * Configure this {@link ResultBuilder} to use the given {@code clock} instead of the default clock.
         * If not specified, the default clock is {@link Clock#defaultClock()}.
         *
         * @param clock the {@link Clock} to use when generating the health check timestamp (useful for unit testing)
         * @return this builder configured to use the given {@code clock}
         */
        public ResultBuilder usingClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Result build() {
            return new Result(this);
        }
    }

    /**
     * Perform a check of the application component.
     *
     * @return if the component is healthy, a healthy {@link Result}; otherwise, an unhealthy {@link
     * Result} with a descriptive error message or exception
     * @throws Exception if there is an unhandled error during the health check; this will result in
     *                   a failed health check
     */
    protected abstract Result check() throws Exception;

    /**
     * Executes the health check, catching and handling any exceptions raised by {@link #check()}.
     *
     * @return if the component is healthy, a healthy {@link Result}; otherwise, an unhealthy {@link
     * Result} with a descriptive error message or exception
     */
    public Result execute() {
        long start = clock().getTick();
        Result result;
        try {
            result = check();
        } catch (Exception e) {
            result = Result.unhealthy(e);
        }
        result.setDuration(TimeUnit.MILLISECONDS.convert(clock().getTick() - start, TimeUnit.NANOSECONDS));
        return result;
    }

    protected Clock clock() {
        return Clock.defaultClock();
    }
}
