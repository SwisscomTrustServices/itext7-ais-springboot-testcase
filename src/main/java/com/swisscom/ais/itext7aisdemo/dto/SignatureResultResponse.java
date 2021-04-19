package com.swisscom.ais.itext7aisdemo.dto;

import com.swisscom.ais.itext7.client.model.SignatureResult;

import java.util.Objects;

public class SignatureResultResponse {

  private final SignatureResult signatureResult;

  public SignatureResultResponse(SignatureResult signatureResult) {
    this.signatureResult = signatureResult;
  }

  public SignatureResult getSignatureResult() {
    return signatureResult;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SignatureResultResponse that = (SignatureResultResponse) o;
    return signatureResult == that.signatureResult;
  }

  @Override
  public int hashCode() {
    return Objects.hash(signatureResult);
  }
}
