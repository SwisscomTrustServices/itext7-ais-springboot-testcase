# How to Test the AIS Client from this SpringBoot Project Video Demo
https://swisscom-my.sharepoint.com/personal/paul_muntean_swisscom_com/_layouts/15/onedrive.aspx?id=%2Fpersonal%2Fpaul%5Fmuntean%5Fswisscom%5Fcom%2FDocuments%2FAufnahmen%2FSpring%20Boot%20Proj%2E%2C%20iText7%20client%20config%2C%20etc%2E%2D20210420%5F143922%2DBesprechungsaufzeichnung%2Emp4&parent=%2Fpersonal%2Fpaul%5Fmuntean%5Fswisscom%5Fcom%2FDocuments%2FAufnahmen&originalPath=aHR0cHM6Ly9zd2lzc2NvbS1teS5zaGFyZXBvaW50LmNvbS86djovcC9wYXVsX211bnRlYW4vRWZobGJyeU9sME5GdU1iZmxtcjI2cnNCWmswdGI2c2dwNVQyMjlXU25QckJoUT9ydGltZT1ZSWxhX2ZzRDJVZw

or

https://swisscom-my.sharepoint.com/:v:/p/paul_muntean/EfhlbryOl0NFuMbflmr26rsBlHU4VtW6ic0N9d1Xp0e9Aw

# Swisscom iText7 AIS Java client demo

