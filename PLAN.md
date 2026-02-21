# Timify v2 — Voice-First Redesign Plan

## Vision
**"Measure time like breathing — effortless and continuous."**

Timify v2 transforms from a chat-based time logger into a **voice-first conversational time companion**. Users talk naturally about their day, and the app silently structures, stores, and reflects their time back to them.

---

## Current State Analysis

### Tech Stack (Keeping)
- **Android native** (Kotlin + Jetpack Compose + Material 3)
- **Room DB** (SQLite) for local storage
- **Koin** for dependency injection
- **Google Gemini AI** (gemini-1.5-flash) for NLP
- **Streamliners** base library (BaseViewModel, compose helpers)

### What Exists Today
| Feature | How it works | Problem |
|---------|-------------|---------|
| Chat | Text-based conversation with Gemini to log time | High friction — user must type everything |
| Voice | Button opens system speech dialog, sends single utterance as text | Not conversational — one-shot input |
| Pie Chart | Single day view, gets CSV from Gemini | No trends, fragile parsing |
| Insights | Gemini generates SQL (disabled) | Hardcoded date, brittle, disabled |
| Sheet Sync | Google OAuth → push/pull to Sheets | Complex setup, not core value |

### Architecture (40 Kotlin files)
```
feature/
  chat/         → ChatScreen, ChatViewModel, MessageCard, TextInput, VoiceMode
  genAI/        → GeminiModel (system prompts)
  pieChart/     → PieChartScreen, PieChartViewModel
  sheetSync/    → SheetSyncScreen, SheetSyncViewModel
  voice/        → SpeechRecognitionButton
domain/model/   → TaskInfo, ChatHistoryItem, CustomAttribute, SheetSyncState
data/local/     → Room DB, DAOs, LocalRepo
```

---

## Redesigned Architecture

### Core Principle
The app has **3 modes** aligned with **3 Jobs to Be Done**:

| Mode | JTBD | UX |
|------|------|-----|
| **Capture** | "Make logging effortless" | Voice-first conversation — user speaks naturally, app extracts tasks |
| **Reflect** | "Show me where my time goes" | Dashboard — daily/weekly/monthly views with charts and trends |
| **Improve** | "Help me do better" | AI Insights — smart coaching, patterns, comparisons |

### New Screen Flow
```
┌─────────────────────────────────────┐
│          HOME SCREEN                │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Today's Summary Card       │  │
│  │    "5h 30m tracked today"     │  │
│  │    [mini donut chart]         │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Recent Activity Timeline   │  │
│  │    ● 9:00-11:00 — Coding     │  │
│  │    ● 11:00-12:00 — Meeting   │  │
│  │    ● 12:00-1:00 — Lunch      │  │
│  └───────────────────────────────┘  │
│                                     │
│         🎤 (Large FAB)             │
│     "Tap to tell me about         │
│      your day"                     │
│                                     │
│  ┌────┐  ┌────┐  ┌────┐           │
│  │Home│  │Stats│  │ AI │           │
│  └────┘  └────┘  └────┘           │
└─────────────────────────────────────┘
```

---

## Detailed Feature Specs

### 1. CAPTURE — Voice-First Conversational Logging

**Current**: User types messages, Gemini responds, ping-pong chat.
**New**: User presses mic, speaks freely, app extracts structured data.

#### How it works:

1. **User taps the large mic FAB on home screen**
2. **Continuous listening** begins (not single-shot like current)
   - Uses Android's `SpeechRecognizer` in continuous mode
   - Shows real-time transcript as user speaks
   - Animated waveform/pulse shows listening state
3. **User speaks naturally**:
   - "I woke up around 7, had breakfast till 8, then worked on the Android project from 8:30 to noon. Had lunch, took a nap. Went back to coding around 3 and worked till 6. Then gym for an hour."
4. **App processes with Gemini** (improved prompt):
   - Extracts: task name, approximate start/end times, duration
   - Handles fuzzy time ("around 7", "till noon", "for an hour")
   - Asks clarifying questions ONLY when needed ("What did you do between 1 PM and 3 PM?")
5. **Shows extracted tasks as editable cards**:
   - User can tap to edit any field (name, time, duration)
   - Confirm all / edit individual
   - Add more via voice or text
6. **Saves to Room DB** immediately on confirmation

#### Key improvements over current:
- **Continuous listening** vs single utterance
- **Batch extraction** vs back-and-forth chat
- **Visual confirmation** with edit capability vs trusting AI silently
- **Text fallback** always available (bottom sheet quick-add)

