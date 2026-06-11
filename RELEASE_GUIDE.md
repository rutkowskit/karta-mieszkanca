# Instrukcja Budowania Wersji Produkcyjnej (Signed Release Build Guide)

Niniejsza instrukcja opisuje krok po kroku, jak wygenerować klucz podpisujący aplikację (Keystore) na lokalnym komputerze oraz jak skonstruować i zabezpieczyć proces budowania produkcyjnej wersji instalatora **APK** lub paczki dystrybucyjnej **AAB** (Android App Bundle) dla Google Play.

---

## Krok 1: Generowanie Lokalnego Klucza Podpisującego (Keystore)

Aby podpisać aplikację wersją produkcyjną, potrzebujesz pliku keystore (`.jks` lub `.keystore`). Możesz go wygenerować za pomocą narzędzia `keytool` dostarczanego wraz z JDK (Java Development Kit) lub bezpośrednio przez Android Studio.

### Metoda A: Konsola (Linia poleceń)
Uruchom poniższe polecenie w terminalu (podmieniając według uznania):

```bash
keytool -genkey -v -keystore gorzow_resident_card_release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias gorzow-signing-key
```

* **`gorzow_resident_card_release.jks`**: Nazwa wyjściowego pliku klucza. Zapamiętaj, gdzie go zapisujesz!
* **`gorzow-signing-key`**: Unikalny alias klucza wewnątrz pliku jks.
* Podczas generowania zostaniesz poproszony(-a) o zdefiniowanie silnego hasła dla klucza oraz podanie podstawowych danych właściciela.

### Metoda B: Android Studio GUI
1. W górnym menu Android Studio wybierz **Build** -> **Generate Signed Bundle / APK...**.
2. Wybierz format docelowy (np. **Android App Bundle** dla sklepu lub **APK** do szybkiej instalacji lokalnej) i kliknij **Next**.
3. Pod napisem *Key store path* kliknij **Create new...**.
4. Wypełnij formularz (ścieżka zapisu, hasła, alias klucza, dane organizacyjne) i kliknij **OK**.

---

## Krok 2: Konfiguracja Bezpieczeństwa w Projekcie (build.gradle.kts)

> [!WARNING]
> **NIGDY** nie wpisuj haseł do klucza produkcyjnego bezpośrednio w kodzie źródłowym ani w pliku `build.gradle.kts`. Dane te powinny być pobierane z nieśledzonych plików konfiguracyjnych (np. zmiennych środowiskowych systemu, pliku `.env` lub pliku `local.properties` znajdującego się całkowicie poza repozytorium Gita).

### 1. Przykład zabezpieczenia parametrów podpisywania w lokalnym środowisku
Stwórz plik o nazwie `local.properties` (lub dopisz do istniejącego pliku w głównym katalogu projektu) następujące wpisy:

```properties
# Plik: /local.properties (Upewnij się, że jest on dodany do .gitignore!)
RELEASE_STORE_FILE=/pelna/sciezka/do/pliku/gorzow_resident_card_release.jks
RELEASE_STORE_PASSWORD=MojeSuperTrudneHasloDlaStore123
RELEASE_KEY_ALIAS=gorzow-signing-key
RELEASE_KEY_PASSWORD=MojeSuperTrudneHasloDlaAlias123
```

### 2. Dostosowanie pliku `app/build.gradle.kts`
Zmodyfikuj konfigurację podpisywania w module aplikacji, wczytując parametry reaktywnie z obu potencjalnych źródeł (zmiennych środowiskowych systemowych lub pliku `local.properties`):

```kotlin
android {
    // ... nagłówek i podstawowe konfiguracje ...

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = java.util.Properties()
    if (keystorePropertiesFile.exists()) {
        java.io.FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
    }

    signingConfigs {
        create("release") {
            val keyStorePath = System.getenv("RELEASE_STORE_FILE")
                ?: (keystoreProperties["RELEASE_STORE_FILE"] as? String)
                ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keyStorePath)
            
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                ?: (keystoreProperties["RELEASE_STORE_PASSWORD"] as? String)

            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                ?: (keystoreProperties["RELEASE_KEY_ALIAS"] as? String)
                ?: "upload"

            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                ?: (keystoreProperties["RELEASE_KEY_PASSWORD"] as? String)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Włącza optymalizację i zaciemnianie kodu (ProGuard / R8)
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## Krok 3: Budowanie Paczki Produkcyjnej (Gradle CLI)

Otwórz okno terminala w głównym katalogu projektu i wykonaj odpowiednie polecenie kompilacji.

### Budowanie Instalatora APK (Szybka dystrybucja ręczna / testowa)
Aby zbudować i podpisać plik produkcyjny `.apk`:
```bash
gradle :app:assembleRelease
```
Wyjściowy, gotowy plik produkcyjny zostanie zapisany w lokalizacji:
📂 `app/build/outputs/apk/release/app-release.png` (lub ze zmienioną nazwą na `.apk`).

### Budowanie Paczki AAB (Gotowe wydanie do sklepu Google Play)
Nowoczesne wydania dystrybuowane w markecie wymagają wersji `.aab`:
```bash
gradle :app:bundleRelease
```
Wyjściowy, spakowany plik produkcyjny zostanie zapisany w lokalizacji:
📂 `app/build/outputs/bundle/release/app-release.aab`

---

## Krok 4: Weryfikacja Poprawności Podpisu

Możesz upewnić się, że plik APK został pomyślnie podpisany Twoim dedykowanym certyfikatem produkcyjnym za pomocą narzędzia `apksigner` znajdującego się w folderze narzędziowym Android SDK:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
W konsoli powinieneś zobaczyć odcisk palca SHA-256 wygenerowanego na pierwszym kroku klucza, potwierdzający integralność kodu źródłowego. Nowy podpis gwarantuje pełną odporność na próby modyfikacji lub złośliwego przejęcia aplikacji.
