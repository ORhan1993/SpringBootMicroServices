package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı (User) entity'si için veritabanı işlemlerini yöneten repository arayüzü.
 * Spring Data JPA'nın JpaRepository arayüzünü genişleterek standart CRUD (Create, Read, Update, Delete)
 * işlemlerini ve daha fazlasını kolayca gerçekleştirmemizi sağlar.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Belirtilen e-posta adresine sahip kullanıcıyı veritabanında arar.
     * Spring Data JPA, metot isminden yola çıkarak "findByEmail" sorgusunu otomatik olarak oluşturur.
     *
     * @param email Aranan kullanıcının e-posta adresi.
     * @return Eğer kullanıcı bulunursa, kullanıcıyı içeren bir {@link Optional} nesnesi döner. Bulunamazsa, boş bir {@link Optional} döner.
     */
    Optional<User> findByEmail(String email);

    /**
     * Belirtilen e-posta adresinin veritabanında mevcut olup olmadığını kontrol eder.
     * Bu metot, özellikle yeni bir kullanıcı kaydı oluşturulurken e-posta adresinin benzersizliğini
     * (unique) doğrulamak için verimli bir yoldur.
     *
     * @param email Kontrol edilecek e-posta adresi.
     * @return Eğer e-posta adresi zaten bir kullanıcı tarafından kullanılıyorsa {@code true}, aksi halde {@code false} döner.
     */
    boolean existsByEmail(String email);

    /**
     * Belirtilen müşteri kimliğine (Customer ID) sahip kullanıcıyı veritabanında arar.
     * Müşteri kimliği, genellikle dış sistemlerle entegrasyon için kullanılan, kullanıcıya özel benzersiz bir tanımlayıcıdır.
     *
     * @param customerId Aranan kullanıcının müşteri kimliği (örn: "cus_12345").
     * @return Eğer kullanıcı bulunursa, kullanıcıyı içeren bir {@link Optional} nesnesi döner. Bulunamazsa, boş bir {@link Optional} döner.
     */
    Optional<User> findByCustomerId(String customerId);
}
