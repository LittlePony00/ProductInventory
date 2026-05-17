# ProductInventory: подробные вопросы и ответы для защиты

## 1. Какую проблему решает проект?

Проект решает проблему совместного учета домашних продуктов. В обычной семье продукты покупаются несколькими людьми, лежат в разных местах, быстро забываются, заканчиваются неожиданно или портятся. Простая заметка или таблица плохо работает, потому что нет разграничения по домохозяйствам, нет уведомлений, нет синхронизации между устройствами, нет сканирования штрихкодов и нет связи с рецептами.

`ProductInventory` строит полный цикл: пользователь создает домохозяйство, добавляет продукты, видит остатки и сроки годности, расходует продукты, получает realtime-обновления на других устройствах, получает push-уведомления о новых событиях, малом остатке и сроках годности, а также получает рецепты на основе текущих запасов. Это не просто CRUD-приложение, а система учета с синхронизацией, офлайн-режимом, внешними источниками данных и AI-enhancement.

## 2. Почему проект разделен на `server`, `shared`, `composeApp` и `iosApp`?

Такое разделение отражает реальные границы ответственности.

`server` отвечает за авторитетное состояние: пользователи, домохозяйства, продукты, категории, уведомления, refresh tokens, device tokens, рецептные документы, безопасность и внешние интеграции.

`shared` содержит общую KMP-логику: repositories, use cases, DTO mapping, Ktor client, token refresh, offline queue, ViewModels и state machines. Это позволяет Android и iOS вести себя одинаково без копирования бизнес-логики.

`composeApp` - Android host: Compose UI, typed navigation, Room, CameraX, ML Kit, notification permission и Firebase Messaging service.

`iosApp` - SwiftUI host: нативная навигация, SwiftUI screens, bridge к KMP ViewModels, APNs/FCM integration.

Если бы все было в одном модуле, проект стал бы сложнее собирать, тестировать и развивать. Если бы Android и iOS были полностью независимыми, пришлось бы дважды реализовывать и поддерживать одни и те же use cases.

## 3. Почему выбран Kotlin Multiplatform?

KMP выбран потому, что основная сложность проекта лежит не только в UI, а в бизнес-логике: авторизация, refresh-token flow, repository layer, offline queue, DTO mapping, realtime event mapping, фильтры продуктов, presentation state machines. Эти части одинаковы для Android и iOS.

KMP дает:

- единые модели и use cases;
- единые тесты common логики;
- меньше расхождений поведения между платформами;
- возможность оставить UI нативным: Compose на Android и SwiftUI на iOS;
- общий Ktor client и единый refresh-token механизм.

Альтернатива - две независимые native реализации. Она проще на старте, но дороже при развитии: каждую ошибку нужно исправлять дважды, каждое изменение API маппить дважды, каждую state machine проверять отдельно.

## 4. Почему iOS не сделан полностью на Compose Multiplatform UI?

Потому что в этом проекте разумнее разделить business reuse и platform UX. KMP используется для общей логики, а SwiftUI - для iOS shell. Такой подход дает нативную навигацию, привычный iOS lifecycle, нормальную интеграцию с APNs/Firebase и SwiftUI previews/tests, при этом не дублирует repositories/use cases/ViewModels.

Compose Multiplatform UI на iOS мог бы уменьшить количество UI-кода, но в учебном/защитном проекте важно показать зрелую архитектуру: общая логика там, где она действительно общая, и нативный UI там, где платформа имеет сильные особенности.

## 5. Почему backend построен как hexagonal/Clean architecture?

Backend зависит от нескольких внешних технологий: REST, JPA/PostgreSQL, Firebase Cloud Messaging, GigaChat, OpenFoodFacts, GS1, SSE. Если смешать все это в сервисах напрямую, бизнес-логика будет зависеть от деталей HTTP, таблиц, FCM payload и AI API.

Hexagonal architecture решает это через порты:

