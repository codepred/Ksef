package io.alapierre.ksef.sample;

import io.alapierre.ksef.client.ApiClient;
import io.alapierre.ksef.client.ApiException;
import io.alapierre.ksef.client.JsonSerializer;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneFakturaApi;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneSesjaApi;
import io.alapierre.ksef.client.api.InterfejsyInteraktywneZapytaniaApi;
import io.alapierre.ksef.client.iterator.InvoiceQueryResponseAdapter;
import io.alapierre.ksef.client.iterator.KsefResultStream;
import io.alapierre.ksef.client.model.rest.auth.InitSignedResponse;
import io.alapierre.ksef.client.model.rest.auth.SessionStatus;
import io.alapierre.ksef.client.model.rest.common.InvoiceRequest;
import io.alapierre.ksef.client.model.rest.query.InvoiceQueryRequest;
import io.alapierre.ksef.client.model.rest.query.InvoiceQueryResponse;
import io.alapierre.ksef.client.okhttp.OkHttpApiClient;
import io.alapierre.ksef.client.serializer.gson.GsonJsonSerializer;
import io.alapierre.ksef.token.facade.KsefTokenFacade;
import lombok.val;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import static io.alapierre.ksef.client.AbstractApiClient.Environment;
import static io.alapierre.ksef.client.model.rest.auth.AuthorisationChallengeRequest.IdentifierType;

/**
 * @author Adrian Lapierre {@literal al@alapierre.io}
 * Copyrights by original author 2022.01.02
 */

public class Main {

    public static final String NIP_FIRMY = "5094861655";
    private static final  File tokenFile = new File("token.p12");
    private static final  KeyStore.PasswordProtection pas = new KeyStore.PasswordProtection("_____token_password_____".toCharArray());

    private static final JsonSerializer serializer = new GsonJsonSerializer();
    private static final ApiClient client = new OkHttpApiClient(serializer);
    private static final InterfejsyInteraktywneSesjaApi sesjaApi = new InterfejsyInteraktywneSesjaApi(client);

    public static final String token = "D2B5226DD6E4E532688FBCDF2970DA01EC85A420BE56594C807C779AD98E6FD6";
    //"24BB2B31E766F3BB2FF7244964DABCC680D611C515F85420F270254AD0C6E7D7"

    public static void main(String[] args)  {
        try {
            val signedResponse = loginByToken();
//            System.out.println("session token = " + signedResponse.getSessionToken().getToken());

            val invoiceApi = new InterfejsyInteraktywneFakturaApi(client);
            val sessionToken = signedResponse.getSessionToken().getToken();
//            val sessionToken = "20240130-SE-9DC5A3FAE0-8EF1762877-86";
            System.out.println(sessionToken);
            SessionStatus sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            while(sessionStatus.getProcessingCode() != 315) {
                TimeUnit.SECONDS.sleep(5);
                sessionStatus = sesjaApi.sessionStatus(sessionToken, 10, 0);
            }

            System.out.println(sessionStatus.getProcessingCode());

//            val resp = invoiceApi.invoiceSend(new File("ksef-sample/src/main/resources/FA2.xml"), sessionToken);
//
//            System.out.printf("ElementReferenceNumber %s, ReferenceNumber %s, ProcessingCode %d\n",
//                    resp.getElementReferenceNumber(),
//                    resp.getReferenceNumber(),
//                    resp.getProcessingCode());

//            val zapytaniaApi = new InterfejsyInteraktywneZapytaniaApi(client);
//            val request = InvoiceQueryRequest.builder()
//                    .queryCriteria(InvoiceQueryRequest.QueryCriteria.builder()
//                            .subjectType("subject1")
//                            .type("incremental")
//                            .acquisitionTimestampThresholdFrom("2024-01-28T00:00:00")
//                            .acquisitionTimestampThresholdTo("2024-01-31T00:00:00")
//                            .build())
//                    .build();
//
//            KsefResultStream.builder(
//                            page -> new InvoiceQueryResponseAdapter(zapytaniaApi.invoiceQuery(sessionToken, request, 10, page)))
//                    .forEach(System.out::println);
            FileOutputStream stream = new FileOutputStream("5094861655-20240130-77C6BB40E86D-6D.xml");
            invoiceApi.getInvoice("5094861655-20240130-77C6BB40E86D-6D", sessionToken, stream);
            stream.close();
            sesjaApi.terminateSession(sessionToken);
        } catch (ApiException ex) {
            System.err.printf("Błąd wywołania API %d (%s) opis błędu %s", ex.getCode(), ex.getMessage(),  ex.getResponseBody());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static void fetchInvoiceByCommonAPI(){
        try {
            val invoiceApi = new InterfejsyInteraktywneFakturaApi(client);
            InvoiceQueryResponse.IssuedToIdentifier issued = new InvoiceQueryResponse.IssuedToIdentifier("onip", "9655573052");
            InvoiceQueryResponse.IssuedToName name = new InvoiceQueryResponse.IssuedToName("fn", null, "Firma Janowski");
            InvoiceQueryResponse.SubjectTo subject = new InvoiceQueryResponse.SubjectTo(issued, name);
            val invoiceDetails = InvoiceRequest.InvoiceDetails.builder()
                .invoiceOryginalNumber("FK2023/09/14")
                .subjectTo(subject)
                .dueValue("100")
                .build();

            val requestBody = InvoiceRequest.builder()
                .ksefReferenceNumber("5282740347-20230914-2979E373175E-25")
                .invoiceDetails(invoiceDetails)
                .build();


            ByteArrayOutputStream out = new ByteArrayOutputStream(0);
            invoiceApi.getInvoice(requestBody, out);

            try (FileOutputStream fos = new FileOutputStream(requestBody.getKsefReferenceNumber() + ".xml")) {
                fos.write(out.toByteArray());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        } catch (ApiException ex) {
            System.out.printf("Błąd wywołania API %d (%s) opis błędu %s", ex.getCode(), ex.getMessage(), ex.getResponseBody());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

//    public static InitSignedResponse loginBySignature() throws IOException, ApiException {
//
//        val signer = new P12Signer(pas, tokenFile);
//        KsefDssFacade facade = new KsefDssFacade(signer, sesjaApi);
//        return facade.authByDigitalSignature(NIP_FIRMY, IdentifierType.onip);
//    }

    @SuppressWarnings("DuplicatedCode")
    public static InitSignedResponse loginByToken() throws ApiException, ParseException {

        val facade = new KsefTokenFacade(sesjaApi);
        return facade.authByToken(Environment.TEST, NIP_FIRMY, IdentifierType.onip, token);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void loadIncomingInvoices(String sessionToken) throws ParseException, ApiException {

        val zapytaniaApi = new InterfejsyInteraktywneZapytaniaApi(client);

        val request = InvoiceQueryRequest.builder()
                .queryCriteria(InvoiceQueryRequest.QueryCriteria.builder()
                        .subjectType("subject2")
                        .type("incremental")
                        .acquisitionTimestampThresholdFrom("2024-08-01T00:00:00")
                        .acquisitionTimestampThresholdTo("2024-09-01T00:00:00")
                        .build())
                .build();

        KsefResultStream.builder(
                page -> new InvoiceQueryResponseAdapter(zapytaniaApi.invoiceQuery(sessionToken, request, 10, page)))
                .forEach(System.out::println);
    }



}
