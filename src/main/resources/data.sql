-- data.sql

-- Örnek Hesaplar (Account tablosu için)
-- Account entity'nizin sütunlarına göre uyarlayın (id, account_number, balance, currency, owner_name vb.)
-- ID'ler otomatik artan ise, ID sütununu eklemeyebilirsiniz (veritabanı türüne bağlı)
INSERT INTO account (id, account_number, balance, currency, owner_name, created_at, updated_at) VALUES
                                                                                                    (1, 'TR001', 1000.00, 'TRY', 'Ali Veli', NOW(), NOW()),
                                                                                                    (2, 'TR002', 2500.50, 'TRY', 'Ayşe Yılmaz', NOW(), NOW()),
                                                                                                    (3, 'TR003', 500.75, 'TRY', 'Mehmet Kaya', NOW(), NOW()),
                                                                                                    (4, 'TR004', 10000.00, 'TRY', 'Zeynep Demir', NOW(), NOW()),
                                                                                                    (5, 'TR005', 750.00, 'TRY', 'Can Bilen', NOW(), NOW());

-- Örnek İşlemler (Transaction tablosu için)
-- Transaction entity'nizin sütunlarına göre uyarlayın (id, from_account, to_account, amount, currency, description, transaction_type, status, transaction_date)
INSERT INTO transaction (id, from_account, to_account, amount, currency, description, transaction_type, status, transaction_date) VALUES
                                                                                                                                      (1, NULL, 'TR001', 500.00, 'TRY', 'İlk para yatırma', 'DEPOSIT', 'COMPLETED', NOW() - INTERVAL '3 day'),
                                                                                                                                      (2, 'TR002', NULL, 100.00, 'TRY', 'ATMden çekim', 'WITHDRAWAL', 'COMPLETED', NOW() - INTERVAL '2 day'),
                                                                                                                                      (3, 'TR001', 'TR003', 200.00, 'TRY', 'Kira ödemesi', 'TRANSFER', 'COMPLETED', NOW() - INTERVAL '1 day'),
                                                                                                                                      (4, NULL, 'TR004', 2000.00, 'TRY', 'Maaş yatırıldı', 'DEPOSIT', 'COMPLETED', NOW()),
                                                                                                                                      (5, 'TR005', 'TR001', 50.00, 'TRY', 'Arkadaşa borç', 'TRANSFER', 'COMPLETED', NOW() - INTERVAL '5 hour');

-- Eğer ID'leriniz otomatik artan (auto-increment) ise ve JPA bunları yönetiyorsa,
-- INSERT sorgularında ID'leri belirtmeyebilirsiniz.
-- Örneğin:
-- INSERT INTO account (account_number, balance, currency, owner_name, created_at, updated_at) VALUES
-- ('TR006', 150.00, 'TRY', 'Fatma Gül', NOW(), NOW());-- data.sql
--
-- -- Örnek Hesaplar (Account tablosu için)
-- -- Account entity'nizin sütunlarına göre uyarlayın (id, account_number, balance, currency, owner_name vb.)
-- -- ID'ler otomatik artan ise, ID sütununu eklemeyebilirsiniz (veritabanı türüne bağlı)
-- INSERT INTO account (id, account_number, balance, currency, owner_name, created_at, updated_at) VALUES
-- (1, 'TR001', 1000.00, 'TRY', 'Ali Veli', NOW(), NOW()),
-- (2, 'TR002', 2500.50, 'TRY', 'Ayşe Yılmaz', NOW(), NOW()),
-- (3, 'TR003', 500.75, 'TRY', 'Mehmet Kaya', NOW(), NOW()),
-- (4, 'TR004', 10000.00, 'TRY', 'Zeynep Demir', NOW(), NOW()),
-- (5, 'TR005', 750.00, 'TRY', 'Can Bilen', NOW(), NOW());
--
-- -- Örnek İşlemler (Transaction tablosu için)
-- -- Transaction entity'nizin sütunlarına göre uyarlayın (id, from_account, to_account, amount, currency, description, transaction_type, status, transaction_date)
-- INSERT INTO transaction (id, from_account, to_account, amount, currency, description, transaction_type, status, transaction_date) VALUES
-- (1, NULL, 'TR001', 500.00, 'TRY', 'İlk para yatırma', 'DEPOSIT', 'COMPLETED', NOW() - INTERVAL '3 day'),
-- (2, 'TR002', NULL, 100.00, 'TRY', 'ATMden çekim', 'WITHDRAWAL', 'COMPLETED', NOW() - INTERVAL '2 day'),
-- (3, 'TR001', 'TR003', 200.00, 'TRY', 'Kira ödemesi', 'TRANSFER', 'COMPLETED', NOW() - INTERVAL '1 day'),
-- (4, NULL, 'TR004', 2000.00, 'TRY', 'Maaş yatırıldı', 'DEPOSIT', 'COMPLETED', NOW()),
-- (5, 'TR005', 'TR001', 50.00, 'TRY', 'Arkadaşa borç', 'TRANSFER', 'COMPLETED', NOW() - INTERVAL '5 hour');
--
-- -- Eğer ID'leriniz otomatik artan (auto-increment) ise ve JPA bunları yönetiyorsa,
-- -- INSERT sorgularında ID'leri belirtmeyebilirsiniz.
-- -- Örneğin:
-- -- INSERT INTO account (account_number, balance, currency, owner_name, created_at, updated_at) VALUES
-- -- ('TR006', 150.00, 'TRY', 'Fatma Gül', NOW(), NOW());