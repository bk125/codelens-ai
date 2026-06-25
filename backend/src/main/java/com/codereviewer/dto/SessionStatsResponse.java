package com.codereviewer.dto;

public class SessionStatsResponse {

    private String sessionId;
    private long totalReviews;
    private double averageScore;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SessionStatsResponse obj = new SessionStatsResponse();

        public Builder sessionId(String s)       { obj.sessionId = s; return this; }
        public Builder totalReviews(long n)      { obj.totalReviews = n; return this; }
        public Builder averageScore(double d)    { obj.averageScore = d; return this; }
        public SessionStatsResponse build()      { return obj; }
    }

    public String getSessionId() { return sessionId; }
    public long getTotalReviews() { return totalReviews; }
    public double getAverageScore() { return averageScore; }
}