- inbound ports описывают use cases;
- outbound ports описывают необходимые внешние зависимости;
- application services реализуют сценарии;
- infrastructure adapters подключают REST, JPA, FCM, AI и barcode providers.

Преимущество: `ProductServiceImpl`, `ReminderServiceImpl`, `AuthServiceImpl` можно тестировать изолированно. Можно заменить FCM sender, GigaChat client или barcode provider без переписывания бизнес-правил.

## 6. Почему не хватило бы обычного `Controller -> Service -> Repository`?

Для маленького CRUD хватило бы. Но здесь есть:

- проверка membership для каждого household-scoped сценария;
- события realtime;
- in-app notification store;
- push delivery;
- barcode provider chain;
- AI enrichment fallback;
- RAG-like recipe retrieval;
- offline mobile sync;
- JWT/refresh-token security;
- миграции базы.

Обычная схема `Controller -> Service -> Repository` быстро превратилась бы в сервисы, которые одновременно знают DTO, JPA entity, FCM JSON и внешние AI responses. Текущая архитектура отделяет эти детали и делает код объяснимым на защите.

## 7. Как работает регистрация и вход?

При регистрации `AuthServiceImpl.register` проверяет уникальность email, нормализует email, хеширует пароль через BCrypt и сохраняет `User`. Затем `generateTokens` создает JWT access token и refresh token. Refresh token сохраняется в базе.

При входе `login` ищет пользователя по email, проверяет пароль через `PasswordEncoder.matches`, затем выдает такую же пару токенов.

Access token используется для авторизации REST/SSE запросов. Refresh token нужен для получения новой пары при истечении access token.

## 8. Как работает refresh token и почему это безопаснее, чем бесконечный JWT?

Access token короткоживущий и stateless. Refresh token хранится в базе, имеет срок действия и может быть отозван. При refresh backend:

1. Находит refresh token.
2. Проверяет `revoked`.
3. Проверяет `expiresAt`.
4. Находит пользователя.
5. Отзывает старые refresh tokens пользователя.
6. Создает новую пару tokens.

Бесконечный JWT нельзя отозвать без blacklist. Здесь refresh token контролируется сервером, поэтому компрометацию проще ограничить.

## 9. Как backend понимает, какой пользователь делает запрос?

`JwtAuthenticationFilter` извлекает `Authorization: Bearer ...`, валидирует JWT через `JwtTokenProvider`, берет `userId` из subject и кладет authentication в `SecurityContext`. Контроллеры вызывают `currentUserId()`, который достает UUID текущего пользователя из authentication principal.

Важно: наличие валидного JWT не означает доступ ко всем данным. Application services дополнительно проверяют membership в конкретном household.

## 10. Как защищены данные разных домохозяйств?

Каждый household-scoped use case вызывает проверку membership через `IMembershipRepository.findByUserIdAndHouseholdId`. Например, `ProductServiceImpl`, `HouseholdServiceImpl`, `CategoryServiceImpl`, `BarcodeProductServiceImpl`, `ProductEnrichmentServiceImpl`, `ReminderServiceImpl` проверяют, что пользователь является участником домохозяйства.

Это означает, что пользователь не может просто подставить чужой `householdId` в URL. Даже если endpoint аутентифицирован, service layer отклонит доступ.

## 11. Почему домохозяйство является основной границей данных?

Продукты, категории, участники, invite codes, realtime events и household reminders логически принадлежат домохозяйству. Пользователь может состоять в нескольких household, поэтому нельзя хранить продукты только на user level. Household boundary позволяет:

- вести общий список семьи;
- приглашать участников;
- разделять разные группы, например дом и дача;
- синхронизировать события только нужным подписчикам;
- проверять доступ через membership.

## 12. Как работает invite code?

