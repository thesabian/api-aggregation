# Technical test for FedEx

Daniele Sabia

## API Aggregation

#### IDE

IntelliJ CE

Java 17

#### Project Setup

build the project
```
mvn install
```

docker compose up
```
docker-compose -f compose.yml up -d
```

30+ seconds
```

curl -X GET "http://127.0.0.1:8080/actuator"

curl -X GET "http://127.0.0.1:8080/aggregation?shipmentsOrderNumbers=987654321,123456789&trackOrderNumbers=987654321,123456789&pricingCountryCodes=NL,CN"

```

#### Useful curls

```

curl "http://127.0.0.1:4000/shipment-products?orderNumber=109347263"

curl "http://127.0.0.1:4000/track-status?orderNumber=109347263"

curl "http://127.0.0.1:4000/pricing?countryCode=NL"

```

