package ro.etti.pki.platform.service.ca;

import ro.etti.pki.platform.config.properties.PkiProperties;

/** Creates loaded {@link CertificateAuthorityService} instances for tests outside this package. */
public final class TestCaServices {

    private TestCaServices() {
    }

    public static CertificateAuthorityService loaded(PkiProperties properties) {
        CertificateAuthorityService service = new CertificateAuthorityService(properties);
        service.loadCertificateAuthority();
        return service;
    }
}
