package ro.etti.pki.platform.service.crypto;

import ro.etti.pki.platform.config.properties.PkiProperties;

/** Creates initialized {@link CryptoService} instances for tests outside this package. */
public final class TestCryptoServices {

    private TestCryptoServices() {
    }

    public static CryptoService initialized(PkiProperties properties) {
        CryptoService service = new CryptoService(properties);
        service.init();
        return service;
    }
}
