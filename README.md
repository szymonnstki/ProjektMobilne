# Monitor Środowiskowy - Projekt Mobilny

Aplikacja mobilna na platformę Android służąca do akwizycji danych środowiskowych. Pozwala użytkownikowi na pobieranie lokalizacji, mierzenie poziomu hałasu oraz dokumentację wizualną (zdjęcia), a następnie zapisywanie tych danych w historii.

Projekt zrealizowany w ramach przedmiotu Programowanie urządzeń mobilnych.

## [cite_start]Wykorzystane sensory i funkcje [cite: 86]
Aplikacja korzysta z następujących uprawnień i sensorów:
1.  **GPS (Location Services):** Do pobierania szerokości i długości geograficznej.
2.  **Mikrofon (MediaRecorder):** Do pomiaru amplitudy dźwięku i obliczania poziomu hałasu (dB).
3.  **Aparat (Camera Intent):** Do wykonywania zdjęć dokumentujących pomiar.
4.  **Pamięć urządzenia:** Do zapisu historii pomiarów w formacie JSON (trwałość danych).

## [cite_start]Zrzuty ekranu [cite: 88]

### Ekran Główny
<img width="189" height="395" alt="image" src="https://github.com/user-attachments/assets/28425fb3-ad67-4ec7-9f23-936abdcfb6c7" />

### Historia Pomiarów
<img width="186" height="397" alt="image" src="https://github.com/user-attachments/assets/0cd6578c-83ef-4d3f-9dd8-c59a460d195b" />


## [cite_start]Instrukcja uruchomienia [cite: 90]
1. Pobierz plik `.apk` z sekcji Releases lub sklonuj repozytorium.
2. Uruchom aplikację na telefonie z systemem Android (min. SDK 24).
3. Zaakceptuj wymagane uprawnienia (Lokalizacja, Mikrofon, Aparat).
4. Upewnij się, że włączona jest lokalizacja (GPS).

## Autor
Szymon Stawarz
