package de.adorsys.ledgers.oba.rest.server.resource;

import de.adorsys.ledgers.middleware.api.domain.account.AccountDetailsTO;
import de.adorsys.ledgers.middleware.api.domain.account.AccountStatusTO;
import de.adorsys.ledgers.middleware.api.domain.account.AccountTypeTO;
import de.adorsys.ledgers.middleware.api.domain.account.UsageTypeTO;
import de.adorsys.ledgers.middleware.api.domain.oauth.OauthCodeResponseTO;
import de.adorsys.ledgers.middleware.api.domain.sca.OpTypeTO;
import de.adorsys.ledgers.middleware.api.domain.sca.SCAConsentResponseTO;
import de.adorsys.ledgers.middleware.api.domain.sca.SCALoginResponseTO;
import de.adorsys.ledgers.middleware.api.domain.sca.ScaStatusTO;
import de.adorsys.ledgers.middleware.api.domain.um.AccessTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.AisAccountAccessInfoTO;
import de.adorsys.ledgers.middleware.api.domain.um.AisConsentTO;
import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;
import de.adorsys.ledgers.middleware.client.rest.AccountRestClient;
import de.adorsys.ledgers.middleware.client.rest.AuthRequestInterceptor;
import de.adorsys.ledgers.middleware.client.rest.ConsentRestClient;
import de.adorsys.ledgers.middleware.client.rest.OauthRestClient;
import de.adorsys.ledgers.oba.rest.api.resource.exception.ConsentAuthorizeException;
import de.adorsys.ledgers.oba.rest.server.auth.ObaMiddlewareAuthentication;
import de.adorsys.ledgers.oba.service.api.domain.*;
import de.adorsys.ledgers.oba.service.api.service.AuthorizationService;
import de.adorsys.ledgers.oba.service.api.service.RedirectConsentService;
import de.adorsys.psd2.consent.api.ais.AisAccountAccess;
import de.adorsys.psd2.consent.api.ais.AisAccountConsentAuthorisation;
import de.adorsys.psd2.consent.api.ais.CmsAisAccountConsent;
import de.adorsys.psd2.consent.api.ais.CmsAisConsentResponse;
import de.adorsys.psd2.xs2a.core.consent.AisConsentRequestType;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import org.adorsys.ledgers.consent.psu.rest.client.CmsPsuAisClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import static de.adorsys.ledgers.middleware.api.domain.sca.ScaStatusTO.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AISControllerTest {
    private static final String PIN = "12345";
    private static final String LOGIN = "anton.brueckner";
    private static final String ENCRYPTED_ID = "ENC_123";
    private static final String AUTH_ID = "AUTH_1";
    private static final String METHOD_ID = "SCA_1";
    private static final String COOKIE = "COOKIE";
    private static final String TOKEN = "TOKEN";
    private static final String OK_URI = "OK_URI";
    private static final String NOK_URI = "NOK_URI";
    private static final String CONSENT_ID = "12345";
    private static final String CODE = "xyz132";
    private static final String ASPSP_ACC_ID = "ASPSP_ACC_ID";
    private static final String RESOURCE_ID = "RESOURCE_ID";
    private static final String IBAN = "DE123456789";
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final String TPP_AUTH_ID = "TPP_123";
    private static final LocalDate DATE = LocalDate.of(2020, 1, 24);
    private static final LocalDate EXPIRE_DATE = LocalDate.of(2050, 1, 1);

    @InjectMocks
    private AISController controller;
    @Mock
    private CmsPsuAisClient cmsPsuAisClient;
    @Mock
    private ConsentRestClient consentRestClient;
    @Mock
    private AccountRestClient accountRestClient;
    @Mock
    private OauthRestClient oauthRestClient;
    @Mock
    private RedirectConsentService redirectConsentService;
    @Mock
    private XISControllerService xisService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ResponseUtils responseUtils;
    @Mock
    private ObaMiddlewareAuthentication middlewareAuth;
    @Mock
    private AuthorizationService authService;
    @Mock
    private AuthRequestInterceptor authInterceptor;

    @Test
    void aisAuth() {
        // Given
        when(xisService.auth(any(), any(), any(), any())).thenReturn(getAuthResponse());

        // When
        ResponseEntity<AuthorizeResponse> result = controller.aisAuth(AUTH_ID, ENCRYPTED_ID, TOKEN);

        // Then
        assertEquals(getAuthResponse(), result);
    }

    @Test
    void login() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(PSUIDENTIFIED, ConsentStatus.RECEIVED));
        when(xisService.performLoginForConsent(anyString(), anyString(), anyString(), anyString(), any(OpTypeTO.class))).thenReturn(getScaLoginResponse(true));
        when(cmsPsuAisClient.updatePsuDataInConsent(anyString(), anyString(), anyString(), any())).thenReturn(ResponseEntity.ok(null));
        when(accountRestClient.getListOfAccounts()).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.login(ENCRYPTED_ID, AUTH_ID, LOGIN, PIN, COOKIE);

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, PSUIDENTIFIED)), result);
    }

    @Test
    void login_cms_psu_error() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(PSUIDENTIFIED, ConsentStatus.RECEIVED));
        when(xisService.performLoginForConsent(anyString(), anyString(), anyString(), anyString(), any(OpTypeTO.class))).thenReturn(getScaLoginResponse(true));
        when(cmsPsuAisClient.updatePsuDataInConsent(anyString(), anyString(), anyString(), any())).thenReturn(ResponseEntity.badRequest().build());
        when(responseUtils.couldNotProcessRequest(any(), any(), any(), any())).thenReturn(ResponseEntity.badRequest().build());

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.login(ENCRYPTED_ID, AUTH_ID, LOGIN, PIN, COOKIE);

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void login_consent_exception() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.login(ENCRYPTED_ID, AUTH_ID, LOGIN, PIN, COOKIE);

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void login_fail_ledgers_login() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(PSUIDENTIFIED, ConsentStatus.RECEIVED));
        when(xisService.performLoginForConsent(anyString(), anyString(), anyString(), anyString(), any(OpTypeTO.class))).thenReturn(getScaLoginResponse(false));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.login(ENCRYPTED_ID, AUTH_ID, LOGIN, PIN, COOKIE);

        // Then
        assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(), result);
    }

    @Test
    void login_update_cms_exception() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(PSUIDENTIFIED, ConsentStatus.RECEIVED));
        when(xisService.performLoginForConsent(anyString(), anyString(), anyString(), anyString(), any(OpTypeTO.class))).thenReturn(getScaLoginResponse(true));
        when(cmsPsuAisClient.updatePsuDataInConsent(anyString(), anyString(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        ResponseEntity<ConsentAuthorizeResponse> result = controller.login(ENCRYPTED_ID, AUTH_ID, LOGIN, PIN, COOKIE);
        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void startConsentAuth() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));

        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(PSUIDENTIFIED, ConsentStatus.RECEIVED));
        when(accountRestClient.getListOfAccounts()).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.startConsentAuth(ENCRYPTED_ID, AUTH_ID, COOKIE, getAisConsentTO(false));

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, PSUIDENTIFIED)), result);
    }

    @Test
    void startConsentAuth_exempted() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(EXEMPTED, ConsentStatus.RECEIVED));
        when(accountRestClient.getListOfAccounts()).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.startConsentAuth(ENCRYPTED_ID, AUTH_ID, COOKIE, getAisConsentTO(false));

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void startConsentAuth_received() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(RECEIVED, ConsentStatus.RECEIVED));
        when(accountRestClient.getListOfAccounts()).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.startConsentAuth(ENCRYPTED_ID, AUTH_ID, COOKIE, getAisConsentTO(false));

        // Then
        assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(), result);
    }

    @Test
    void startConsentAuth_fail() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.startConsentAuth(ENCRYPTED_ID, AUTH_ID, COOKIE, getAisConsentTO(false));

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void authrizedConsent() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(FINALISED, ConsentStatus.RECEIVED));
        when(consentRestClient.authorizeConsent(anyString(), anyString(), anyString())).thenReturn(ResponseEntity.ok(getScaConsentResponse(FINALISED)));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.authrizedConsent(ENCRYPTED_ID, AUTH_ID, COOKIE, CODE);

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)), result);
    }

    @Test
    void authrizedConsent_error() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.authrizedConsent(ENCRYPTED_ID, AUTH_ID, COOKIE, CODE);

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void selectMethod() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(SCAMETHODSELECTED, ConsentStatus.RECEIVED));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.selectMethod(ENCRYPTED_ID, AUTH_ID, METHOD_ID, COOKIE);

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, SCAMETHODSELECTED)), result);
    }

    @Test
    void selectMethod_error() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.selectMethod(ENCRYPTED_ID, AUTH_ID, METHOD_ID, COOKIE);

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    @Test
    void getListOfAccounts() throws NoSuchFieldException {
        // Given
        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));
        when(accountRestClient.getListOfAccounts()).thenReturn(ResponseEntity.ok(getAccounts()));

        // When
        ResponseEntity<List<AccountDetailsTO>> result = controller.getListOfAccounts(COOKIE);

        // Then
        assertEquals(ResponseEntity.ok(getAccounts()), result);
    }

    @Test
    void aisDone() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(FINALISED, ConsentStatus.VALID));
        when(responseUtils.redirect(anyString(), any())).thenReturn(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)));
        when(authService.resolveAuthConfirmationCodeRedirectUri(anyString(), anyString())).thenReturn(OK_URI);

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.aisDone(ENCRYPTED_ID, AUTH_ID, COOKIE, false, "code");

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)), result);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(responseUtils).redirect(urlCaptor.capture(), any());
        assertEquals(OK_URI, urlCaptor.getValue());
    }

    @Test
    void aisDone_nok() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(RECEIVED, ConsentStatus.REJECTED));
        when(responseUtils.redirect(anyString(), any())).thenReturn(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)));
        when(authService.resolveAuthConfirmationCodeRedirectUri(anyString(), anyString())).thenReturn("");

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.aisDone(ENCRYPTED_ID, AUTH_ID, COOKIE, false, "code");

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)), result);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        verify(responseUtils).redirect(urlCaptor.capture(), any());
        assertEquals(NOK_URI, urlCaptor.getValue());
    }

    @Test
    void aisDone_oauth2_integrated() {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(FINALISED, ConsentStatus.RECEIVED));
        when(responseUtils.redirect(anyString(), any())).thenReturn(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)));
        when(oauthRestClient.oauthCode(any())).thenReturn(ResponseEntity.ok(new OauthCodeResponseTO(OK_URI, "code")));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.aisDone(ENCRYPTED_ID, AUTH_ID, COOKIE, true, "code");

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(true, true, false, FINALISED)), result);
    }

    @Test
    void revokeConsent() throws NoSuchFieldException {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenReturn(getConsentWorkflow(FINALISED, ConsentStatus.RECEIVED));

        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));

        when(cmsPsuAisClient.updateAuthorisationStatus(anyString(), anyString(), anyString(), anyString(), ArgumentMatchers.nullable(String.class), ArgumentMatchers.nullable(String.class), ArgumentMatchers.nullable(String.class), anyString(), any())).thenReturn(ResponseEntity.ok(null));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.revokeConsent(ENCRYPTED_ID, AUTH_ID, COOKIE);

        // Then
        assertEquals(ResponseEntity.ok(getConsentAuthorizeResponse(false, false, true, ScaStatusTO.EXEMPTED)), result);
    }

    @Test
    void revokeConsent_error() throws NoSuchFieldException {
        // Given
        when(responseUtils.consentCookie(any())).thenReturn(COOKIE);
        when(redirectConsentService.identifyConsent(anyString(), anyString(), anyBoolean(), anyString(), any())).thenThrow(new ConsentAuthorizeException(ResponseEntity.badRequest().build()));

        FieldSetter.setField(controller, controller.getClass().getDeclaredField("middlewareAuth"), new ObaMiddlewareAuthentication(null, new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO())));

        // When
        ResponseEntity<ConsentAuthorizeResponse> result = controller.revokeConsent(ENCRYPTED_ID, AUTH_ID, COOKIE);

        // Then
        assertEquals(ResponseEntity.badRequest().build(), result);
    }

    private List<AccountDetailsTO> getAccounts() {
        return Collections.singletonList(new AccountDetailsTO(ASPSP_ACC_ID, IBAN, null, null, null, null, EUR, LOGIN, null, AccountTypeTO.CASH, AccountStatusTO.ENABLED, null, null, UsageTypeTO.PRIV, null, Collections.emptyList()));
    }

    private SCAConsentResponseTO getScaConsentResponse(ScaStatusTO status) {
        SCAConsentResponseTO to = new SCAConsentResponseTO();
        to.setBearerToken(getBearerToken());
        to.setConsentId(CONSENT_ID);
        to.setScaStatus(status);
        to.setAuthorisationId(AUTH_ID);
        return to;
    }

    private BearerTokenTO getBearerToken() {
        return new BearerTokenTO(TOKEN, null, 999, null, getAccessTokenTO());
    }

    private AisConsentTO getAisConsentTO(boolean isEmpty) {
        return isEmpty
                   ? new AisConsentTO(null, null, null, 0, new AisAccountAccessInfoTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null), null, false)
                   : new AisConsentTO(CONSENT_ID, LOGIN, "TPP_ID", 5, null, DATE, false);
    }

    private ConsentAuthorizeResponse getConsentAuthorizeResponse(boolean hasAuth, boolean hasEncrConsId, boolean isEmptyConsent, ScaStatusTO scaStatus) {
        ConsentAuthorizeResponse cons = new ConsentAuthorizeResponse();
        cons.setScaStatus(scaStatus);
        cons.setAuthorisationId(hasAuth ? AUTH_ID : null);
        cons.setAccounts(new ArrayList<>());
        cons.setConsent(getAisConsentTO(isEmptyConsent));
        cons.setEncryptedConsentId(hasEncrConsId ? ENCRYPTED_ID : null);
        return cons;
    }

    private ResponseEntity<SCALoginResponseTO> getScaLoginResponse(boolean hasBearer) {
        SCALoginResponseTO to = new SCALoginResponseTO();
        to.setAuthorisationId(AUTH_ID);
        to.setScaStatus(PSUIDENTIFIED);

        to.setBearerToken(hasBearer
                              ? new BearerTokenTO(null, null, 999, null, getAccessTokenTO())
                              : null);
        return ResponseEntity.ok(to);
    }

    private ResponseEntity<AuthorizeResponse> getAuthResponse() {
        AuthorizeResponse resp = new AuthorizeResponse();
        resp.setAuthorisationId(AUTH_ID);
        resp.setEncryptedConsentId(ENCRYPTED_ID);
        resp.setScaStatus(PSUIDENTIFIED);
        resp.setScaMethods(Collections.emptyList());
        return ResponseEntity.ok(resp);
    }

    private ConsentWorkflow getConsentWorkflow(ScaStatusTO status, ConsentStatus consentStatus) {
        ConsentWorkflow workflow = new ConsentWorkflow(getCmsAisConsentResponse(consentStatus), getConsentReference());
        workflow.setAuthResponse(getConsentAuthorizeResponse(true, true, false, status));
        workflow.setScaResponse(getScaConsentResponse(status));
        return workflow;
    }

    private ConsentReference getConsentReference() {
        ConsentReference ref = new ConsentReference();
        ref.setAuthorizationId(AUTH_ID);
        ref.setConsentType(ConsentType.AIS);
        ref.setCookieString(COOKIE);
        ref.setEncryptedConsentId(ENCRYPTED_ID);
        ref.setRedirectId(AUTH_ID);
        return ref;
    }

    private CmsAisConsentResponse getCmsAisConsentResponse(ConsentStatus consentStatus) {
        return new CmsAisConsentResponse(getCmsAisAccountConsent(consentStatus), AUTH_ID, OK_URI, NOK_URI);
    }

    private CmsAisAccountConsent getCmsAisAccountConsent(ConsentStatus consentStatus) {
        return new CmsAisAccountConsent("123", new AisAccountAccess(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, null, null),
                                        false, DATE, EXPIRE_DATE, 5, null, consentStatus, false, true, AisConsentRequestType.BANK_OFFERED, null,
                                        null, null, false, Collections.singletonList(new AisAccountConsentAuthorisation("asd", null,
                                                                                                                        consentStatus == ConsentStatus.RECEIVED
                                                                                                                            ? ScaStatus.FAILED
                                                                                                                            : ScaStatus.FINALISED)),
                                        null, null, null, null);
    }

    private AccessTokenTO getAccessTokenTO() {
        AccessTokenTO tokenTO = new AccessTokenTO();
        tokenTO.setLogin(LOGIN);
        tokenTO.setConsent(getAisConsentTO(false));
        return tokenTO;
    }
}
