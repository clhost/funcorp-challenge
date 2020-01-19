[![Build Status](https://travis-ci.com/clhost/funcorp-challenge.svg?token=8Qu4bPcVDqaSCkhPsMBM&branch=release/1.0.x)](https://travis-ci.com/clhost/funcorp-challenge)

# Funcorp Challenge Task
Напишите приложение на Java или/и KotlIn, которое с максимальной скоростью распознает и соберёт все мемы на 
немецком языке и вернёт их в качестве ленты контента, сопровождаемого метаданными, отсортированной по времени 
публикации от более свежих к более старым.

Термин «мемы» мы трактуем широко: это могут быть тематические картинки, комиксы, забавные видео, рисунки, 
производные от аниме и манги и т.п.

Контент в приложении должен быть уникальным, то есть нужно отфильтровать дубликаты. 
Алгоритм любой, от хэш-суммы файла до нечёткого сравнения.

Настройка источников мемов должна быть достаточно гибкой, 
чтобы добавление новых можно было обеспечить редактированием конфигурационных файлов.

Предполагается, что лента отдаётся в REST-парадигме: endpoint GET /feed, 
возвращающий постраничный список контента для предпросмотра, и endpoint GET /feed/:contentID, 
возвращающий отдельные контенты с полными метаданными.

Приложение будет работать в Docker-контейнере, а значит в корне репозитория должен лежать Dockerfile и приложение должно 
запускаться по команде: docker run --rm -it $(docker build -q .)

Приложение должно соответствовать основным принципам 12-factor:
* Писать логи в STDOUT без промежуточной буферизации
* Конфигурироваться через environment-переменные
* Не хранить состояние
* Масштабироваться горизонтально

Под последним пунктом мы понимаем конфигурацию, когда приложение развёрнуто в кластере из нескольких хостов и запросы 
между ними распределяет балансировщик. Все хосты кластера получают одинаковый набор environment-переменных. 
Все нужные вашему приложению внешние сервисы (базы данных, кэши, объектные или файловые хранилища) также должны 
задаваться конфигурационными параметрами. Если что, то наш любимый стек — Redis, MongoDB и AWS S3.

Конкурсное задание должно сопровождаться файлом README с описанием сборки и запуска, 
пример конфигурации переменных среды можно положить рядом в файле .env.example

# Architecture
## Участвующие компоненты
1. **Minio** - как объектное неупорядоченное хранилище картинок (мемов)
2. **Consul** - как система, управляющая конфигурацией источников и как service discovery для обнаружения 
   компонентами ```App``` компоненты  ```tree```
3. **Postgres** - бд, хранящая персистентно мета-информацию для поиска в Minio и множество 
   перцептивных хешей для построения метрического дерева (VP-tree)
4. **VP-tree** - дерево для объектов метрического пространства, необходимое для быстрого обнаружения дубликатов мемов
5. **App** - само приложение, агрегирующее мемы с заданных источников и выдающее их в отсортированную ленту


## В двух словах
Оба компонента используют одну и ту же схему базы данных.
В данный момент VP-Tree - это узкое место, которое не масштабируется горизонтально ввиду консистентного определения на дубликаты. 
App же горизонтально масштабируется.
По технически обстоятельствам источник данных всего 1 - vk. Чтобы добавить любой другой источник, на уровне кода достаточно
заимплементить интерфейс ```MemeLoader``` и приаттачить его в ```MemeLoaderConfiguration```, либо же вызывать метод 
```/tree/putAsync``` из любого другого приложения. 

### VP-Tree
1. Swagger spec: [Swagger](tree/api.yaml)
2. Dockerfile: [Dockerfile](tree/Dockerfile)
3. Env-example: [Env](tree/env-example.env)

### App
1. Swagger spec: [Swagger](app/api.yaml)
2. Dockerfile: [Dockerfile](app/Dockerfile)
3. Env-example: [Env](app/env-example.env)

### Метрики
VP-Tree и App имеют endpoint ```host:port/actuator/prometheus``` и стандартный набор метрик. VP-Tree вдобавок считает количество 
найденных дубликатов и размер дерева.

## Алгоритмы
1. Динамическое распределение источников между нодами приложения - на старте в Consul KV выгружается в путь 
   ```example/source``` содержимое файла ```example-sources.json```, который имеет формат:
   ```
   [
     {
       "source": "vk",          // источник мемов
       "lang": "de",            // на каком языке мемы
       "type": "group",         // где искать мемы в заданном источнике "source"
       "subSource": "germameme" // конкретный источник мемов, имеющий тип "type"
     },
     {
       /.../
     }
   ]
    ```
   Распределение источников равномерное, можно добавлять/удалять источник/ноду, т.к. на старте работы воркера 
   изменения трекаются и нода пересчитывает свои источники согласно изменениям.
   
2. Поиск дубликатов - вычисление расстояния Хемминга между двумя перцептивными хешами двух сравниваемых мемов.
   Хранение в VP-tree выборки хешей (last N) для быстрой проверки, персист - в Postgres.
   
   
## Build, configure and run
1. Consul
    ```
    docker pull consul
    docker run -d --network host --name=dev-consul consul agent -dev -node -ui
    ``` 

2. Minio
    ```
    docker pull minio
    
    # Без volume для простоты тестирования
    docker run -d -p 9000:9000 --name=minio -e "MINIO_ACCESS_KEY=your_access_key" -e "MINIO_SECRET_KEY=your_secret_key" minio/minio server /data
    
    # Используя volume
    docker run -d -p 9000:9000 --name=minio \
      -e "MINIO_ACCESS_KEY=your_access_key" \
      -e "MINIO_SECRET_KEY=your_secret_key" \
      -v /mnt/data:/data \
      minio/minio server /data
    
    # Конфигурация с помощью клиента mc
    mkdir minio-client
    cd minio-client
    
    wget https://dl.min.io/client/mc/release/linux-amd64/mc
    
    ./mc config host add minio your_endpoint your_access_key your_secret_key --api S3v4
    ./mc mb minio/memes-bucket
    ./mc policy set download minio/memes-bucket/
    ```

3. Postgres
    ```
    # Example
    cd dockerfiles/postgres
    docker build -t pgtree .
    docker run -d --network host --name=postgres pgtree
    ```

4. VP-Tree
    ```
    # Example
    docker build -t memes-tree:1.0.1 .
    docker run --network host --env-file=env-example.env memes-tree:1.0.1
    ```

5. App
    ```
    # Example
    docker build -t memes-app:1.0.1 .
    docker run --network host --env-file=env-example.env memes-app:1.0.1
    ```

## Props
### VP-Tree
|Placeholder|Description|
|---|---|
|APP_PORT|Порт приложения|
|CONSUL_HOST|Хост консула|
|CONSUL_PORT|Порт консула|
|JAVA_OPTSXmx256m|Ключи|
|---|---|
|DATASOURCE_URL|Хост постгреса|
|DATASOURCE_USERNAME|Логин к постгресу|
|DATASOURCE_PASSWORD|Пароль к постгресу|
|---|---|
|TREE_COUNT_OF_HASHES|Количество хешей в дереве, загружаемое при старте приложения|
|TREE_BIT_RESOLUTION|Размер перцептивного хеша (количество бит)|
|TREE_DUPLICATE_THRESHOLD|Пороговое значение расстояния Хемминга для выявления дубликатов|
|TREE_BUCKET_DUPLICATE_THRESHOLD|Процентное соотношение элементов-дубликатов бакета к его размеру, чтобы считать бакет дубликатом в целом|
|TREE_IS_NORMALIZED_DISTANCE|Расстояние Хемминга нормализовано или нет|
|---|---|
|S3_ENDPOINT|Хост S3|
|S3_PORT|Порт S3|
|S3_ACCESS_KEY|S3 access key|
|S3_SECRET_KEY|S3 secret key|
|S3_BUCKET_NAME|Имя бакета|
|S3_UPLOAD_WORKERS_COUNT|Количество воркеров, асинхронно сохраняющих мемы в S3|
|---|---|
|QUEUE_CAPACITY|Размер принимающей очереди (putAsync метод)|
|IMAGE_WORKERS_COUNT|Количество воркеров, скачивающих мемы и считающих перцептивные хеши|

**Note**: ```TREE_DUPLICATE_THRESHOLD``` и ```TREE_BIT_RESOLUTION``` 
должны задаваться только 1 раз при запуске, дальнейшие рестарты с другими 
параметрами без клиринга постгреса могут повлечь за собой ложные определения 
дубликатов или их пропуск.

### App
|Placeholder|Description|
|---|---|
|APP_PORT|Порт приложения|
|CONSUL_HOST|Хост консула|
|CONSUL_PORT|Порт консула|
|CONSUL_KV_SRC|Файл в формате JSON с конфигурацией источников|
|JAVA_OPTS|Ключи|
|---|---|---|
|DATASOURCE_URL|Хост постгреса|
|DATASOURCE_USERNAME|Логин к постгресу|
|DATASOURCE_PASSWORD|Пароль к постгресу|
|---|---|---|
|TREE_NAME|Имя компоненты VP-Tree, зарегистрированного в Consul|
|TREE_READ_TIMEOUT|Таймаут на чтение|
|TREE_CONNECTION_TIMEOUT|Таймаут на коннект|
|---|---|---|
|FEED_PAGE_ITEM_COUNT|Количество мем-бакетов в ленте|
|WORKER_DELAY|Период работы воркера загрузки мемов в миллисекундах|
|WORKER_INIT_DELAY|Задержка перед стартом работы воркера загрузки мемов в миллисекундах|
|---|---|---|
|VK_APP_ID|Vk app id|
|VK_CLIENT_SECRET|Vk client secret|
|VK_SERVICE_KEY|Vk secret key|
|VK_MEMES_COUNT|Количество мемов, загружаемое воркером на каждой итерации, отличной от стартовой|
|VK_STARTUP_MEMES_COUNT|Количество мемов, загружаемое воркером на старте, состоящих из пачек по VK_MEMES_COUNT штук|