#### Technical changes:
- New `VoiceCaptureScreen` with animated recording UI
- New `TaskExtractionService` — sends transcript to Gemini, parses structured response
- Updated Gemini system prompt to return **JSON** (not CSV)
- New `TaskConfirmationSheet` — shows extracted tasks as editable cards
- `SpeechRecognizer` in streaming mode (replace current `RecognizerIntent`)

---

### 2. REFLECT — Dashboard & Analytics

**Current**: Single-day pie chart only.
**New**: Rich dashboard with daily/weekly/monthly views.

#### Home Screen (Daily View):
- **Today's summary card**: total hours tracked, task count
- **Mini donut chart**: proportional time breakdown
- **Activity timeline**: vertical list of today's tasks with colored indicators
- **Gaps indicator**: highlights untracked time blocks

#### Stats Screen (Multi-period):
- **Period selector**: Day / Week / Month tabs
- **Charts**:
  - Pie/donut chart for selected period
  - Bar chart showing daily totals across the week
  - Category breakdown (aggregate tasks by name)
- **Comparisons**: "This week vs last week" side-by-side
- **Top activities**: ranked list with time + percentage

#### Technical changes:
- New `HomeScreen` (replaces Chat as start destination)
- New `StatsScreen` with period tabs
- New `DashboardViewModel` — queries Room DB for aggregated data
- New Room queries in `TaskInfoDao`:
  - `getTasksForDateRange(startDate, endDate)`
  - `getTotalMinutesByDate(date)`
  - `getTopTasksByDuration(startDate, endDate, limit)`
  - `getDailyTotals(startDate, endDate)` for bar chart
- Replace YCharts pie chart with **Vico** library (more modern, better Compose support)

---

### 3. IMPROVE — AI Insights (Smart Coaching)

**Current**: Disabled. Generated raw SQL queries.
**New**: Pre-built insight cards powered by Gemini.

#### How it works:
- **Not a chat**. Instead, a scrollable feed of insight cards.
- The app runs predefined queries against Room DB, then sends results to Gemini for **natural language interpretation**.

#### Insight cards:
| Card | Data Source | AI Interpretation |
|------|-----------|-------------------|
| "Your week in review" | Weekly task totals | "You spent 40% of your week coding, up from 30% last week" |
| "Time gaps" | Untracked hours | "You have 3h unaccounted for today. What were you doing?" |
| "Consistency streak" | Daily tracking count | "You've tracked 5 days in a row! Keep going." |
| "Top time consumers" | Aggregated durations | "Meetings took 12h this week. That's 30% of your tracked time." |
| "Trend alert" | Week-over-week comparison | "Gym time dropped 50% this week compared to last." |

#### Technical changes:
- New `InsightsScreen` with card-based UI
- New `InsightsViewModel` — runs queries, sends to Gemini for interpretation
- Gemini prompt changed: **"Given this data: [task data], generate a brief insight in 1-2 sentences"**
- No raw SQL generation — all queries are predefined in DAO
- Cached insights (regenerate daily or on-demand)

---

### 4. IMPROVED DATA MODEL

#### Current TaskInfo:
```kotlin
TaskInfo(id, date, startTime, endTime, durationInMins, name)
```

#### New TaskInfo (enhanced):
```kotlin
TaskInfo(
    id: Int,
    date: String,           // yyyy/MM/dd
    startTime: String,      // hh:mm a
    endTime: String,        // hh:mm a
    durationInMins: Int,
    name: String,
    category: String?,      // AI-suggested: "Work", "Health", "Personal", etc.
    source: String          // "voice", "text", "import"
)
```

Add a `Category` entity:
```kotlin
Category(
    id: Int,
    name: String,           // "Work", "Health", "Personal", "Learning"
    color: Long             // color for charts
)
```

#### Migration strategy:
- Room migration to add `category` and `source` columns
- Gemini prompt updated to also suggest category
- User can customize categories

---

### 5. NAVIGATION REDESIGN

#### Current:
```
Chat (start) → PieChart
                → SheetSync
```

#### New:
```
Home (start) ──→ Voice Capture ──→ Task Confirmation
     │
     ├──→ Stats (Day/Week/Month)
     │
     ├──→ Insights (AI cards)
     │
     └──→ Settings
              ├── Categories
              ├── Sheet Sync
              └── About
```

Bottom navigation bar with 3 tabs: **Home**, **Stats**, **Insights**

---

### 6. UI/UX DESIGN LANGUAGE

