# Gateway Service - Merezh

Единая точка входа в микросервисную платформу. Отвечает за маршрутизацию, аутентификацию и авторизацию.

## 📋 Обзор

Gateway - это микросервис на Spring Boot, который служит **единственной точкой входа** для всех клиентских запросов. Он проверяет JWT-токены, извлекает `userId` и `role` и передаёт их во внутренние сервисы через заголовки `X-User-Id` и `X-User-Role`. Дополнительно Gateway выполняет **ролевую авторизацию** (например, эндпоинты `ADMIN`) и проксирует запросы к соответствующим сервисам.

Все внутренние сервисы (user, auth, wallet, order, payment) **не доступны извне** - они работают только внутри Docker-сети `dbnet`. Gateway - **единственный** контейнер, порт которого проброшен на хост.

## 🚀 Технологический стек

**Backend**

- Java 21 - основной язык
- Spring Boot 3 - фреймворк приложения
- Spring Security - аутентификация и авторизация
- RestTemplate - проксирование HTTP-запросов

**DevOps**

- Docker - контейнеризация
- Docker Compose - оркестрация нескольких контейнеров

## ✨ Возможности

### 🔐 Аутентификация и авторизация

- Проверка access-токена (подпись, `exp`, `type=access`)
- Извлечение `userId` и `role` из токена
- Ролевая авторизация на уровне Gateway (`hasRole("ADMIN")` для защищённых путей)
- `permitAll` для `/api/v1/auth/register`, `/login`, `/refresh`
- `AuthenticationEntryPoint` → 401 JSON
- `AccessDeniedHandler` → 403 JSON

### 🚦 Маршрутизация

- Проксирование всех запросов к соответствующим сервисам
- Проброс `X-User-Id` и `X-User-Role` в заголовки
- **Вырезание** клиентских `Authorization`, `X-User-Id`, `X-User-Role`, `Host`, `Content-Length`, `Cookie` и hop-by-hop заголовков
- Обработка ошибок сервисов (проброс `message` из ответа)
- Единый формат ошибок при недоступности сервиса (503)

### 🛡️ Безопасность

- **Подмена `X-User-Id` невозможна** - клиентский заголовок вырезается перед проксированием
- **Проверка `type` токена** - refresh-токен не принимается на защищённых эндпоинтах
- **Валидация подписи** - токен, подписанный другим секретом, отклоняется
- **Роли** - `hasRole("ADMIN")` для admin-эндпоинтов

## 🛠️ Быстрый старт

### Требования

- Docker
- Docker Compose

### Запуск через Docker Compose

```bash
docker compose up --build
```

Gateway будет доступен на **`http://localhost:8000`**.
Swagger path - `/swagger-ui.html`.

## 📚 Эндпоинты API

Gateway **не имеет собственных бизнес-эндпоинтов** - он проксирует запросы к сервисам. Единственные собственные эндпоинты - Actuator.

### Проксируемые пути

| Path pattern              | Сервис          | Access              |
|---------------------------|-----------------|---------------------|
| `/api/v1/auth/register`   | auth-service    | Public              |
| `/api/v1/auth/login`      | auth-service    | Public              |
| `/api/v1/auth/refresh`    | auth-service    | Public              |
| `/api/v1/auth/logout`     | auth-service    | Authenticated       |
| `/api/v1/users/**` (GET)  | user-service    | ADMIN               |
| `/api/v1/users/**` (DELETE) | user-service  | ADMIN               |
| `/api/v1/wallets/**`      | wallet-service  | Authenticated       |
| `/api/v1/orders/**`       | order-service   | Authenticated       |
| `/api/v1/payments/**`     | payment-service | Authenticated       |

**Внутренние эндпоинты** (`/users/create`, `/orders/update`, `/payments/place`) **не проксируются** через Gateway - они доступны только внутри Docker-сети для сервис-сервис взаимодействия.

## 📦 Структура проекта

```
src/main/java/ru/merezh/gateway/
├── config/                    # Spring configuration (RestTemplate, SecurityConfig)
├── controller/                # GatewayController (проксирование)
├── security/                  # JwtFilter, JwtValidateService
│   ├── dto/                   # JwtUserDto
│   └── service/               # JwtValidateService
└── exception/                 # Обработчики ошибок
```

## 🔒 Безопасность

- **Все запросы проходят через Gateway.** Внутренние сервисы не доступны извне.
- **JWT-валидация** - подпись, `exp`, `type=access`.
- **`X-User-Id` / `X-User-Role` вырезаются** из клиентских запросов.
- **Ролевая авторизация** - `hasRole("ADMIN")` для admin-путей.
- **`AuthenticationEntryPoint`** - 401 JSON для неаутентифицированных.
- **`AccessDeniedHandler`** - 403 JSON для неавторизованных.
- **Внутренние эндпоинты не проксируются** - `/users/create`, `/orders/update`, `/payments/place`.
- **Только порт Gateway проброшен на хост** - остальные сервисы внутри `dbnet`.

Готово. Теперь у тебя есть **русская версия** README для **gateway**.

Остался **общий README** (в корне проекта) с архитектурной диаграммой и описанием саги. Скажи, когда делать.
