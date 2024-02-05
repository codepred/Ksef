package com.studia.controller;

import com.studia.dto.Response;
import io.alapierre.ksef.client.AbstractApiClient;
import io.alapierre.ksef.client.ApiClient;
import io.alapierre.ksef.client.ApiException;
import io.alapierre.ksef.client.JsonSerializer;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneFakturaApi;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneSesjaApi;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneZapytaniaApi;
import io.alapierre.ksef.client.iterator.InvoiceQueryResponseAdapter;
import io.alapierre.ksef.client.model.rest.auth.AuthorisationChallengeRequest;
import io.alapierre.ksef.client.model.rest.auth.InitSignedResponse;
import io.alapierre.ksef.client.model.rest.auth.SessionStatus;
import io.alapierre.ksef.client.model.rest.query.InvoiceQueryRequest;
import io.alapierre.ksef.client.okhttp.OkHttpApiClient;
import io.alapierre.ksef.client.serializer.gson.GsonJsonSerializer;
import io.alapierre.ksef.token.facade.KsefTokenFacade;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class Controller {

    @Value( "${version}" )
    private String version;
    private static final JsonSerializer serializer = new GsonJsonSerializer();
    private static final ApiClient client = new OkHttpApiClient(serializer);
    private static final InterfejsyInteraktywneSesjaApi sesjaApi = new InterfejsyInteraktywneSesjaApi(client);

    @PostMapping("/api/add-invoice")
    public ResponseEntity<Object> addInvoice(@RequestParam(name = "invoice") MultipartFile invoice,
                                            @RequestParam(name = "token") String token,
                                             @RequestParam(name = "nip") String nip)
            throws ParseException, ApiException, InterruptedException, IOException {
        val facade = new KsefTokenFacade(sesjaApi);
        InitSignedResponse signedResponse;
        try {
            signedResponse = facade.authByToken(AbstractApiClient.Environment.DEMO, nip, AuthorisationChallengeRequest.IdentifierType.onip, token);
        } catch (ApiException e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "NIP or token are invalid"));
        }
        val invoiceApi = new InterfejsyInteraktywneFakturaApi(client);
        val sessionToken = signedResponse.getSessionToken().getToken();
        SessionStatus sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
        int counter = 0;
        while(sessionStatus.getProcessingCode() != 315) {
            if(counter == 5) {
                return ResponseEntity.status(408).body(new Response("TIMEOUT", "Server is taking too long to response"));
            }
            TimeUnit.SECONDS.sleep(5);
            sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            counter++;
        }
        val resp = invoiceApi.invoiceSend(invoice.getBytes(), sessionToken);
        TimeUnit.SECONDS.sleep(1);
        val ret = invoiceApi.invoiceStatus(sessionToken, resp.getElementReferenceNumber());
        if(ret.getProcessingCode() == 425) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Document is not valid with schema"));
        }
        sesjaApi.terminateSession(sessionToken);
        return ResponseEntity.status(200).body(new Response("OK", "Document has been added successfully"));
    }

    @GetMapping("/api/get-issued-invoice-list/size={size}/offset={offset}")
    public ResponseEntity<Object> getIssuedInvoices(@RequestParam(name = "token") String token, @RequestParam(name = "nip") String nip,
                                            @RequestParam(name = "dateFrom") String dateFrom, @RequestParam(name = "dateTo") String dateTo,
                                            @PathVariable("size") int size, @PathVariable("offset") int offset) throws ParseException, ApiException, InterruptedException {
        if(size < 10) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Size must be at least 10"));
        }
        try {
            LocalDateTime.parse(dateFrom);
            LocalDateTime.parse(dateTo);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Correct date format is: YYYY-MM-DDTHH:MM:SS"));
        }
        val facade = new KsefTokenFacade(sesjaApi);
        InitSignedResponse signedResponse;
        try {
            signedResponse = facade.authByToken(AbstractApiClient.Environment.DEMO, nip, AuthorisationChallengeRequest.IdentifierType.onip, token);
        } catch (ApiException e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Make sure token and nip are valid or try again later"));
        }
        val sessionToken = signedResponse.getSessionToken().getToken();
        SessionStatus sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
        int counter = 0;
        while(sessionStatus.getProcessingCode() != 315) {
            if(counter == 5) {
                return ResponseEntity.status(408).body(new Response("TIMEOUT", "Server is taking too long to response"));
            }
            TimeUnit.SECONDS.sleep(5);
            sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            counter++;
        }
        val zapytaniaApi = new InterfejsyInteraktywneZapytaniaApi(client);
        val request = InvoiceQueryRequest.builder()
                .queryCriteria(InvoiceQueryRequest.QueryCriteria.builder()
                        .subjectType("subject1")
                        .type("incremental")
                        .acquisitionTimestampThresholdFrom(dateFrom)
                        .acquisitionTimestampThresholdTo(dateTo)
                        .build())
                .build();
        val result = new InvoiceQueryResponseAdapter(zapytaniaApi.invoiceQuery(sessionToken, request, size, offset));
        return ResponseEntity.status(200).body(result);
    }

    @GetMapping("/api/get-received-invoice-list/size={size}/offset={offset}")
    public ResponseEntity<Object> getReceivedInvoices(@RequestParam(name = "token") String token, @RequestParam(name = "nip") String nip,
                                             @RequestParam(name = "dateFrom") String dateFrom, @RequestParam(name = "dateTo") String dateTo,
                                             @PathVariable("size") int size, @PathVariable("offset") int offset) throws ParseException, ApiException, InterruptedException {
        if(size < 10) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Size must be at least 10"));
        }
        try {
            LocalDateTime.parse(dateFrom);
            LocalDateTime.parse(dateTo);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Correct date format is: YYYY-MM-DDTHH:MM:SS"));
        }
        val facade = new KsefTokenFacade(sesjaApi);
        InitSignedResponse signedResponse;
        try {
            signedResponse = facade.authByToken(AbstractApiClient.Environment.DEMO, nip, AuthorisationChallengeRequest.IdentifierType.onip, token);
        } catch (ApiException e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Make sure token and nip are valid or try again later"));
        }
        val sessionToken = signedResponse.getSessionToken().getToken();
        SessionStatus sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
        int counter = 0;
        while(sessionStatus.getProcessingCode() != 315) {
            if(counter == 5) {
                return ResponseEntity.status(408).body(new Response("TIMEOUT", "Server is taking too long to response"));
            }
            TimeUnit.SECONDS.sleep(5);
            sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            counter++;
        }
        val zapytaniaApi = new InterfejsyInteraktywneZapytaniaApi(client);
        val request = InvoiceQueryRequest.builder()
                .queryCriteria(InvoiceQueryRequest.QueryCriteria.builder()
                        .subjectType("subject2")
                        .type("incremental")
                        .acquisitionTimestampThresholdFrom(dateFrom)
                        .acquisitionTimestampThresholdTo(dateTo)
                        .build())
                .build();
        val result = new InvoiceQueryResponseAdapter(zapytaniaApi.invoiceQuery(sessionToken, request, size, offset));
        return ResponseEntity.status(200).body(result);
    }

    @GetMapping("/api/get-invoice/{referenceNumber}")
    ResponseEntity<Object> getInvoice(@RequestParam(name = "token") String token, @RequestParam(name = "nip") String nip,
                                      @PathVariable("referenceNumber") String ksefReferenceNumber,
                                        HttpServletResponse response) throws ApiException, InterruptedException, IOException {
        val facade = new KsefTokenFacade(sesjaApi);
        InitSignedResponse signedResponse;
        try {
            signedResponse = facade.authByToken(AbstractApiClient.Environment.DEMO, nip, AuthorisationChallengeRequest.IdentifierType.onip, token);
        } catch (ApiException | ParseException e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Make sure token and nip are valid or try again later"));
        }
        val sessionToken = signedResponse.getSessionToken().getToken();
        SessionStatus sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
        int counter = 0;
        while(sessionStatus.getProcessingCode() != 315) {
            if(counter == 5) {
                return ResponseEntity.status(408).body(new Response("TIMEOUT", "Server is taking too long to response"));
            }
            TimeUnit.SECONDS.sleep(5);
            sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            counter++;
        }
        val invoiceApi = new InterfejsyInteraktywneFakturaApi(client);
        File file = new File("tmpp.xml");
        response.setContentType("application/xml");
        response.setHeader("Content-disposition", "attachment; filename=" + file.getName());
        try {
            invoiceApi.getInvoice(ksefReferenceNumber, sessionToken, response.getOutputStream());
        } catch (ApiException e) {
            return ResponseEntity.status(400).body(new Response("BAD_REQUEST", "Make sure reference number is valid or try again later"));
        }
        sesjaApi.terminateSession(sessionToken);
        file.delete();
        return ResponseEntity.status(200).body(null);
    }

    @GetMapping("/version")
    ResponseEntity<Object> getVersion(){
        return ResponseEntity.status(200).body(version);
    }

    @GetMapping()
    ResponseEntity<Object> getVersionDefault(){
        return ResponseEntity.status(200).body(version);
    }
}
