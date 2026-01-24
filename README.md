# Mini Classifieds Platform

Бекенд сервіс для оголошень (аналог OLX) з чистою архітектурою та SOLID принципами.

## 🚀 Технологічний стек

- **Java 21**
- **Spring Boot 4.0.1**
- **Spring Data JPA** (Hibernate)
- **PostgreSQL** / **H2** (для dev/test)
- **Flyway** (database migrations)
- **Maven**
- **Lombok**
- **JUnit 5** + **Mockito** + **AssertJ**

## 📐 Архітектура

Проект побудований за принципами **Clean Architecture** з чотирма основними шарами:

```
┌─────────────────────────────────────────┐
│         API Layer (Presentation)        │  ← Controllers, DTOs, Exception Handlers
├─────────────────────────────────────────┤
│       Application Layer (Use Cases)     │  ← Business Processes, Use Cases, Services
├─────────────────────────────────────────┤
│       Domain Layer (Core Business)      │  ← Entities, Value Objects, Rules
├─────────────────────────────────────────┤
│     Infrastructure Layer (Technical)    │  ← Persistence, File Storage, Config
└─────────────────────────────────────────┘
```

### Залежності між шарами:
- **API** → **Application** → **Domain**
- **Infrastructure** → **Domain** (через Ports/Interfaces)

## ✨ Основні можливості

- ✅ Створення оголошень зі статусом `DRAFT`
- ✅ Публікація оголошень (`DRAFT` → `PUBLISHED`) з підтримкою **Idempotency-Key**
- ✅ Завантаження фото (до 10 шт., формати: JPEG/PNG/WebP, макс. 2MB)
- ✅ Пошук оголошень з фільтрами та пагінацією
- ✅ Отримання деталей оголошення
- ✅ **Audit Log** для важливих подій
- ✅ **Request ID tracking** (X-Request-Id)
- ✅ **Optimistic Locking** для конкурентного доступу
- ✅ Уніфіковані помилки з правильними HTTP статусами

## 🏃 Швидкий старт

### Вимоги
- Java 21+
- Maven 3.6+

### Запуск в Development режимі (H2 Database)

#### Клонувати репозиторій
```bash
git clone https://github.com/dragster7422/ClassifiedsPlatform.git
cd ClassifiedsPlatform
```

#### Запустити проект
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Проект буде доступний на `http://localhost:8080`

**H2 Console**: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:classifieds_dev`
- Username: `sa`
- Password: _(порожній)_


### Запуск тестів

#### Всі тести
```bash
mvn test
```

#### Тільки integration tests
```bash
mvn test -Dtest="*IntegrationTest"
```


## 📡 API Endpoints

### Протестувати "API Endpoints" ви можете через застосунок Postman

---

### Доступні варіанти значень для сутностей
```java
public enum Currency {
    UAH,
    USD,
    EUR
}
```
```java
public enum Category {
    ELECTRONICS,
    REAL_ESTATE,
    VEHICLES,
    FASHION,
    HOME_GARDEN,
    SERVICES,
    PETS,
    SPORTS,
    OTHER
}
```

---

### Listings

#### Створити оголошення
```http
POST
http://localhost:8080/listings
```
```http
Content-Type: application/json

{
  "title": "MacBook Pro 2024",
  "description": "Новий MacBook Pro з M4 чіпом",
  "price": 2500.00,
  "currency": "USD",
  "category": "ELECTRONICS"
}

Response: 201 Created
```

#### Опублікувати оголошення
```http
POST
http://localhost:8080/listings/{id}/publish
```
```http
Content-Type: application/json
Idempotency-Key: unique-key-123

Response: 200 OK
```

#### Отримати список оголошень
```http
GET
http://localhost:8080/listings?query=macbook&category=ELECTRONICS&status=PUBLISHED&page=0&size=20&sortBy=createdAt&sortDirection=desc
```
```http
Response: 200 OK

{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

#### Отримати деталі оголошення
```http
GET
http://localhost:8080/listings/{id}

Response: 200 OK
```

### Photos

#### Завантажити фото (Для одного оголошення, можна завантажити всього 10 фото у форматі [ jpg, png, webp ])
```http
POST
http://localhost:8080/listings/{listingId}/photos
```
```http
Content-Type: multipart/form-data

files: Зображення

Response: 201 Created
```

## 🗂️ Структура проекту

```
src/main/java/com/classifieds/
├── api/                           # API Layer
│   ├── controller/                # REST Controllers
│   ├── dto/                       # Request/Response DTOs
│   ├── exception/                 # Global Exception Handler
│   └── mapper/                    # DTO ↔ Domain mappers
│
├── application/                   # Application Layer
│   ├── usecase/                   # Use Cases (бізнес-процеси)
│   ├── service/                   # Services (AuditLog, Idempotency)
│   └── port/
│       ├── in/                    # Input Ports (Commands/Queries)
│       └── out/                   # Output Ports (Repository interfaces)
│
├── domain/                        # Domain Layer
│   ├── event/                     # Domain Events
│   ├── exception/                 # Domain Exceptions
│   └── model/                     # Aggregates, Entities, Value Objects
│
└── infrastructure/                # Infrastructure Layer
    ├── config/                    # Configuration
    ├── persistence/               # JPA Entities, Repositories, Adapters
    │   ├── adapter/               # Repository implementations
    │   ├── entity/                # JPA Entities
    │   ├── mapper/                # Entity ↔ Domain mappers
    │   └── repository/            # Spring Data JPA Repositories
    └── storage/                   # File Storage

src/main/resources/
└── db/migration/                  # Flyway migrations
    ├── V1__create_listings_table.sql
    ├── V2__create_listing_photos_table.sql
    ├── V3__create_audit_log_table.sql
    └── V4__create_idempotency_table.sql
```

## 🎯 Domain-Driven Design

### Aggregates & Entities
- **Listing** - контролює всі бізнес-правила та переходи станів
- **Photo** - зв'язана з Listing
- **AuditLog** - логування подій
- **IdempotencyRecord** - ідемпотентність

### Value Objects
- **Money** - amount + currency з валідацією
- **PhotoMetadata** - filename, contentType, size з валідацією
- **Currency**, **Category**, **ListingStatus** - enums

### Domain Events
- **ListingPublishedEvent** - тригериться при публікації
- **PhotoUploadedEvent** - тригериться при завантаженні фото

### Factory Methods
- `create()` - для створення НОВИХ сутностей через бізнес-логіку
- `reconstitute()` - для ВІДНОВЛЕННЯ з persistence layer

## 🔒 Безпека та надійність

- **Optimistic Locking** - `@Version` на Listing для конкурентного доступу
- **Idempotency Keys** - дедуплікація повторних publish запитів
- **File Validation** - формат, розмір, ліміт кількості
- **Domain Validation** - бізнес-правила в Domain Layer
- **Request ID Tracking** - X-Request-Id в кожному запиті/логі

## 📊 Database Schema

```sql
listings 
(id, title, description, price_amount, price_currency, category, status, created_at, updated_at, version)
          
photos
(id, listing_id, filename, content_type, file_size, storage_path, created_at)
        
audit_log 
(id, event_type, listing_id, payload_json, created_at)

idempotency_records 
(id, idempotency_key, listing_id, result_json, http_status, created_at, expires_at)
```

## 📚 Додаткова документація

- [Design Notes](DESIGN_NOTES.md) - ключові рішення, компроміси та що можна покращити.