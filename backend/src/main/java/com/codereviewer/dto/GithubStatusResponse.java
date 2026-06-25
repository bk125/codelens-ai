package com.codereviewer.dto;

public class GithubStatusResponse {
    private boolean connected;
    private String githubUsername;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final GithubStatusResponse o = new GithubStatusResponse();
        public Builder connected(boolean v)      { o.connected = v; return this; }
        public Builder githubUsername(String v)  { o.githubUsername = v; return this; }
        public GithubStatusResponse build()      { return o; }
    }

    public boolean isConnected() { return connected; }
    public String getGithubUsername() { return githubUsername; }
}
