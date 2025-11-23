package org.bozgeyik.paymentservice.service;

import org.bozgeyik.paymentservice.dto.UserCreateRequest;
import org.bozgeyik.paymentservice.dto.UserUpdateRequest;
import org.bozgeyik.paymentservice.model.User;
import org.bozgeyik.paymentservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Kullanıcı ile ilgili iş mantığını yöneten servis sınıfı.
 * Bu sınıf, kullanıcı oluşturma, getirme, güncelleme ve silme gibi işlemleri içerir.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * UserService yapıcısı (constructor).
     * Spring, bağımlılıkları (dependency injection) otomatik olarak enjekte eder.
     *
     * @param userRepository Kullanıcı veritabanı işlemleri için repository.
     * @param passwordEncoder Şifreleri güvenli bir şekilde hash'lemek için kullanılır.
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Yeni bir kullanıcı oluşturur ve veritabanına kaydeder.
     *
     * @param createRequest Kullanıcı oluşturmak için gerekli bilgileri içeren DTO (Data Transfer Object).
     * @return Oluşturulan ve veritabanına kaydedilen {@link User} nesnesi.
     * @throws IllegalArgumentException Eğer verilen e-posta adresi zaten kullanımdaysa.
     */
    public User createUser(UserCreateRequest createRequest) {
        // E-posta adresinin benzersiz olup olmadığını kontrol et.
        if (userRepository.existsByEmail(createRequest.getEmail())) {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kullanılıyor: " + createRequest.getEmail());
        }

        // Yeni bir User nesnesi oluştur.
        User user = new User();
        user.setName(createRequest.getName());
        user.setEmail(createRequest.getEmail());

        // Güvenlik Notu: Şifreyi asla açık metin olarak kaydetme.
        // PasswordEncoder kullanarak şifreyi hash'le.
        user.setPassword(passwordEncoder.encode(createRequest.getPassword()));

        // Benzersiz bir müşteri kimliği (customer_id) oluştur.
        // Bu, dış sistemlerle (örneğin, ödeme sağlayıcıları) entegrasyon için kullanılır.
        user.setCustomerId("cus_" + UUID.randomUUID().toString());

        // Kullanıcıyı veritabanına kaydet ve sonucu döndür.
        return userRepository.save(user);
    }

    /**
     * Belirtilen ID'ye sahip kullanıcıyı bulur.
     *
     * @param userId Aranan kullanıcının ID'si.
     * @return Bulunan {@link User} nesnesi.
     * @throws EntityNotFoundException Eğer belirtilen ID'ye sahip bir kullanıcı bulunamazsa.
     */
    public User getUserById(Long userId) {
        // Kullanıcıyı ID'ye göre bul. Bulunamazsa, EntityNotFoundException fırlat.
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı. ID: " + userId));
    }

    /**
     * Veritabanındaki tüm kullanıcıları listeler.
     *
     * @return Tüm kullanıcıları içeren bir {@link List<User>} listesi.
     */
    public List<User> getAllUsers() {
        // Tüm kullanıcıları veritabanından al ve döndür.
        return userRepository.findAll();
    }

    /**
     * Mevcut bir kullanıcının bilgilerini günceller.
     *
     * @param userId        Güncellenecek kullanıcının ID'si.
     * @param updateRequest Güncelleme için yeni bilgileri içeren DTO.
     * @return Güncellenmiş {@link User} nesnesi.
     * @throws EntityNotFoundException    Eğer belirtilen ID'ye sahip bir kullanıcı bulunamazsa.
     * @throws IllegalArgumentException Eğer yeni e-posta adresi başka bir kullanıcı tarafından zaten kullanılıyorsa.
     */
    public User updateUser(Long userId, UserUpdateRequest updateRequest) {
        // İlk olarak, güncellenecek kullanıcının veritabanında mevcut olduğundan emin ol.
        User existingUser = getUserById(userId);

        // Eğer istekte yeni bir e-posta adresi varsa ve bu adres mevcut e-postadan farklıysa...
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(existingUser.getEmail())) {
            // Yeni e-posta adresinin başka bir kullanıcı tarafından kullanılıp kullanılmadığını kontrol et.
            userRepository.findByEmail(updateRequest.getEmail()).ifPresent(user -> {
                // Eğer e-postayı kullanan kullanıcı, güncellediğimiz kullanıcı değilse, hata fırlat.
                if (!user.getId().equals(userId)) {
                    throw new IllegalArgumentException("Bu e-posta adresi başka bir kullanıcı tarafından kullanılıyor: " + updateRequest.getEmail());
                }
            });
            // E-posta adresini güncelle.
            existingUser.setEmail(updateRequest.getEmail());
        }

        // Eğer istekte yeni bir isim varsa, ismi güncelle.
        if (updateRequest.getName() != null) {
            existingUser.setName(updateRequest.getName());
        }

        // Güncellenmiş kullanıcı nesnesini veritabanına kaydet ve döndür.
        return userRepository.save(existingUser);
    }

    /**
     * Belirtilen ID'ye sahip kullanıcıyı siler.
     *
     * @param userId Silinecek kullanıcının ID'si.
     * @throws EntityNotFoundException Eğer belirtilen ID'ye sahip bir kullanıcı bulunamazsa.
     */
    public void deleteUser(Long userId) {
        // Silinecek kullanıcının var olup olmadığını kontrol et. Yoksa, getUserById zaten hata fırlatacaktır.
        User userToDelete = getUserById(userId);
        
        // Not: Eğer kullanıcı başka entity'lerle (örneğin, siparişler, ödemeler) ilişkiliyse,
        // bu ilişkileri de yönetmek gerekebilir (örneğin, silinmelerini engellemek veya kaskad silme uygulamak).
        // Bu örnekte, doğrudan silme işlemi yapılıyor.
        userRepository.delete(userToDelete);
    }
}
