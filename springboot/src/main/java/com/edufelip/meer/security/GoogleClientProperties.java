package com.edufelip.meer.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.google")
public class GoogleClientProperties {
    private String androidClientId = "";
    private String iosClientId = "";
    private String webClientId = "";

    public String getAndroidClientId() { return androidClientId; }
    public void setAndroidClientId(String androidClientId) { this.androidClientId = androidClientId; }
    public String getIosClientId() { return iosClientId; }
    public void setIosClientId(String iosClientId) { this.iosClientId = iosClientId; }
    public String getWebClientId() { return webClientId; }
    public void setWebClientId(String webClientId) { this.webClientId = webClientId; }
}
