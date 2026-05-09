# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MallChat is an IM (instant messaging) + e-commerce system with Netty-based WebSocket real-time communication, WeChat login integration, RocketMQ async messaging, and various self-built infrastructure components (distributed locking, rate limiting, transaction-safe execution).

## Build & Run Commands

```bash
# Build the entire project (tests are skipped by default)
mvn clean install -DskipTests

# Build a specific module
mvn clean install -pl mallchat-chat-server -am -DskipTests

# Run the application (config in application-{profile}.properties)
# The main class is in mallchat-chat-server:
# com.abin.mallchat.common.MallchatCustomApplication

# Run with test profile
mvn spring-boot:run -pl mallchat-chat-server -Dspring-boot.run.profiles=test
```

Config profiles: `test` (local dev), `pro` (production). Fill credentials in `application-test.properties` or `application-pro.properties`.

## Module Structure

### mallchat-chat-server (main application)
- `common.chat` — Chat rooms, messages, contacts, group members. Strategy pattern via `MsgHandlerFactory` + `AbstractMsgHandler<Req>` for different message types (text, image, file, video, sound, emoji, recall, system).
- `common.user` — User management, friends, emoji, badges/items, OSS upload, WebSocket auth. WeChat MP login flow through `WxMpService`.
- `common.chatai` — AI chat integration (ChatGPT, ChatGLM). Token counting, configurable per-user rate limits.
- `common.common` — Shared infrastructure: annotations (`@RedissonLock`, `@FrequencyControl`), events/listeners (Spring `ApplicationEventPublisher`), exception hierarchy, domain base classes, configs (thread pools, Redis, MyBatis Plus, Swagger, Caffeine, sensitive word).
- `common.websocket` — Netty WebSocket server on port 8090. `NettyWebSocketServer` (Spring `@Configuration` with `@PostConstruct`/`@PreDestroy`), `NettyWebSocketServerHandler` (message dispatch), `HttpHeadersHandler` (IP capture).
- `common.sensitive` — Sensitive word detection with AC automaton + DFA algorithm, loaded from `sensitive_word` table.

### mallchat-tools (infrastructure modules)
- `mallchat-common-starter` — `RedisUtils`, `JsonUtils`, `SpElUtils`
- `mallchat-transaction` — `@SecureInvoke` for transaction-safe async execution with retry via `SecureInvokeService`
- `mallchat-frequency-control` — Rate limiting: `@FrequencyControl` annotation + AOP aspect, supports fixed window, sliding window, token bucket, leaky bucket strategies
- `mallchat-oss-starter` — MinIO object storage auto-configuration (`MinIOTemplate`)
- `mallchat-redis` — Redis support

## Key Architectural Patterns

**Layered structure per domain**: `controller` → `service` (interface + impl) → `dao` → `mapper` (MyBatis Plus). Domain objects in `domain/entity`, `domain/vo`, `domain/dto`, `domain/enums`.

**Adapter pattern**: `*Adapter` classes in `service/adapter/` convert between entities, DTOs, and VOs (e.g., `MessageAdapter`, `UserAdapter`, `WSAdapter`, `RoomAdapter`).

**Strategy pattern** — Three main factories:
- `MsgHandlerFactory` + `AbstractMsgHandler` per message type
- `MsgMarkFactory` + `AbstractMsgMarkStrategy` for likes/dislikes
- `FrequencyControlStrategyFactory` for rate limiting algorithms

**Cache pattern** — "旁路缓存" (cache-aside) with Redis + Caffeine. Cache classes per domain (e.g., `UserCache`, `RoomCache`, `MsgCache`) use raw `RedisUtils` calls. `@Cacheable`/`@CacheEvict` on Spring Caffeine cache for stable, low-churn data (blacklist, roles).

**Event-driven architecture** — Spring events in `common.common.event` and listeners in `common.common.event.listener`. Events: `MessageSendEvent`, `UserOnlineEvent`, `UserOfflineEvent`, `UserRegisterEvent`, `ItemReceiveEvent`, `MessageMarkEvent`, `MessageRecallEvent`, `UserApplyEvent`, `UserBlackEvent`, `GroupMemberAddEvent`.

**Async messaging** — RocketMQ for cross-process communication. Consumers in `*.consumer` packages (e.g., `PushConsumer` for WebSocket push, `MsgLoginConsumer` for login handling).

## Inter-Module Communication

1. **WebSocket** (Netty, port 8090) — Real-time bidirectional messaging. Messages routed through `WebSocketService.sendToUid()` / `sendToAllOnline()`
2. **REST API** (Spring MVC) — Standard CRUD via `@RestController`
3. **RocketMQ** — Topics defined in `MQConstant`. Push topic uses broadcast mode
4. **Spring Events** — In-process pub/sub for decoupling within the same JVM

## Tech Stack

- **Spring Boot 2.6.7** / Java 8
- **MyBatis Plus 3.4** (ORM, code generation, pagination)
- **Netty 4.1.76** (WebSocket server)
- **RocketMQ** (async messaging and push)
- **Redis** + **Redisson 3.17** (caching, distributed locks)
- **Caffeine** (local cache)
- **WeChat MP SDK** (wx-java-mp 4.4, login)
- **MinIO** (object storage)
- **Hutool 5.8** (utility library)
- **JWT** (token auth)
- **Swagger 3.0** (API docs)
- **MySQL 8.0** (primary database)
