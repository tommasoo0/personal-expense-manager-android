# Gestore Spese Android

Applicazione Android sviluppata interamente da me per gestire entrate, uscite, conti, categorie e andamento delle spese personali.

Il progetto e nato per una mia esigenza reale: tracciare le spese quotidiane usando un mese finanziario personalizzato, piu vicino al ciclo effettivo dello stipendio.

---

## :pushpin: Project Overview

L'app permette di registrare movimenti personali, organizzarli per categoria e conto, filtrare rapidamente i dati e visualizzare una dashboard con riepiloghi e grafici.

La particolarita principale e il **mese finanziario personalizzato**: invece di partire dal giorno 1, il mese parte dal giorno 10. In questo modo entrate, uscite e grafici seguono meglio il periodo reale di spesa.

---

## :hammer_and_wrench: Tech Stack

- **Kotlin**: linguaggio principale.
- **Jetpack Compose**: UI dichiarativa.
- **Material 3**: componenti e stile.
- **Room Database**: persistenza locale strutturata.
- **SQLite**: database interno.
- **Coroutines / Flow**: dati reattivi dal database.
- **Gradle**: build system.
- **Android Studio**: ambiente di sviluppo.

---

## :sparkles: Key Features

- Inserimento, modifica ed eliminazione transazioni.
- Gestione entrate e uscite.
- Categorie personalizzabili.
- Conti personalizzabili.
- Ricerca per descrizione, categoria, conto, importo o data.
- Filtri per mese, anno e conto.
- Dashboard riepilogativa.
- Grafici per categorie e andamento entrate/uscite.
- Salvataggio locale su dispositivo.
- Mese finanziario personalizzato con partenza dal giorno 10.

---

## :open_file_folder: Repository Structure

```text
GestoreSpeseAndroid/
|-- app/
|   |-- src/main/java/com/expenses/mobile/
|   |   |-- data/        # Entity, DAO e database Room
|   |   |-- ui/          # UI Compose e tema
|   |   `-- MainActivity.kt
|   `-- build.gradle.kts
|-- gradle/              # Gradle wrapper
|-- build.gradle.kts
|-- settings.gradle.kts
`-- README.md
```

---

## :rocket: Installation and Usage

### 1. Clone the repository

```bash
git clone https://github.com/tommasoo0/gestore-spese-android.git
cd gestore-spese-android
```

### 2. Open in Android Studio

Open the project folder with Android Studio and wait for Gradle sync.

### 3. Run the app

Launch the app on an Android emulator or physical device.

On Windows, you can also build from terminal with:

```bash
gradlew.bat assembleDebug
```

---

## :bar_chart: Project Status

Personal project used by me.
