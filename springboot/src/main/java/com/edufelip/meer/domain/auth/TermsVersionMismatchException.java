package com.edufelip.meer.domain.auth;

public class TermsVersionMismatchException extends RuntimeException {
  private final String requiredVersion;
  private final String providedVersion;

  public TermsVersionMismatchException(String requiredVersion, String providedVersion) {
    super(
        "terms_version does not match required version. required="
            + requiredVersion
            + ", provided="
            + providedVersion);
    this.requiredVersion = requiredVersion;
    this.providedVersion = providedVersion;
  }

  public String getRequiredVersion() {
    return requiredVersion;
  }

  public String getProvidedVersion() {
    return providedVersion;
  }
}
