package ro.etti.pki.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.etti.pki.platform.entity.Certificate;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Certificate} entity.
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /**
     * Finds a certificate by its unique serial number.
     * Used when verifying a PDF signature: the serial is extracted from the PDF
     * and the certificate is looked up in the DB.
     */
    Optional<Certificate> findBySerialNumber(String serialNumber);

    /**
     * Finds all certificates of a user.
     * Used by the UI so users can list their own certificates.
     */
    List<Certificate> findByUserId(Long userId);

    /**
     * Finds a certificate by id and owner at the same time.
     * Used by the PDF signing endpoint to check both existence and ownership
     * in a single query, avoiding LazyInitializationException with
     * {@code spring.jpa.open-in-view=false}.
     */
    Optional<Certificate> findByIdAndUserId(Long id, Long userId);
}