Owner household вызывает endpoint генерации invite code. Backend создает 8-символьный uppercase code, привязанный к household, создателю и сроку истечения 7 дней. При вступлении backend проверяет, что code существует, не used и не expired. Если пользователь уже состоит в household, backend не создает дубль membership. Если пользователь новый, он получает роль `MEMBER`, invite помечается used, участникам отправляется уведомление, а realtime layer получает `MEMBER_JOINED`.

## 13. Почему owner не может просто уйти из домохозяйства с участниками?

Потому что household должен иметь понятного владельца. В текущей реализации owner может уйти только если он единственный участник; тогда household удаляется. Если есть другие участники, сервис запрещает уход owner до удаления/передачи участников. Это предотвращает “осиротевшие” домохозяйства без владельца.

## 14. Как работает добавление продукта?

Mobile ViewModel собирает данные формы и вызывает shared `AddProductUseCase`, который идет в `ProductRepositoryImpl`. Repository пытается синхронизировать pending offline actions, затем отправляет `POST /api/v1/households/{householdId}/products`.

Backend `ProductServiceImpl.addProduct`:

- проверяет membership;
- разрешает category/categoryId;
- создает доменный `Product`;
- сохраняет его через `IProductRepository`;
- публикует `PRODUCT_CREATED`;
- публикует `INVENTORY_LOW` или `EXPIRING_SOON`, если продукт уже в таком состоянии;
- создает уведомления для участников;
- отправляет push с backend notification id;
- сохраняет barcode metadata, если barcode есть.

Ответ возвращается клиенту, shared repository кладет продукт в локальный cache.

## 15. Как работает редактирование продукта?

Backend сначала загружает существующий продукт и проверяет membership. Затем применяет только переданные поля, сохраняя старые значения для `null`. Если меняется категория, backend разрешает новую category/categoryId и публикует `CATEGORY_CHANGED`. После сохранения публикуется `PRODUCT_UPDATED`, а критические state events публикуются только при переходе из нормального состояния в низкий остаток или близкий срок.

Такой подход предотвращает лишний шум: если продукт уже был low-stock, повторный update не должен бесконечно генерировать `INVENTORY_LOW`.

## 16. Как работает расходование продукта?

Клиент отправляет amount. Backend проверяет, что amount положительный и не превышает `remainingAmount`. Затем сохраняет продукт с уменьшенным остатком. Всегда публикуется `PRODUCT_QUANTITY_CHANGED`. Если раньше остаток был больше нуля, а теперь стал ноль, дополнительно публикуется `PRODUCT_DEPLETED`.

`PRODUCT_DEPLETED` важен для рекомендаций: depleted продукт не должен участвовать в подборе рецептов.

## 17. Почему у продукта есть и `quantity`, и `remainingAmount`?

`quantity` описывает исходное количество/упаковку, например “1 литр” или “500 грамм”. `remainingAmount` описывает текущий остаток. Без `remainingAmount` приложение могло бы только знать, что продукт был куплен, но не могло бы корректно учитывать частичное расходование.

Это особенно важно для low-stock reminders и рецептов: рецепт должен учитывать то, что осталось, а не только то, что когда-то было куплено.

## 18. Как работают категории?

Есть системные категории с фиксированными UUID и legacy enum `ProductCategory`: dairy, meat/fish, vegetables/fruits, cereals, beverages, other. Есть пользовательские категории household. Backend возвращает системные плюс пользовательские, а продукт хранит `categoryId`, legacy code и category name.

Системные категории нельзя редактировать и архивировать. Пользовательские можно создавать, переименовывать и архивировать. Архивация выбрана вместо физического удаления, чтобы не ломать историю и связи продуктов.

## 19. Почему не использовать только строку категории?

Свободная строка привела бы к дублям: “молоко”, “Молочка”, “dairy”, “молочные продукты”. Фильтрация и аналитика стали бы ненадежными. `categoryId` дает стабильную связь, а `categoryName` дает человекочитаемое название. Legacy enum сохраняет совместимость с существующей логикой и rules.

