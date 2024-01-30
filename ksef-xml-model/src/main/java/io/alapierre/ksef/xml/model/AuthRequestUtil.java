package io.alapierre.ksef.xml.model;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import pl.gov.mf.ksef.schema.gtw.svc.online.auth.request._2021._10._01._0001.InitSessionSignedRequest;
import pl.gov.mf.ksef.schema.gtw.svc.online.auth.request._2021._10._01._0001.InitSessionTokenRequest;
import pl.gov.mf.ksef.schema.gtw.svc.online.auth.request._2021._10._01._0001.ObjectFactory;
import pl.gov.mf.ksef.schema.gtw.svc.online.types._2021._10._01._0001.AuthorisationContextTokenType;
import pl.gov.mf.ksef.schema.gtw.svc.types._2021._10._01._0001.*;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * @author Adrian Lapierre {@literal al@alapierre.io}
 * Copyrights by original author 2021.12.23
 */
public class AuthRequestUtil {

    private AuthRequestUtil() {
    }

    public static @NotNull InitSessionTokenRequest prepareTokenAuthRequest(@NotNull String challenge, @NotNull String identifier, @NotNull String encryptedToken) {

        val initSessionTokenRequest = new InitSessionTokenRequest();

        val subjectIdentifier = new SubjectIdentifierByCompanyType();
        subjectIdentifier.setIdentifier(identifier);

        val context = new AuthorisationContextTokenType();
        context.setChallenge(challenge);
        context.setIdentifier(subjectIdentifier);
        context.setToken(encryptedToken);

        context.setDocumentType(prepareDocumentType());
        initSessionTokenRequest.setContext(context);

        return initSessionTokenRequest;
    }

    public static @NotNull InitSessionSignedRequest prepareAuthRequest(@NotNull String challenge, @NotNull String identifier) {

        var factory = new ObjectFactory();
        var contextFactory = new pl.gov.mf.ksef.schema.gtw.svc.online.types._2021._10._01._0001.ObjectFactory();

        InitSessionSignedRequest request = factory.createInitSessionSignedRequest();

        var context = contextFactory.createAuthorisationContextSignedType();
        context.setType(AuthorisationTypeType.SERIAL_NUMBER);
        context.setChallenge(challenge);

        var documentTypeType = prepareDocumentType();

        context.setDocumentType(documentTypeType);

        var id = new SubjectIdentifierByCompanyType();
        id.setIdentifier(identifier);
        context.setIdentifier(id);

        request.setContext(context);

        return request;
    }

    public static @NotNull DocumentTypeType prepareDocumentType() {

        val documentTypeType = new DocumentTypeType();
        documentTypeType.setService(ServiceType.K_SE_F);

        val form = new FormCodeType();
        form.setSystemCode("FA (2)");
        form.setSchemaVersion("1-0E");
        form.setTargetNamespace("http://crd.gov.pl/wzor/2023/06/29/12648/");
        form.setValue("FA");

        documentTypeType.setFormCode(form);

        return documentTypeType;
    }

    public static void requestToFile(@NotNull InitSessionSignedRequest request, @NotNull File outputFile) {
        AuthRequestSerializer serializer = new AuthRequestSerializer();
        serializer.toFile(request, outputFile.getAbsolutePath());
    }

    public static byte[] requestToBytes(@NotNull InitSessionSignedRequest request) {
        AuthRequestSerializer serializer = new AuthRequestSerializer();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        serializer.toStream(request, os);
        return os.toByteArray();
    }

}
