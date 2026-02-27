# DriveXpand Backend

Das Backend für DriveXpand verarbeitet Fahrzeugtelemetriedaten, die über einen ESP32-basierten OBD2-Dongle ausgelesen werden. Es aggregiert die Rohdaten zu logischen Fahrten (Trips) und stellt eine REST-API für das dazugehörige React-Frontend zur Verfügung.

## Tech Stack
* **Sprache:** Java 21
* **Framework:** Spring Boot 4
* **Sicherheit:** Spring Security
* **Persistenz:** Spring Data JPA / Hibernate, PostgreSQL 15 (Prod), H2 (Dev)
* **Mapping:** MapStruct
* **Infrastruktur:** Docker & Docker Compose
* **API-Dokumentation:** OpenAPI 3.1.0 (Swagger)

## Features
* **REST-API:** Bereitstellung von Endpunkten für Telemetrie-Ingest und Datenabruf.
* **Trip Aggregation:** Fortlaufende Aufsummierung von Fahrtmetriken (wie Distanz und Messpunkten) direkt beim Datenbank-Schreibvorgang zur Optimierung der Lesezugriffe.
* **Fahrzeugverwaltung:** Endpunkte zur Anpassung von Gerätenamen, Abruf von Fahrten- und Auslastungsstatistiken sowie Speicherung von Fahrzeugbildern (als `BYTEA`).
* **Digitales Wartungsbuch:** CRUD-Operationen für fahrzeugspezifische Reparatur- und Wartungsnotizen inkl. Kosten- und Datumstracking.
* **Dual-Authentication:** API-Key für die M2M-Kommunikation (Edge-Device) und zustandslose JWT-Authentifizierung für das Web-Frontend.