## 20. Как работает сканирование штрихкода?

На Android `BarcodeScannerScreen` использует CameraX для camera preview и ML Kit Barcode Scanning для распознавания barcode. После распознавания shared `BarcodeScanViewModel` вызывает lookup use case. Backend проверяет membership и ищет draft:

1. В barcode cache.
2. В provider chain: локальная база, OpenFoodFacts, GS1.
3. Если ничего нет, возвращает пустой draft.
4. Если category отсутствует, применяет rules, GigaChat и fallback `OTHER`.

После draft lookup приложение открывает `AddProductScreen` с предзаполненными полями.

## 21. Что происходит, если barcode не найден во внешних базах?

Это не блокирующая ошибка. Backend возвращает empty draft с barcode и низкой confidence. Пользователь вручную заполняет форму. После сохранения продукта backend сохраняет barcode metadata в локальный cache. В следующий раз этот barcode может быть найден быстрее и с лучшим качеством.

## 22. Как работает product enrichment через AI?

Клиент отправляет неполные данные продукта: name, brand, barcode, ingredients. Backend передает GigaChat список доступных категорий household. Если AI возвращает валидную категорию по id, code или name, backend формирует suggestion. Если AI недоступен или возвращает невалидный ответ, backend использует rule matcher. Если и rules не сработали, возвращается fallback `OTHER`.

AI здесь не обязательная часть функциональности, а enhancement. Приложение продолжает работать без GigaChat.

## 23. Почему нужен rule-based fallback, если есть GigaChat?

Внешний AI может быть недоступен, может вернуть невалидный JSON, может попасть в rate limit или дать нерелевантный ответ. Rule-based matcher дешевый, быстрый, тестируемый и предсказуемый. Он закрывает распространенные категории по названию/ингредиентам. Fallback делает систему надежной: user flow не ломается при отказе AI.

## 24. Как работает подбор рецептов?

Backend получает продукты household, передает их в `IRecipeProvider`. `GigaChatRecipeProvider` сначала вызывает `RecipeRetriever`, который ищет совпадения в `recipe_documents`. Retrieval считает score по совпадению ингредиентов, срокам годности, low-stock и покрытию категорий. Затем provider пытается сгенерировать рецепт через GigaChat с найденными документами как контекстом.

Если GigaChat недоступен, rate limited или вернул невалидный JSON, пользователь получает deterministic fallback - рецепты из recipe documents.

## 25. Почему это можно назвать RAG-like подходом?

Потому что перед генерацией идет retrieval: система выбирает релевантные документы рецептов из локальной базы знаний, а затем передает их в prompt как контекст. Это не полноценная vector RAG с embeddings, но архитектурно похоже: сначала извлечение знаний, затем генерация или fallback.

Преимущество: меньше hallucination, есть тестируемые seed recipes, можно работать без AI.

## 26. Как работает realtime-синхронизация?

Backend публикует `HouseholdEvent` при изменениях продуктов и membership. `SpringHouseholdEventPublisher` отправляет application event, transactional listener после commit передает его в `HouseholdEventSseBroadcaster`. Broadcaster держит SSE emitters по household, отправляет event с id/type/data и сохраняет последние 512 events.

Клиент `KtorSseRealtimeEventSource` подключается к `/events`, передает `Last-Event-ID`, дедуплицирует события и дополнительно polling-ом запрашивает `/events/missed`. `ProductListViewModel` применяет create/update/delete или вызывает resync.

## 27. Почему SSE, а не WebSocket?

Сценарий realtime однонаправленный: сервер сообщает клиенту об изменениях, а клиентские команды уже идут через REST. SSE проще:

- работает поверх HTTP;
- имеет event id;
- удобно реализует reconnect;
- проще тестируется;
- не требует отдельного двунаправленного протокола.

