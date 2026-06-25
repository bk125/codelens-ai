package com.codereviewer.dto;

public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private Long userId;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuthResponse obj = new AuthResponse();
        public Builder token(String t)  { obj.token = t; return this; }
        public Builder name(String n)   { obj.name = n; return this; }
        public Builder email(String e)  { obj.email = e; return this; }
        public Builder userId(Long id)  { obj.userId = id; return this; }
        public AuthResponse build()     { return obj; }
    }

    public String getToken() { return token; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Long getUserId() { return userId; }
}
