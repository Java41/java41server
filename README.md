
```Java
Unirest.setTimeouts(0, 0);
HttpResponse<String> response = Unirest.post("https://faruegonar.beget.app/auth/register")
  .header("Content-Type", "application/json")
  .body("{\"email\":\"test@quarkus.io\",\"password\":\"password123\",\"birthdate\": \"2001-07-13\"}")
  .asString();
```
# Ожидаемый ответ
```json
{
    "message": "Пользователь успешно зарегистрирован"
}
```

```Java
Unirest.setTimeouts(0, 0);
HttpResponse<String> response = Unirest.post("https://faruegonar.beget.app/auth/login")
  .header("Content-Type", "application/json")
  .body("{\"email\":\"test@quarkus.io\",\"password\":\"password123\"}")
  .asString();
```
# Ожидаемый ответ
```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsInVwbiI6InRlc3RAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJVc2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE3NDczNzUxNTEsImV4cCI6MTc0NzM3NjA1MSwianRpIjoiZDZiYjFlYjEtOTk0NC00MjAxLWEyMTQtNWE3MjhiOWI3ODU4In0.AIKoBmpXMjfroOZ_2awL12McBwVa9BNcqzKB3NRs-0QbcOsUBMu0kkpWZ-A-l8XRyj6AQNC3Bj9kbzrUArsbk2nvW3BnlV9uBH4HbcDmqGPtnAx9n0taIbUsUXcBqWmgUFLixKG7tpsAwiBz-p-5zYlp47pPXtMGz77FR7ZGRQd68b73lgOmNXFZfwYEmLBoJs3kBXv9tR10BMTzLlWayxh3BRZX-eRxaI8Q-HH4zxK0suShElNpCjSTysMqaCaEAcH5HREAmauIl4pIBvbALth5EJTR9jev_QPbz8pVpEna3N-Czwf8QnYqHwWaEIkB0hCI9Pn6TmvddRwMTwf3og",
  "refreshToken": "99faaa6b-609e-4178-bebc-586bd72bdb7f"
}
```

```Java
Unirest.setTimeouts(0, 0);
HttpResponse<String> response = Unirest.post("https://faruegonar.beget.app/auth/refresh")
  .header("Content-Type", "application/json")
  .body("{\"refreshToken\":\"fff4196b-a7fa-4af1-ad69-37b0e5bc2bf7\"}")
  .asString();
```