WebSocket полезен для чата или интерактивного двунаправленного обмена. Здесь он усложнил бы архитектуру без существенной выгоды.

## 28. Что будет, если SSE-соединение оборвалось?

Клиент хранит last event id. При reconnect он передает `Last-Event-ID`. Backend replay-ит missed events из per-household history. Дополнительно shared client каждые 3 секунды вызывает `/events/missed`, чтобы догрузить события, если streaming временно недоступен. Если событие невозможно безопасно применить, mapper возвращает `ResyncRequired`, и список продуктов перезагружается.

## 29. Как работает офлайн-режим продуктов?

`ProductRepositoryImpl` сначала пытается выполнить pending actions. Если remote request успешен, результат сохраняется в local cache. Если сеть недоступна:

- add создает локальный продукт с временным UUID и кладет `ADD_PRODUCT` в queue;
- update применяет изменения к cached product и кладет `UPDATE_PRODUCT`;
- consume уменьшает локальный остаток и кладет `CONSUME_PRODUCT`;
- delete удаляет локально и кладет `DELETE_PRODUCT`.

При следующем sync actions выполняются по `createdAt`. Если server вернул настоящий UUID для локально созданного продукта, repository remap-ит последующие queued actions с временного id на server id.

## 30. Является ли offline sync полноценным conflict-free алгоритмом?

Нет. Это осознанный pragmatic offline-first подход, а не CRDT. Он сохраняет намерения пользователя при временной потере сети и синхронизирует их в порядке создания. Сложное разрешение конфликтов, когда несколько пользователей одновременно меняют один продукт, пока не реализовано. Для текущего учебного проекта это разумный tradeoff: базовая надежность есть, сложность не раздута.

## 31. Как устроены push-уведомления?

Backend создает запись в `notifications`, затем при включенном push вызывает `INotificationSender.sendPush(userId, title, message, notificationId)`. `FcmNotificationSender` находит активные device tokens пользователя и отправляет FCM HTTP v1 message.

Для Android текущая реализация отправляет high-priority data message с `title`, `body`, `notificationId`. `ProductInventoryFirebaseMessagingService` получает message даже когда приложение свернуто, показывает локальное уведомление и отмечает backend notification id как shown.

Для iOS payload сохраняет notification/data подход, потому что доставка push зависит от APNs/FCM платформенных правил.

## 32. Почему Android push сделан как data message, а не notification payload?

Data message дает приложению контроль над локальным уведомлением и дедупликацией. Если использовать только notification payload, Android/Firebase может показать уведомление сам, а приложение не узнает backend `notificationId` в нужный момент. Тогда foreground polling мог бы показать дубль.

С data message service сам вызывает `showProductInventoryNotification`, передает backend `notificationId` и помечает его как shown.

## 33. Как предотвращаются дубли уведомлений?

Есть две линии защиты.

На backend для reminders используется `dedupeKey` и уникальный индекс `(user_id, dedupe_key)`. Это предотвращает повторное создание одного и того же reminder.

На Android для доставки используется backend `notificationId`. Если FCM уже показал уведомление, `AndroidNotificationPresenter` сохраняет id в SharedPreferences. Foreground polling получает unread notifications, но пропускает уже shown IDs. Так FCM и polling fallback не создают дубль.

## 34. Почему product-created notification отправляется actor user?

Потому что один и тот же аккаунт может быть открыт на нескольких устройствах. Если backend исключит actor user целиком, второе устройство того же пользователя не получит push. Текущая реализация включает actor user, чтобы same-account multi-device работал.

Минус: инициирующее устройство тоже может получить self-notification. Более точное будущее решение - передавать device id/token в mutating request и исключать только инициирующее устройство.

## 35. Что происходит с устаревшими FCM токенами?

`FcmNotificationSender` при HTTP client error 400 или 404 деактивирует token через repository. Это важно, потому что мобильные устройства переустанавливают приложение, Firebase token меняется, старые tokens становятся недействительными. Без деактивации backend продолжал бы пытаться отправлять push на мусорные tokens.