#### Theme:
- **Dark mode first** (easier on eyes for daily use)
- Calm, focused color palette (navy/slate background, accent green/blue)
- Large typography for key numbers
- Subtle animations for transitions
- Haptic feedback on voice start/stop

#### Voice UI states:
```
IDLE       → Large mic button with subtle pulse
LISTENING  → Animated waveform, live transcript
PROCESSING → Loading animation, "Thinking..."
CONFIRMING → Task cards with edit/confirm buttons
ERROR      → Gentle retry message
```

---

## Implementation Phases

### Phase 1: Foundation (Data + Navigation)
1. Create new branch from main
2. Add `category` and `source` fields to TaskInfo (Room migration)
3. Add new DAO queries for date ranges, aggregations
4. Set up bottom navigation (Home, Stats, Insights)
5. Create empty screen scaffolds for all new screens

### Phase 2: Voice Capture (Core Feature)
1. Build `VoiceCaptureScreen` with continuous `SpeechRecognizer`
2. Redesign Gemini prompt for JSON extraction
3. Build `TaskExtractionService`
4. Build `TaskConfirmationSheet` (editable cards)
5. Integrate end-to-end: speak → extract → confirm → save

### Phase 3: Home Screen & Dashboard
1. Build `HomeScreen` with today's summary card
2. Activity timeline component
3. Mini donut chart
4. Quick-add FAB with voice trigger

### Phase 4: Stats Screen
1. Period selector (Day/Week/Month)
2. Pie/donut chart for selected period
3. Bar chart for daily totals
4. Top activities list
5. Week-over-week comparison

### Phase 5: AI Insights
1. Predefined insight queries in DAO
2. Gemini-powered natural language interpretations
3. Card-based UI feed
4. Cache layer for insights

### Phase 6: Polish & Settings
1. Dark theme refinement
2. Settings screen (categories, sheet sync, about)
3. Onboarding flow for first-time users
4. Edge cases (empty states, error handling)
5. Keep existing Sheet Sync under Settings

---

## Files to Create (New)
```
feature/
  home/
    HomeScreen.kt
    HomeViewModel.kt
    comp/
      TodaySummaryCard.kt
      ActivityTimeline.kt
      TimelineItem.kt
  capture/
    VoiceCaptureScreen.kt
    VoiceCaptureViewModel.kt
    TaskExtractionService.kt
    comp/
      WaveformAnimation.kt
      LiveTranscript.kt
      TaskConfirmationSheet.kt
      EditableTaskCard.kt
  stats/
    StatsScreen.kt
    StatsViewModel.kt
    comp/
      PeriodSelector.kt
      DonutChart.kt
      DailyBarChart.kt
      TopActivitiesList.kt
  insights/
    InsightsScreen.kt
    InsightsViewModel.kt
    comp/
      InsightCard.kt
  settings/
    SettingsScreen.kt
    comp/
      CategoryManager.kt
domain/model/
  Category.kt
data/local/dao/
  CategoryDao.kt
ui/main/
  BottomNavBar.kt
```

## Files to Modify (Existing)
```
domain/model/TaskInfo.kt          → add category, source fields
data/local/LocalDB.kt             → add migration, CategoryDao
data/local/dao/TaskInfoDao.kt     → add new queries
di/Koin.kt                        → register new ViewModels and DAOs
ui/main/NavHostGraph.kt           → new navigation structure
ui/main/Screen.kt                 → new screen routes
ui/main/MainActivity.kt           → bottom nav setup
feature/genAI/GeminiModel.kt      → new system prompts for JSON extraction + insights
ui/theme/Color.kt                 → new calm color palette
ui/theme/Theme.kt                 → dark-first theme
```

## Files to Keep (Unchanged or Minor Changes)
```
feature/sheetSync/                 → move under settings, keep logic
feature/voice/SpeechRecognitionButton.kt → may refactor for continuous mode
android/helper/                    → keep all helpers
```

## Files Potentially Deprecated
```
feature/chat/                      → replaced by VoiceCapture + Home
feature/pieChart/                  → replaced by Stats screen
```

---

## Key Decisions Needed from You

1. **Chart library**: Keep YCharts or switch to Vico? (Vico has better Compose M3 support)
2. **Categories**: Should AI auto-categorize, or should user pick? (Recommend: AI suggests, user confirms)
3. **History**: Should old chat-based data be migrated to the new format, or fresh start?
4. **Sheet Sync**: Keep as-is under Settings, or remove entirely?
5. **Minimum Android version**: Currently compileSdk 34 — keep or update?
