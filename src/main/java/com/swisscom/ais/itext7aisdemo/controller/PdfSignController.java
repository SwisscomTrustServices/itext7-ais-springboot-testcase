package com.swisscom.ais.itext7aisdemo.controller;

import com.swisscom.ais.itext7.client.AisClient;
import com.swisscom.ais.itext7.client.model.PdfMetadata;
import com.swisscom.ais.itext7.client.model.SignatureMode;
import com.swisscom.ais.itext7.client.model.UserData;
import com.swisscom.ais.itext7.client.utils.ClientUtils;
import com.swisscom.ais.itext7aisdemo.dto.SignatureResultResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ais")
public class PdfSignController {

  private final AisClient aisClient;

  private final UserData userData;
  private final UserData staticUserData;

  public PdfSignController(AisClient aisClient, @Qualifier("OnDemandUserData") UserData userData,
                           @Qualifier("StaticUserData") UserData staticUserData) {
    this.aisClient = aisClient;
    this.userData = userData;
    this.staticUserData = staticUserData;
  }

  @PostMapping("/on-demand-step-up-file")
  public SignatureResultResponse signOnDemandStepUpFile(@RequestParam String inputFilePath, @RequestParam String outputFilePath) throws FileNotFoundException {
    PdfMetadata pdfMetadata = new PdfMetadata(new FileInputStream(inputFilePath), new FileOutputStream(outputFilePath));
    return new SignatureResultResponse(aisClient.signWithOnDemandCertificateAndStepUp(Collections.singletonList(pdfMetadata), userData));
  }

  @PostMapping(value = "/static-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<String> signStaticMultipart(@RequestParam MultipartFile inputFile) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PdfMetadata pdfMetadata = new PdfMetadata(inputFile.getInputStream(), outputStream);
    aisClient.signWithStaticCertificate(Collections.singletonList(pdfMetadata), staticUserData);
    String encodedDocument = Base64.getEncoder().encodeToString(outputStream.toByteArray());
    return ResponseEntity.ok()
                         .contentType(MediaType.APPLICATION_PDF)
                         .body(encodedDocument);
  }

  @PostMapping(value = "/timestamp-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<String> signTimestampBatch(@RequestParam MultipartFile[] inputFiles) {
    List<PdfMetadata> documents = Arrays.stream(inputFiles).map(this::toPdfMetadata).collect(Collectors.toList());
    aisClient.signWithTimestamp(documents, userData);
    return documents.stream()
                    .map(document -> Base64.getEncoder().encodeToString(((ByteArrayOutputStream) document.getOutputStream()).toByteArray()))
                    .collect(Collectors.toList());
  }

  @PostMapping("/dynamic")
  public SignatureResultResponse signDocument(@RequestParam String inputFilePath, @RequestParam String outputFilePath,
                                                         @RequestParam SignatureMode signatureMode) throws FileNotFoundException {
    PdfMetadata pdfMetadata = new PdfMetadata(new FileInputStream(inputFilePath), new FileOutputStream(outputFilePath));
    return new SignatureResultResponse(ClientUtils.sign(aisClient, Collections.singletonList(pdfMetadata), signatureMode, userData));
  }

  private PdfMetadata toPdfMetadata(MultipartFile inputFile) {
    try {
      return new PdfMetadata(inputFile.getInputStream(), new ByteArrayOutputStream());
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("The file %s could not be read", inputFile.getOriginalFilename()));
    }
  }
}
