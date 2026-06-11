# PODRĘCZNIK DEWELOPERA i AI AGENTÓW (AGENTS.md)

Witaj! Ten dokument zawiera kluczowe wytyczne techniczne, architektoniczne i operacyjne dotyczące modyfikowania i rozwijania aplikacji **Karta Mieszkańca**. Przeczytaj ten plik przed wprowadzeniem jakichkolwiek modyfikacji w kodzie.

---

## 🏗️ Architektura i Przepływ Danych

Aplikacja opiera się w 100% na architekturze **MVVM + Repository**:
1. **Model**: Encja Room `ResidentCard` wspiera opcjonalne zdjęcia (`userPhotoPath`) oraz zrzuty oryginalnych kart (`cardScreenshotPath`) przechowywane jako lokalne ścieżki absolutne w piaskownicy aplikacji (`/data/user/0/.../files/`).
2. **Dao**: Interfejs `ResidentCardDao` zawiera transakcje zabezpieczające spójność flagi `isPrimary`. Wywołanie metody zapisu automatycznie resetuje flagę `isPrimary` na wszystkich pozostałych rekordach dekretem technologicznym:
   ```kotlin
   @Query("UPDATE resident_cards SET isPrimary = 0 WHERE id != :cardId")
   suspend fun clearOtherPrimaries(cardId: Int)
   ```
3. **Repository**: Repozytorium pośredniczy w dostępie, eksponując strumienie `Flow<List<ResidentCard>>` oraz `Flow<ResidentCard?>`.
4. **ViewModel**: Odpowiada za logiczne kopiowanie strumieni wejściowych URI obrazów do pamięci lokalnej przed zapisaniem ich w bazach danych.

---

## 🚨 Krytyczne Zasady Implementacji (MANDATES)

### 1. Bezpieczeństwo Zdjęć i Plików Pickerów
* **NIGDY** nie zapisuj surowego URI zwróconego przez `ActivityResultContracts.PickVisualMedia` bezpośrednio do bazy danych SQLite! Te URI po jakimś czasie tracą uprawnienia odczytu (np. po restarcie systemu Android).
* **ZAWSZE** używaj zaimplementowanej metody w ViewModelu:
  ```kotlin
  fun copyImageToLocalStorage(context: Context, uri: Uri, prefix: String): String?
  ```
  Kopiuje ona plik binarnie do katalogu prywatnego aplikacji (`context.filesDir`) i zwraca ścieżkę absolutną (`File.absolutePath`), która nigdy nie traci uprawnień.

### 2. Automatyczne Wstrzykiwanie 100% Jasności (Screen Brightness)
* Podczas wyświetlania kodów QR/paskowych ekran musi świecić z mocą 100%.
* **ZAWSZE** realizuj to za pomocą reaktywnego elementu `DisposableEffect` powiązanego z flagą aktywności kodów:
  ```kotlin
  DisposableEffect(isCodeTabActive) {
      if (isCodeTabActive) {
          val window = (context as? Activity)?.window
          val layoutParams = window?.attributes
          val original = layoutParams?.screenBrightness ?: -1f
          layoutParams?.screenBrightness = 1.0f // 100%
          window?.attributes = layoutParams
          onDispose {
              layoutParams?.screenBrightness = original
              window?.attributes = layoutParams
          }
      } else { onDispose {} }
  }
  ```
* Pod żadnym pozorem nie zmieniaj jasności globalnie na stałe! Musi ona natychmiast wracać do normy, gdy użytkownik przechodzi do widoku oryginalnego zrzutu ekranu lub formularza edycji.

### 3. Wymuszona Orientacja Pionowa (Portrait Mode)
* Karta musi być wyświetlana wyłącznie pionowo. Zabezpiecza to wpis w `AndroidManifest.xml` z ignorowaniem ostrzeżeń narzędzi deweloperskich:
  ```xml
  android:screenOrientation="portrait"
  tools:ignore="LockedOrientationActivity"
  ```
* Nigdy nie usuwaj tej konfiguracji.

### 4. Generowanie Kodów kreskowych i QR (ZXing) oraz ochrona przed Force Dark
* Kod QR oraz kod kreskowy (Code 128) generowane są jako czarno-białe obiekty bitmapowe za pomocą biblioteki `MultiFormatWriter`.
* **ZAWSZE** wyświetlaj je w białej karcie o pełnym kontraście (`containerColor = Color.White`), zapewniając prawidłową detekcję skanerów laserowych/optycznych niezależnie od ciemnego motywu systemowego.
* **KRYTYCZNE (Znikające teksty/Inwersja kolorów)**: Niektóre nakładki systemowe (np. Xiaomi MIUI/HyperOS "Force Dark Mode", Realme UI, ColorOS) siłowo odwracają kolory jasnych kontenerów w aplikacjach działających w trybie ciemnym. Powoduje to, że czarny tekst pod kodami oraza same kody na białym tle stają się niewidoczne lub nieczytelne dla fizycznych skanerów. Aby temu zapobiec, w głównym oknie aplikacji (`MainActivity`) zablokowane jest automatyczne wymuszanie ciemnego motywu dla widoków:
  ```kotlin
  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      window.decorView.isForceDarkAllowed = false
  }
  ```
  Zabezpieczenie to gwarantuje zachowanie idealnego kontrastu (czarne kody/teksty na białym tle) bez względu na agresywne globalne tryby ułatwień dostępu lub nakładki vendorów.

---

## 🧩 Rozszerzanie Funkcjonalności (Jak dodać nową cechę?)

### Dodawanie nowego pola w karcie (np. PESEL lub Grupa krwi):
1. Zmodyfikuj klasę `ResidentCard` w `/app/src/main/java/com/example/data/ResidentCard.kt` dodając nowe pole (np. `val pesel: String? = null`).
2. Jeśli zmieniasz strukturę tabeli, pamiętaj o zwiększeniu `version` bazy danych w `AppDatabase.kt` lub skorzystaniu ze strategii `.fallbackToDestructiveMigration()` podczas prototypowania.
3. Dodaj odpowiedni komponent `OutlinedTextField` w overlay'u formularza `CardFormOverlay` w `MainActivity.kt`.
4. Dostosuj makietę wyświetlania `CardReplica`, aby zgrabnie wyrenderować nowe dane z zachowaniem Material Design 3.
