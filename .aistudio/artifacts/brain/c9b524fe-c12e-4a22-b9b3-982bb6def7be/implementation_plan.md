# Chaoxing-Style Timetable & Pinyin Schedule System

Transform the course schedule application into an authentic, high-fidelity clone of the Chaoxing (超星学习通) university timetable interface based on user screenshots, featuring offline ruby Pinyin annotations for Chinese course characters, a standard 12-session daily timetable grid, dedicated Add Schedule & Course Details screens, and dynamic startup navigation.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarifying questions:
> - **Pinyin Ruby Annotation**: Auto-generate tone-marked Pinyin above Chinese characters for course titles, teachers, and classrooms with a global toggle switch in the menu/settings.
> - **Session Architecture**: Strict 12-session standard university day (Period 1 at 08:00 through Period 12 at 22:50) with multi-session duration calculation (`Course Begin` + `Session Number`).
> - **Dynamic Navigation**: Smart dynamic flow allowing the user to select their default view (Timetable Grid vs. Dashboard) or dynamically open the Timetable during active class weeks.

- **Confirmed Decision 1**: Pinyin pronunciation guides will be rendered as ruby annotations (拼音注音) placed neatly above each corresponding Hanzi character using an offline phonetic lookup engine.
- **Confirmed Decision 2**: Timetable grid uses the authentic Chaoxing layout with week dropdown (`Week 8 ∨`), current day date-pill header (`Monday 10-19`), dashed session dividers, and pastel course cards.
- **Confirmed Decision 3**: The top-right hamburger menu (`≡`) provides seamless access to the side drawer / sheet for switching to Dashboard, Tasks & Exams, Study Focus, and Offline Backup.

---

## 1. Overview & Core Concept

- **What It Does**: Provides international and university students with a replica of the Chaoxing timetable interface. Students can view multi-session classes with Pinyin annotations, add new courses with period start/span selectors, inspect clean course details cards, and toggle Pinyin guides on or off.
- **Target Audience / Persona**: University students (especially international students studying in China or taking bilingual courses) who need visual Pinyin pronunciation aids alongside Chinese course and building names.
- **Key Value**: Delivers the exact look, feel, and ergonomics of the Chaoxing timetable while maintaining 100% offline security, zero cloud telemetry, and integration with local Room persistence.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Viewing the Chaoxing Timetable Grid (Screenshots 3 & 4)**:
   - Top app bar displays a back/drawer toggle, a prominent centered `Week 8 ∨` week selector, and a top-right hamburger menu (`≡`).
   - The weekday header row displays `Monday 10-19`, `Tuesday 10-20`, ..., `Sunday 10-25`. The current day is highlighted with a rounded light-blue pill (`#E0F2FE` background with `#0284C7` border and text).
   - Left vertical time ruler displays 12 periods with exact time intervals (e.g. `1  8:00-8:50`, `3  10:10-11:00`, `9  19:00-19:50`).
   - Courses spanning multiple sessions (e.g. Sessions 3–5) render as single continuous rounded blocks with custom pastel colors (sky blue, emerald, salmon, ochre).
   - Each course card renders Chinese characters with miniature ruby Pinyin directly above each character, plus `@Classroom` with Pinyin.
   - Tapping an empty grid slot launches the **Add Schedule** screen pre-populated with that day and session.

2. **Inspecting Course Details (Screenshot 2)**:
   - Tapping an active course block opens the Course Details screen.
   - Top bar shows `<` Back arrow, session badge subtitle (e.g. `10-19 Monday The 3-5Session`), and a right-aligned `Edit` text button.
   - A minimalist white card container displays:
     - Large course title with ruby Pinyin text: `计算机组成原理(国际学生)`
     - `Teacher: 徐媛媛` with ruby Pinyin: `xú yuán yuán`
     - `Weekly: The 8Week` (or active week span)
     - `Take place at: 明理楼B105` with ruby Pinyin and chevron `>`
     - `Note:` row with chevron `>`

3. **Adding / Editing a Schedule (Screenshot 1)**:
   - Tapping `Edit` or `+` opens the full-screen "Add schedule" view with clean row items:
     - **Course**: Text input with live generated Pinyin preview.
     - **Teacher**: Instructor name input with live Pinyin preview.
     - **Class Week**: Dropdown modal to select target week or multi-week range.
     - **Class Time**: Day selector (`Monday` to `Sunday`).
     - **Course Begin...**: Session picker (`Session 1` through `Session 12`).
     - **Session Number**: Span count (e.g., `1`, `2`, `3`, or `4` sessions).
     - **Take place at**: Classroom location with live Pinyin preview.
     - **Delete Button**: Soft-red/blue outlined action button at the bottom (when editing).
     - Top bar has `Finish` action button in brand blue to validate and commit to Room.

### Visual Identity & Theme

