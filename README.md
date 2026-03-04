# Aviasales Backend

## MongoDB integration

### Run MongoDB locally
```bash
docker compose -f docker-compose.mongo.yml up -d
```

Spring will use:
```properties
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/aviasales}
spring.data.mongodb.auto-index-creation=${APP_MONGO_AUTO_INDEX_CREATION:false}
```

You can override URI:
```bash
export MONGODB_URI=mongodb://localhost:27017/aviasales
```

Enable Mongo index auto-creation only when MongoDB is available:
```bash
export APP_MONGO_AUTO_INDEX_CREATION=true
```

## Existing utility commands

Connection to Postgres:
```bash
docker exec -it postgres-app bash
```

Отправка файлов на helios:
```bash
scp -P 2222 aviasales-0.0.1-SNAPSHOT.jar s408522@helios.se.ifmo.ru:./
```

Пробрасывание портов на helios:
```bash
ssh -p 2222 -L 8080:localhost:8080 s408522@helios.se.ifmo.ru
```
. 