# Ожидаемый ответ
```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsInVwbiI6InRlc3RAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJVc2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE3NDczNzUxNTEsImV4cCI6MTc0NzM3NjA1MSwianRpIjoiZDZiYjFlYjEtOTk0NC00MjAxLWEyMTQtNWE3MjhiOWI3ODU4In0.AIKoBmpXMjfroOZ_2awL12McBwVa9BNcqzKB3NRs-0QbcOsUBMu0kkpWZ-A-l8XRyj6AQNC3Bj9kbzrUArsbk2nvW3BnlV9uBH4HbcDmqGPtnAx9n0taIbUsUXcBqWmgUFLixKG7tpsAwiBz-p-5zYlp47pPXtMGz77FR7ZGRQd68b73lgOmNXFZfwYEmLBoJs3kBXv9tR10BMTzLlWayxh3BRZX-eRxaI8Q-HH4zxK0suShElNpCjSTysMqaCaEAcH5HREAmauIl4pIBvbALth5EJTR9jev_QPbz8pVpEna3N-Czwf8QnYqHwWaEIkB0hCI9Pn6TmvddRwMTwf3og",
  "refreshToken": "99faaa6b-609e-4178-bebc-586bd72bdb7f"
}
```
```Java
Unirest.setTimeouts(0, 0);
HttpResponse<String> response = Unirest.get("https://faruegonar.beget.app/secured/roles-allowed")
  .header("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsInVwbiI6InRlc3RAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJVc2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE3NDczNzQwOTUsImV4cCI6MTc0NzM3NDk5NSwianRpIjoiMWI1M2ZmOGItNjRhNi00ZGU1LWI2YzAtZDUwYjUxMmFmZDVhIn0.YM5pdTjHA6T6bpiLF0xDC_YdrJLwmso5DVA-Otyvga7VlUUz8Tbb9G8Fg4ljiQG4X92iutof5A2K6GjABYppDKBNppaP0m6tFxc0uDTmFfuWsjDBip5mGHIOU0UTDCg08qxGZ-NSes37bM9fqGkPB2Vtq6HScXkHa4U6FkUkd0DR6zXkVZ3L9toSbrOVCye1miUQKCgaPdhWz3t-axADBFlc0oqZJBqZkrqkos17omAjmaktPXlvnxtThgk9G1FXXZ3dcuyk2Doj4KpDihDcEnH0TRKOABovTKTSxl587QDa7KlQTXX8QTIk7H2gVTI43YIrGnDJG62ena_gRb7i2w")
  .asString();

```
# Ожидаемый ответ
```text
hello + test@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 2001-07-13
```
```java
Unirest.setTimeouts(0, 0);
HttpResponse<String> response = Unirest.get("https://faruegonar.beget.app/secured/roles-allowed-admin")
        .header("Authorization", " Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsInVwbiI6InRlc3RAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJVc2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE3NDczNzU2OTQsImV4cCI6MTc0NzM3NjU5NCwianRpIjoiNjAyZTIxMmQtMTZkYS00YjM1LWFlZmMtZGVkMDdmMzZiZmE5In0.RVKLhDUlhjrFEsuPxhRSBjjViBwpxuPk4YYZPVrM0UQnRjo65WVBffGEatNcxc0I4guGGZblEM3P-3wT77gFG4D2uhSz5fw8qB3RIcPtQPIGzUYiAtF9g6V7paTGGpx33koHhdjqB9xmPGPJ_22gvkk6vSABswgKpvuwN9geDuud5FdJHLVsTEmUwUzFURW4eR6_R29I4-_-Gz1ypdMf8fa5vMpo5lLYXVnMr5E6JRZt_XFQ6v9Mzgr0mC28Lz6ZmDsOAUNI2UzUHADHXzijlnIYHhPw4OzamSRFzhQ_48okC9bZczvwdr-UuOBwtx8WL_5_1BdSQ45No80gUbyHWw")
        .multiPartContent()
        .asString();
```
# Ожидаемый ответ
```text
тут ответ будет 403 так как этот эндпоинт ожидает роль Admin
```

Этот токен 
```text
"accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsInVwbiI6InRlc3RAcXVhcmt1cy5pbyIsImdyb3VwcyI6WyJVc2VyIl0sImJpcnRoZGF0ZSI6IjIwMDEtMDctMTMiLCJpYXQiOjE3NDczNzUxNTEsImV4cCI6MTc0NzM3NjA1MSwianRpIjoiZDZiYjFlYjEtOTk0NC00MjAxLWEyMTQtNWE3MjhiOWI3ODU4In0.AIKoBmpXMjfroOZ_2awL12McBwVa9BNcqzKB3NRs-0QbcOsUBMu0kkpWZ-A-l8XRyj6AQNC3Bj9kbzrUArsbk2nvW3BnlV9uBH4HbcDmqGPtnAx9n0taIbUsUXcBqWmgUFLixKG7tpsAwiBz-p-5zYlp47pPXtMGz77FR7ZGRQd68b73lgOmNXFZfwYEmLBoJs3kBXv9tR10BMTzLlWayxh3BRZX-eRxaI8Q-HH4zxK0suShElNpCjSTysMqaCaEAcH5HREAmauIl4pIBvbALth5EJTR9jev_QPbz8pVpEna3N-Czwf8QnYqHwWaEIkB0hCI9Pn6TmvddRwMTwf3og"
```
можно рашифровать тут
https://jwt.io/

```

# java41server

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/java41server-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