## 36. Как пользователь управляет уведомлениями?

Есть `NotificationSettings`:

- expiration reminders enabled;
- low stock reminders enabled;
- push enabled;
- expiration reminder days.

Клиент получает и обновляет настройки через `/api/v1/notifications/preferences`. Device token регистрируется через `/device-tokens`. Reminder service применяет настройки перед созданием notification и перед push.

## 37. Почему хранить in-app notifications, если есть push?

Push - канал доставки, а не надежное хранилище. Пользователь мог запретить уведомления, устройство могло быть offline, Android мог не доставить push после force-stop, FCM token мог устареть. In-app notification table сохраняет историю и позволяет показать события при открытии приложения. Это также дает polling fallback на Android.

## 38. Как устроена Android notification bootstrap логика?

`AndroidNotificationBootstrap` монтируется в `App()`, а не на экране уведомлений. Это важно: регистрация token и прием foreground fallback не должны зависеть от того, открыл ли пользователь экран уведомлений.

Bootstrap:

- при authenticated state регистрирует текущий FCM token;
- если push включен, а Android 13+ permission не выдан, запрашивает `POST_NOTIFICATIONS`;
- запускает polling unread notifications только когда lifecycle `RESUMED`;
- показывает unread notifications через `AndroidNotificationPresenter`.

Polling специально не работает в background. Background delivery должен обеспечивать FCM.

## 39. Как устроена база данных?

База реляционная. Основные таблицы:

- `users` - аккаунты;
- `households` - группы учета;
- `memberships` - связь user-household-role;
- `products` - продукты и остатки;
- `categories` - системные/пользовательские категории;
- `notifications` - in-app notifications;
- `notification_settings` - настройки уведомлений;
- `notification_device_tokens` - FCM/APNs tokens;
- `invite_codes` - invite flow;
- `refresh_tokens` - серверно контролируемые refresh tokens;
- `barcode_product_cache` - cache barcode draft;
- `recipe_documents` - knowledge base для рецептов.

Flyway гарантирует, что схема эволюционирует воспроизводимо, а не зависит от случайного `ddl-auto`.

## 40. Почему выбран PostgreSQL?

Предметная область реляционная: пользователи состоят в домохозяйствах, продукты принадлежат household, уведомления связаны с user/product/household, refresh tokens должны быть транзакционно проверяемы, dedupe reminders требует уникального индекса. PostgreSQL хорошо подходит для таких связей, индексов, транзакций и миграций. Документная база была бы менее удобна для membership checks и уникальных ограничений.

## 41. Как работает Room на Android?

Room реализует локальные data sources для shared contracts. `AppDatabase` содержит продукты, домохозяйства, barcode cache и pending sync actions. DAO выполняют queries, adapters маппят Room entities в shared domain/local models. `RoomSyncQueue` хранит действия offline queue.

Room выбран вместо самописного JSON-хранилища на Android, потому что нужны индексы, миграции, типизированные DAO и надежное локальное состояние.

## 42. Почему tokens хранятся в DataStore/NSUserDefaults?

В shared есть абстракция `TokenStorage`. Android реализация использует DataStore, потому что это современное асинхронное key-value хранилище. iOS реализация использует NSUserDefaults как простую платформенную реализацию. JVM тесты используют in-memory storage.

Главная идея: shared Ktor client не знает, где физически лежат tokens. Он зависит от интерфейса.

## 43. Как iOS использует shared ViewModels?

`DIContainer` получает ViewModels из Koin через `KoinHelper`. `SharedVMHolder` подписывается на `viewState` и `viewAction` через `FlowWatchUtils.bind`, публикует state в SwiftUI через `@Published`, а events отправляет обратно в shared ViewModel. Таким образом SwiftUI экран рисует нативный UI, но state machine и бизнес-логика остаются общими.