- **Aesthetic Direction**: Clean, high-density academic minimalism matching Chaoxing Mobile. Pure white surface backgrounds (`#FFFFFF`), whisper-soft dividers (`#F1F5F9`), and crisp typographic hierarchy.
- **Pinyin Ruby Typography**: Two-tier vertical text composable: top tier is 9sp tone-marked Pinyin, bottom tier is 14sp/16sp bold Chinese Hanzi, precisely centered per glyph.
- **Card Color Palette**:
  - Sky Blue: `#60A5FA` / `#38BDF8` (Computer Architecture / Systems)
  - Jade Green: `#10B981` / `#059669` (Database Systems & Software Lab)
  - Salmon Coral: `#F87171` / `#FB7185` (Data Science Programming)
  - Ochre Gold: `#D97706` / `#B45309` (Neural Networks & Deep Learning)
  - Royal Indigo: `#6366F1` / `#4F46E5` (Machine Learning & Analytics)

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Offline Lightweight Hanzi-to-Pinyin Engine**:
  - *Chosen Approach*: Implement a zero-dependency offline Pinyin lookup table covering common educational and campus vocabulary, Hanzi Unicode blocks, and tone-mark diacritics (`ā á ǎ à`).
  - *Why*: 100% offline requirement prevents using cloud translation or online OCR APIs. The local map executes in <1ms without network latency.
  - *Fallback*: Non-Chinese characters (e.g., numbers, Latin letters, brackets like `(国际学生)`) pass through unchanged.
- **Decision 2: 12-Period Fixed Grid Calculation**:
  - *Chosen Approach*: Standardize periods 1–12 with standard Chinese university bell times (Morning: 1–4, Afternoon: 5–8, Evening: 9–12).
  - *Why*: Directly aligns with user screenshots and timetable matrix structures.
- **Decision 3: Dynamic App Startup Navigation**:
  - *Chosen Approach*: Add a "Default Startup Screen" setting in `UserPreferencesManager` (options: `Chaoxing Timetable`, `Smart Dynamic`, `Dashboard`).
  - *Why*: Satisfies user's requirement to make screen flow dynamic while preserving quick access to all planner tools via bottom bar or hamburger drawer.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             MainActivity & Scaffold                         │
│  ┌───────────────────────┬──────────────────────┬────────────────────────┐  │
│  │   Chaoxing Grid View  │ Course Details View  │  Add Schedule Screen   │  │
│  │   - Week 8 Selector   │ - Ruby Pinyin Title  │  - Session Begin / Num │  │
│  │   - Current Date Pill │ - Teacher / Room     │  - Live Pinyin Preview │  │
│  │   - 12-Period Layout  │ - Edit Navigation    │  - Finish / Delete     │  │
│  └───────────▲───────────┴──────────▲───────────┴───────────▲────────────┘  │
│              │                      │                       │               │
│  ┌───────────┴──────────────────────┴───────────────────────┴────────────┐  │
│  │                    RubyText & PinyinEngine (Local)                    │  │
│  │            - Offline Hanzi-to-Pinyin Tone Converter                   │  │
│  │            - Global Pinyin Toggle (StateFlow<Boolean>)                │  │
│  └──────────────────────────────────▲────────────────────────────────────┘  │
│                                     │                                       │
│  ┌──────────────────────────────────┴────────────────────────────────────┐  │
│  │                     ScheduleViewModel & Repositories                  │  │
│  │            - Courses, Periods (1-12), Timetable Filtering             │  │
│  │            - UserPreferencesManager (Show Pinyin, Default Tab)        │  │
│  └──────────────────────────────────▲────────────────────────────────────┘  │
│                                     │                                       │
│  ┌──────────────────────────────────┴────────────────────────────────────┐  │
│  │                         Room Database (Offline)                       │  │
│  │          courses (periods, dayOfWeek, startPeriod, endPeriod)         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Entity Mapping

- **Period Calculation**:
  - `startPeriod`: 1 to 12 (corresponding to `Course Begin...`)
  - `endPeriod`: `startPeriod + sessionNumber - 1`
  - `startTime` & `endTime`: Computed automatically from the 12-session timetable schedule matrix.
- **Pinyin Support**:
  - `PinyinEngine.getPinyinWithTones(text: String): List<Pair<Char, String>>`
  - Generates character-by-character ruby alignment for UI rendering.
- **User Preferences**:
  - `KEY_SHOW_PINYIN`: Boolean toggle (default: `true`).
  - `KEY_DEFAULT_STARTUP_SCREEN`: String (`TIMETABLE` / `DYNAMIC` / `DASHBOARD`).

### Interactive Component & State Mapping

1. **Top Week Switcher Dropdown (`Week 8 ∨`)**: Opens an animated modal sheet with weeks 1 to 16. Selecting a week dynamically updates the timetable grid courses and weekday dates (`10-19` to `10-25`).
2. **RubyText Composable**: Custom layout that stacks tone-marked Pinyin above Chinese characters with aligned horizontal widths, preventing text clipping.
3. **Add Schedule Form Inputs**:
   - `Course Begin...` selects session `1..12`.
   - `Session Number` selects `1..4` sessions duration.
   - Live Pinyin subtitle renders beneath each text field as the user types.
   - Tapping `Finish` validates inputs, checks for time collisions via `CourseConflictDetector`, inserts into Room, and triggers instant grid refresh.
4. **Delete Action**: Triggers an alert confirmation and removes the course from Room.
