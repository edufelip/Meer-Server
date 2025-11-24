package com.edufelip.meer.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private boolean requireAppHeader = true;
    private boolean requireAppCheck = true;
    private boolean disableAuth = false;
    private String appPackage = "com.edufelip.meer";

    public boolean isRequireAppHeader() { return requireAppHeader; }
    public void setRequireAppHeader(boolean requireAppHeader) { this.requireAppHeader = requireAppHeader; }

    public boolean isRequireAppCheck() { return requireAppCheck; }
    public void setRequireAppCheck(boolean requireAppCheck) { this.requireAppCheck = requireAppCheck; }

    public boolean isDisableAuth() { return disableAuth; }
    public void setDisableAuth(boolean disableAuth) { this.disableAuth = disableAuth; }

    public String getAppPackage() { return appPackage; }
    public void setAppPackage(String appPackage) { this.appPackage = appPackage; }
}
