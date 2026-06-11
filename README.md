# Karta Mieszkańca Gorzowa Wielkopolskiego

Mobilna, w pełni offline'owa aplikacja dla systemu Android, służąca do przechowywania i ekspresowego okazywania cyfrowej wersji **Karty Mieszkańca Gorzowa Wielkopolskiego** (`#StądJestem`). 

Aplikacja została zaprojektowana zgodnie z wytycznymi bezpiecznego, nowoczesnego programowania natywnego w języku **Kotlin** z wykorzystaniem **Jetpack Compose** oraz **Material Design 3**.

---

## 📱 Główne Funkcjonalności

1. **Cyfrowa Karta Mieszkańca**:
   - Estetyczna replika karty zawierająca imię i nazwisko właściciela, status ważności (zielony badge „Aktywna”), datę ważności oraz numer identyfikacyjny.
2. **Zaawansowane Przechowywanie Danych (SQLite & Room)**:
   - Wszystkie informacje zapisywane są bezpośrednio w lokalnej i szybkiej bazie danych SQLite za pomocą biblioteki Jetpack Room.
   - Aplikacja **nie zbiera, nie wysyła i nie przekazuje** danych przez internet – działa w 100% offline.
3. **Funkcjonalne Kody Skanowania (QR & Barcode)**:
   - Po kliknięciu w kartę, aplikacja generuje matematycznie precyzyjne kody kreskowe (standard `Code 128`) oraz kody `QR` za pomocą silnika ZXing.
   - Kody są wyświetlane w **sztywno wymuszonym trybie jasnym** (czarny kod na idealnie białym tle), co zapewnia maksymalną czytelność dla dowolnego skanera stacjonarnego lub ręcznego.
4. **Inteligentny System Kontroli Jasności Ekranu**:
   - Podczas wyświetlania kodu QR lub kodu kreskowego aplikacja **automatycznie podnosi jasność ekranu telefonu do 100%**, zapewniając czytelność kodu.
   - Po przełączeniu na inną zakładkę, przejściu do formularza edycji lub zamknięciu widoku, oryginalna jasność użytkownika jest **natychmiastowo przywracana**.
5. **Wymuszona Orientacja Pionowa (Portrait Lock)**:
   - Zgodnie z wymaganiami interfejsu karty, aplikacja ma zablokowaną możliwość obrotu do poziomu, co zapewnia stałą ergonomię użytkowania jedną ręką.
6. **Bezpieczne Załączniki Multimedialne (Zdjęcia & Zrzuty ekranu)**:
   - Użytkownik może dodać swoje opcjonalne zdjęcie profilowe oraz zrzut ekranu oryginalnej karty fizycznej (podobnie jak w Google Wallet).
   - Wybrane zdjęcia są **kopiowane bezpośrednio do prywatnego folderu aplikacji (sandbox)** w pamięci wewnętrznej urządzenia. Zapobiega to utracie dostępu do plików po restarcie telefonu, co jest częstym błędem zwykłych pickerów systemowych.
7. **Obsługa Wielu Profili (Członkowie Rodziny)**:
   - Aplikacja pozwala dodać więcej niż jedną kartę. Szybki karuzelowy selektor u góry ekranu pozwala na płynne przełączanie się np. między kartą własną, a kartą dziecka czy współmałżonka.

---

## 🛠️ Architektura Projektu

Aplikacja opiera się na architekturze **MVVM (Model-View-ViewModel)** połączonej z wzorcem **Repository Pattern**:

- **Model (`com.vrt.data`)**:
  - `ResidentCard`: Encja bazy danych przechowująca imię, nazwisko, numer, datę ważności, ścieżkę do zdjęcia oraz flagę karty głównej.
  - `ResidentCardDao`: Interfejs transakcyjny SQLite.
  - `AppDatabase`: Singleton bazy danych Room.
- **Data Source / Repozytorium**:
  - `ResidentCardRepository`: Pośredniczy w wymianie danych, gwarantując separację logiki od interfejsu.
- **ViewModel (`com.vrt.ui`)**:
  - `ResidentCardViewModel`: Reaktywnie eksponuje stany bazy za pomocą `StateFlow`. Odpowiada za operacje IO (np. bezpieczne kopiowanie bitmap zdjęć do pamięci wewnętrznej aplikacji).
- **Widok (`com.vrt`)**:
  - `MainActivity`: Centralny, zoptymalizowany ekran Jetpack Compose, dynamicznie renderujący Dashboard, karuzelę profili, kody kreskowe QR/1D oraz formularz edycji/dodawania w nakładce modalnej (Overlay).

---

## 🎨 Design i Kolorystyka

- **Dark Mode**: Aplikacja działa wyłącznie w trybie ciemnym (głęboki grafit, czerń, wysoka czytelność w nocy).
- **Gorzowian Accent**: Akcenty i elementy nawigacyjne zostały pomalowane na charakterystyczną dla marki Gorzowa Wielkopolskiego **soczystą limonkową zieleń** (`#8CC63F`), co nadaje aplikacji tożsamość i prestiżowy wygląd znany z oficjalnych systemów miejskich.
- **Kontrast Skanowania**: Kontener kodu QR/kreskowego pozostaje zawsze na białym tle o maksymalnym kontraście, niezależnie od ciemnego motywu reszty interfejsu.
