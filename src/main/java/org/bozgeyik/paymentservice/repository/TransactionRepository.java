package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Finansal işlem (Transaction) entity'si için veritabanı işlemlerini yöneten repository arayüzü.
 * JpaRepository'yi genişleterek temel CRUD operasyonlarını sağlar ve özel sorgular içerir.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Belirtilen idempotency anahtarının veritabanında mevcut olup olmadığını kontrol eder.
     * Bu metot, aynı işlemin ağ sorunları gibi nedenlerle birden fazla kez işlenmesini önlemek (idempotency)
     * için kritik bir rol oynar. Bir işlem gönderilmeden önce bu anahtarın varlığı kontrol edilir.
     *
     * @param idempotencyKey İşlemin benzersizliğini garanti eden anahtar (genellikle bir UUID).
     * @return Eğer anahtar zaten mevcutsa {@code true}, aksi halde {@code false} döner.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Belirtilen cüzdan ID listelerine göre işlemleri (transaction) arar.
     * Bir işlem, kaynak cüzdan (fromWallet) VEYA hedef cüzdan (toWallet) olarak bu listelerden herhangi birinde
     * yer alan bir cüzdanı içeriyorsa, sorgu sonucuna dahil edilir.
     * Sonuçlar, verilen {@link Pageable} nesnesine göre sayfalanır ve sıralanır.
     *
     * @param fromWalletIds Kaynak cüzdan ID'leri listesi.
     * @param toWalletIds   Hedef cüzdan ID'leri listesi.
     * @param pageable      Sayfalama ve sıralama bilgilerini içeren nesne.
     * @return Belirtilen kriterlere uyan işlemleri içeren bir {@link Page<Transaction>} sayfası.
     */
    Page<Transaction> findByFromWalletIdInOrToWalletIdIn(
            List<Long> fromWalletIds,
            List<Long> toWalletIds,
            Pageable pageable
    );
}