## 44. Как обрабатываются ошибки API?

Backend имеет `GlobalExceptionHandler`, который превращает domain/security/validation ошибки в structured `ErrorResponse`. Mobile Ktor client имеет `HttpResponseValidator`: для status code >= 400 он пытается распарсить backend error message и бросает `ApiException(statusCode, message)`. ViewModels переводят ошибки в `UiState.Error` или оставляют silent failure там, где это UX-решение текущей реализации.

## 45. Как валидация защищает backend?

Request DTO используют Bean Validation: обязательные поля, email формат, границы значений. Но важные domain-инварианты не полагаются только на DTO. Application services дополнительно проверяют membership, positive consume amount, not exceeding remaining amount, невозможность редактировать system categories, валидность invite code и refresh token.

Такой подход правильный: DTO validation защищает API boundary, domain/application validation защищает бизнес-правила.

## 46. Как проект тестируется?

Тестирование многоуровневое:

- backend unit tests для services и domain services;
- backend REST/controller tests;
- backend migration/schema tests;
- FCM sender tests;
- shared common tests для repositories, mappers, ViewModels, realtime source;
- Android unit tests для Room migrations, notification bootstrap/presenter и screen smoke;
- iOS UI tests для shell сценариев.

Недавние проверки предыдущего engineering-goal: `:server:test`, `:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug` проходили. Для текущей документационной задачи код не менялся, поэтому достаточно markdown/basic checks.

## 47. Как доказано, что Android background push работает?

В runtime-проверке Android-приложение было свернуто, backend был запущен с Firebase credentials, API добавил продукт `FCM data background QA 1779029381`, backend log подтвердил отправку FCM push на token `ca27a58b-b3de-490a-8465-60ff4ad0e0a9`, уведомление появилось в notification shade без открытия приложения. После открытия приложения polling fallback не создал дубль. Это проверяет именно background/minimized сценарий, а не только foreground polling.

## 48. Какие есть ограничения в текущей реализации?

Главные ограничения:

- backend исключает/включает уведомления на уровне пользователя, а не конкретного устройства;
- same-account multi-device работает, но initiating device может получить self-notification;
- offline sync не реализует сложное conflict resolution;
- Android polling fallback работает только в foreground/resumed и не заменяет FCM;
- force-stopped Android app может не получать FCM до ручного запуска, это ограничение платформы;
- iOS push зависит от provisioning и APNs capabilities;
- AI/внешние barcode sources могут быть недоступны, поэтому есть fallback;
- `server/bin` выглядит как legacy/generated tracked mirror и требует будущей ревизии.

## 49. Что бы вы улучшили следующим этапом?

Я бы сделал следующие улучшения:

- добавить device identity в mutating requests и исключать только initiating device из push;
- добавить app-specific monochrome notification icon вместо системной;
- расширить конфликтную модель offline sync: version field, server timestamps, optimistic locking или user-facing conflict resolution;
- добавить observability: structured logs, metrics по FCM delivery, AI fallback rate, SSE reconnect rate;
- добавить rate limiting auth endpoints;
- добавить refresh token family/reuse detection;
- добавить E2E тесты для full household multi-device flow;
- формально удалить или перенести tracked `server/bin` после согласования;
- улучшить production deployment docs и secret rotation procedure.

## 50. В чем главная инженерная ценность проекта?

Главная ценность в том, что проект демонстрирует не один экран и не один CRUD, а полный production-like контур:

- backend с чистыми границами, безопасностью, миграциями и транзакциями;
- KMP shared layer с реальной общей бизнес-логикой;
- Android и iOS native shells;
- offline queue;
- realtime events;
- push delivery;
- AI/barcode integrations с fallback;
- тестируемость на нескольких уровнях.

Это показывает способность проектировать систему целиком: от базы данных и security до mobile UX, фоновых уведомлений и деградации внешних сервисов.