A demo of using the Java client library
for [Swisscom All-in Signing Service (AIS)](https://www.swisscom.ch/en/business/enterprise/offer/security/all-in-signing-service.html)
to sign and/or timestamp PDF documents. The library is used as a dependency in a Spring Boot application. The current project shows how the AIS
library configuration should be done. Moreover, it exposes some REST endpoints to demonstrate how different documents can be signed in different ways.
More details about the iText7-AIS library can be found [here](https://github.com/SwisscomTrustServices/itext7-ais). Hint: the code is purely 
demonstrative and should be adapted to the business needs!

## Configuring the app

For further details about the configuration properties, please check
this [description](https://github.com/SwisscomTrustServices/itext7-ais/blob/develop/docs/configure-the-AIS-client.md). Considering the Spring way of
configuring the beans needed for our application see the below section. For simplicity, different ``UserData`` objects were defined: one for signing
documents following the *static* flow, and the other for signing documents following the *on-demand* (with *step-up*) or *timestamp* flows. Also, for 
instance, it shows how logging can be configured programmatically. Hint: in a real scenario, an ``UserData`` should be built and used accordingly,
depending on the user that triggers the signing request (for further clarifications about this aspect, please check
this [source](https://github.com/SwisscomTrustServices/itext7-ais/blob/develop/docs/get-authentication-details.md)), but for the sake of this demo, 
the entire configuration is based on the configuration properties.

```java

@Configuration
public class AisConfig {

  @Value("${swisscom.ais-client.signature.staticClaimedIdentityKey}")
  private String staticClaimedIdentityKey;

  @Bean
  @ConfigurationProperties(prefix = "swisscom.ais-client")
  public Properties properties() {
    return new Properties();
  }

  @Bean
  public SignatureRestClient signatureRestClient(Properties properties) {
    RestClientConfiguration restConfig = new RestClientConfiguration().fromProperties(properties).build();
    return new SignatureRestClientImpl().withConfiguration(restConfig);
  }

  @Bean
  public AisClientConfiguration aisClientConfiguration(Properties properties) {
    return new AisClientConfiguration().fromProperties(properties).build();
  }

  @Bean(destroyMethod = "close")
  public AisClient aisClient(AisClientConfiguration aisConfig, SignatureRestClient restClient) {
    return new AisClientImpl(new AisRequestService(), aisConfig, restClient);
  }

  @Bean
  public SigningService signingService(AisClient aisClient) {
    return new SigningService(aisClient);
  }

  @Bean("OnDemandUserData")
  public UserData userData(Properties properties) {
    return new UserData()
        .fromProperties(properties)
        .withConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl))
        .build();
  }

  @Bean("StaticUserData")
  public UserData staticUserData(Properties properties) {
    return new UserData()
        .fromProperties(properties)
        .withClaimedIdentityKey(staticClaimedIdentityKey)
        .withConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl))
        .build();
  }

  @Bean
  public LogbackConfiguration logbackConfiguration() {
    LogbackConfiguration logbackConfiguration = new LogbackConfiguration();
    logbackConfiguration.initialize(VerboseLevel.BASIC);
    return logbackConfiguration;
  }
}
```

## Signing the PDF documents
Several REST endpoints are exposed in order to prove each signing flow. Different ways are implemented regarding the request parameters and responses.
You may find useful to combine them or to adapt the code depending on the end application business logic and requirements. 

* Receive input and output file paths, sign with **On Demand with Step Up** flow and return the result status message: *POST* **/on-demand-step-up-file**
```java
  @PostMapping("/on-demand-step-up-file")
  public SignatureResultResponse signOnDemandStepUpFileWithClient(@RequestParam String inputFilePath, @RequestParam String outputFilePath) throws FileNotFoundException {
    PdfMetadata pdfMetadata = new PdfMetadata(new FileInputStream(inputFilePath), new FileOutputStream(outputFilePath));
    return new SignatureResultResponse(aisClient.signWithOnDemandCertificateAndStepUp(Collections.singletonList(pdfMetadata), userData));
  }
```

* Receive a multipart input file, sign with **Static** flow and return the Base64 encoded resulted PDF: *POST* **/static-multipart**
```java
  @PostMapping(value = "/static-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<String> signStaticMultipartWithService(@RequestParam MultipartFile inputFile) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PdfMetadata pdfMetadata = new PdfMetadata(inputFile.getInputStream(), outputStream);
    signingService.performSignings(Collections.singletonList(pdfMetadata), SignatureMode.STATIC, staticUserData);
    String encodedDocument = Base64.getEncoder().encodeToString(outputStream.toByteArray());
    return ResponseEntity.ok()
                         .contentType(MediaType.APPLICATION_PDF)
                         .body(encodedDocument);
  }
```

* Receive several multipart input files, sign in batch with **Timestamp** flow and return a list of the Base64 encoded resulted PDFs: *POST* **/timestamp-batch** 
```java
  @PostMapping(value = "/timestamp-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<String> signTimestampBatchWithService(@RequestParam MultipartFile[] inputFiles) {
    List<PdfMetadata> documents = Arrays.stream(inputFiles).map(this::toPdfMetadata).collect(Collectors.toList());
    signingService.performSignings(documents, SignatureMode.TIMESTAMP, userData);
    return documents.stream()
                    .map(document -> Base64.getEncoder().encodeToString(((ByteArrayOutputStream) document.getOutputStream()).toByteArray()))
                    .collect(Collectors.toList());
  }

  private PdfMetadata toPdfMetadata(MultipartFile inputFile) {
    try {
      return new PdfMetadata(inputFile.getInputStream(), new ByteArrayOutputStream());
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("The file %s could not be read", inputFile.getOriginalFilename()));
    }
  }
```

* Receive input, output file paths and the ``SignatureMode`` to dynamically sign a document and return the result status message: *POST* **/dynamic**
```java
  @PostMapping("/dynamic")
  public SignatureResultResponse signDocumentWithService(@RequestParam String inputFilePath, @RequestParam String outputFilePath,
                                                         @RequestParam SignatureMode signatureMode) throws FileNotFoundException {
    PdfMetadata pdfMetadata = new PdfMetadata(new FileInputStream(inputFilePath), new FileOutputStream(outputFilePath));
    return new SignatureResultResponse(signingService.performSignings(Collections.singletonList(pdfMetadata), signatureMode, userData));
  }
```
