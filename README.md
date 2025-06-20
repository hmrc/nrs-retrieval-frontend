# nrs-retrieval-frontend

[![Build Status](https://travis-ci.org/hmrc/nrs-retrieval-frontend.svg)](https://travis-ci.org/hmrc/nrs-retrieval-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/nrs-retrieval-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/nrs-retrieval-frontend/_latestVersion)

This is service provides an interface to the nonrep retrieval backend.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")


### Running the application

In order to run the microservice, you must have SBT installed. You should then be able to start the application using:

> ```sbt run {PORT}```

> The port used for this project is 9390

> To run the tests for the application, you can run: ```sbt test```
> or to view coverage run: ```sbt coverage test coverageReport```

> Landing page URL for the service is ```https://{HOST:PORT}/nrs-retrieval/start```

### Running the application using Service Manager

In order to run the application and all of it's dependencies using service manager, you must have service manager installed.
You should then be able to start the application using:

> ```sm --start NRS_RETRIEVAL_ALL -f```

### Test-only endpoints

To use the test-only endpoints, run the service using the `./run-with-test-only-endpoints.sh` script.


`GET  /nrs-retrieval/test-only/validate-download`

This test-only endpoint serves an HTML page that can be used to validate the contents of an available zip to download.

To use, submit the archive name and vault id of the download to verify.


`GET  /nrs-retrieval/test-only/check-authorisation`

This test-only endpoint serves an HTML page that can be used to demonstrate that the backend `nrs-retrieval` service can be integrated with stride auth.

It was added as part of a spike to show whether we can add security in depth to `nrs-retrieval` and we anticipate that once this is done the endpoint will be removed.

The endpoint functions as follows:

    Given I am a user authenticated via Stride with the enrolment nrs_digital_investigator
    When I navigate to /nrs-retrieval/test-only/check-authorisation
    Then a call is made to the nrs-retrieval endpoint /test-only/check-authorisation
    And content is served which shows that nrs-retrieval has deduced that I am authenticated via Stride and authorised for NRS

    Given I am a user not authenticated via Stride
    When I navigate to /nrs-retrieval/test-only/check-authorisation
    Then a call is made to the nrs-retrieval endpoint /test-only/check-authorisation
    And content is served which shows that nrs-retrieval has deduced that I am not authenticated via Stride

    Given I am a user authenticated via Stride but without the enrolment nrs_digital_investigator
    When I navigate to /nrs-retrieval/test-only/check-authorisation
    Then a call is made to the nrs-retrieval endpoint /test-only/check-authorisation
    And content is served which shows that nrs-retrieval has deduced that I am authenticated via Stride but not authorised for NRS

### Check and Reformat code

All code should be formatted before being pushed, to check the format  
>   ```sbt scalafmtCheckAll it/scalafmtCheckAll```

and reformat if required:

>   ```sbt scalafmtAll it/scalafmtAll```

