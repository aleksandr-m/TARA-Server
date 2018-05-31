package org.apereo.cas.oidc.token;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ee.ria.sso.authentication.AuthenticationType;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.OidcConstants;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.accesstoken.AccessToken;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.DigestUtils;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.web.support.WebUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Priit Serk: priit.serk@gmail.com
 * @since 5.1.4
 */

public class OidcIdTokenGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OidcIdTokenGeneratorService.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    private final String issuer;
    private final int skew;
    private final OidcIdTokenSigningAndEncryptionService signingService;

    public OidcIdTokenGeneratorService(final String issuer,
                                       final int skew,
                                       final OidcIdTokenSigningAndEncryptionService signingService) {
        this.signingService = signingService;
        this.issuer = issuer;
        this.skew = skew;
    }

    /**
     * Generate string.
     *
     * @param request           the request
     * @param response          the response
     * @param accessTokenId     the access token id
     * @param timeout           the timeout
     * @param responseType      the response type
     * @param registeredService the registered service
     * @return the string
     * @throws Exception the exception
     */
    public String generate(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final AccessToken accessTokenId,
                           final long timeout,
                           final OAuth20ResponseTypes responseType,
                           final OAuthRegisteredService registeredService) throws JoseException {

        final OidcRegisteredService oidcRegisteredService = (OidcRegisteredService) registeredService;

        final J2EContext context = WebUtils.getPac4jJ2EContext(request, response);
        final ProfileManager manager = WebUtils.getPac4jProfileManager(request, response);
        final Optional<UserProfile> profile = manager.get(true);

        LOGGER.debug("Attempting to produce claims for the id token [{}]", accessTokenId);
        final JwtClaims claims = produceIdTokenClaims(request, accessTokenId, timeout,
            oidcRegisteredService, profile.get(), context, responseType);
        LOGGER.debug("Produce claims for the id token [{}] as [{}]", accessTokenId, claims);

        return this.signingService.encode(oidcRegisteredService, claims);
    }

    /**
     * Produce id token claims jwt claims.
     *
     * @param request       the request
     * @param accessTokenId the access token id
     * @param timeout       the timeout
     * @param service       the service
     * @param profile       the user profile
     * @param context       the context
     * @param responseType  the response type
     * @return the jwt claims
     */
    protected JwtClaims produceIdTokenClaims(final HttpServletRequest request,
                                             final AccessToken accessTokenId,
                                             final long timeout,
                                             final OidcRegisteredService service,
                                             final UserProfile profile,
                                             final J2EContext context,
                                             final OAuth20ResponseTypes responseType) {
        final Authentication authentication = accessTokenId.getAuthentication();
        final Principal principal = authentication.getPrincipal();

        final JwtClaims claims = new JwtClaims();
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setIssuer(this.issuer);
        claims.setAudience(service.getClientId());

        final NumericDate expirationDate = NumericDate.now();
        expirationDate.addSeconds(timeout);
        claims.setExpirationTime(expirationDate);
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(this.skew);
        claims.setSubject(principal.getId());
        claims.setClaim("profile_attributes", filterAttributes(principal.getAttributes()));

        if (AuthenticationType.eIDAS.name().equals(principal.getAttributes().get("authenticationType"))) {
            String levelOfAssurance = (String) principal.getAttributes().get("levelOfAssurance");
            if (levelOfAssurance != null) claims.setStringClaim(OidcConstants.ACR, levelOfAssurance);
        }
        /*if (authentication.getAttributes().containsKey(casProperties.getAuthn().getMfa().getAuthenticationContextAttribute())) {
            final Collection<Object> val = CollectionUtils.toCollection(
                authentication.getAttributes().get(casProperties.getAuthn().getMfa().getAuthenticationContextAttribute()));
            claims.setStringClaim(OidcConstants.ACR, val.iterator().next().toString());
        }*/
        if (authentication.getAttributes().containsKey(AuthenticationHandler.SUCCESSFUL_AUTHENTICATION_HANDLERS)) {
            final Collection<Object> val = CollectionUtils.toCollection(principal.getAttributes().get("authenticationType"));
            claims.setStringListClaim(OidcConstants.AMR, val.toArray(new String[]{}));
        }

        claims.setClaim(OAuth20Constants.STATE, authentication.getAttributes().get(OAuth20Constants.STATE));
        claims.setClaim(OAuth20Constants.NONCE, authentication.getAttributes().get(OAuth20Constants.NONCE));
        claims.setClaim(OidcConstants.CLAIM_AT_HASH, generateAccessTokenHash(accessTokenId));

		/*principal.getAttributes().entrySet().stream()
                .filter(entry -> casProperties.getAuthn().getOidc().getClaims().contains(entry.getKey()))
				.forEach(entry -> claims.setClaim(entry.getKey(), entry.getValue
				()));*/

        return claims;
    }

    private static final Map<String, String> attributeMap = new HashMap<>();
    static {
        attributeMap.put("lastName", "family_name");
        attributeMap.put("firstName", "given_name");
        attributeMap.put("dateOfBirth", "date_of_birth");
        attributeMap.put("mobileNumber", "mobile_number");
    }

    private Map<String, Object> filterAttributes(Map<String, Object> inputAttributes) {
        Map<String, Object> attrs = new TreeMap(String.CASE_INSENSITIVE_ORDER);

        for (String inputAttributeName : inputAttributes.keySet()) {
            String mappedAttributeName = attributeMap.get(inputAttributeName);
            if (mappedAttributeName == null) continue;

            attrs.put(mappedAttributeName, inputAttributes.get(inputAttributeName));
        }

        if (attrs.get("date_of_birth") == null) {
            String principalCode = (String) inputAttributes.get("principalCode");
            if (principalCode != null && isEstonianIdCode(principalCode)) {
                String dateOfBirth = tryToExtractDateOfBirth(principalCode);
                if (dateOfBirth != null) attrs.put("date_of_birth", dateOfBirth);
            }
        }

        return attrs;
    }

    private boolean isEstonianIdCode(String principalCode) {
        return principalCode.length() == 13 && principalCode.startsWith("EE");
    }

    private String tryToExtractDateOfBirth(String estonianIdCode) {
        int sexAndCentury = Integer.parseUnsignedInt(estonianIdCode.substring(2, 3));
        if (sexAndCentury < 1 || sexAndCentury > 6) return null;

        int birthYear = Integer.parseUnsignedInt(estonianIdCode.substring(3, 5));
        birthYear += (1800 + ((sexAndCentury - 1) >>> 1) * 100);

        int birthMonth = Integer.parseUnsignedInt(estonianIdCode.substring(5, 7));
        if (birthMonth < 1 || birthMonth > 12) return null;

        int birthDay = Integer.parseUnsignedInt(estonianIdCode.substring(7, 9));
        if (birthDay < 1 || birthDay > 31) return null;

        return String.format("%04d-%02d-%02d", birthYear, birthMonth, birthDay);
    }

    private String generateAccessTokenHash(final AccessToken accessTokenId) {
        final byte[] tokenBytes = accessTokenId.getId().getBytes();
        final String hashAlg;

        switch (signingService.getJsonWebKeySigningAlgorithm()) {
            case AlgorithmIdentifiers.RSA_USING_SHA512:
                hashAlg = MessageDigestAlgorithms.SHA_512;
                break;
            case AlgorithmIdentifiers.RSA_USING_SHA256:
            default:
                hashAlg = MessageDigestAlgorithms.SHA_256;
        }

        LOGGER.debug("Digesting access token hash via algorithm [{}]", hashAlg);
        final byte[] digested = DigestUtils.rawDigest(hashAlg, tokenBytes);
        final byte[] hashBytesLeftHalf = Arrays.copyOf(digested, digested.length / 2);
        return EncodingUtils.encodeBase64(hashBytesLeftHalf);
    }
}

