# Zatrzymanie Percepcji Czasu (Fabric 1.21.4, mod kliencki)

## Budowanie .jar
1. Java 21.
2. Najłatwiej: wygeneruj pusty projekt na https://fabricmc.net/develop (Minecraft 1.21.4, Mojang mappings, Java 21)
   i skopiuj z niego `gradlew`, `gradlew.bat`, folder `gradle/` oraz wersje z `gradle.properties`.
3. Wrzuć `src/`, `build.gradle`, `settings.gradle` i `gradle.properties` z tej paczki
   (jeśli wersje loader/fabric_version się nie zgadzają, wstaw te z generatora).
4. `./gradlew build` -> `build/libs/timefreeze-1.0.0.jar` (plik bez "-sources").
5. Wrzuć jar do folderu `mods` razem z Fabric Loader 0.16.x i Fabric API dla 1.21.4.

## Sterowanie (zmienne w Opcje -> Sterowanie)
- K - GUI moda (suwak jasności, przełączniki, "Zapisz pozycję i stan")
- J - włącz/wyłącz zamrożenie obrazu

## Struktura
src/main/java/com/devcode/timefreeze/
  TimeFreezeClient.java        - entrypoint klienta
  Config.java                  - config JSON (config/timefreeze.json)
  client/FreezeHandler.java    - zamrożenie obrazu, blokada ruchu/kamery, klawisze
  client/TimeFreezeScreen.java - GUI (suwak + przyciski)
  client/KeyBindings.java      - rejestracja skrótów
  client/GammaHelper.java      - gamma powyżej 1.0

## Najprostsza droga do .jar (bez instalowania czegokolwiek)
1. Załóż darmowe konto na github.com i utwórz nowe repozytorium.
2. Wrzuć do niego CAŁĄ zawartość tego folderu (z ukrytym folderem `.github`).
3. Wejdź w zakładkę Actions -> "build-jar" -> poczekaj na zielony znaczek.
4. Wejdź w ukończone uruchomienie i pobierz artefakt "timefreeze-jar" (zip z plikiem .jar w środku).
