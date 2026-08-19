# WeightTracker — CS-360

A daily weight tracking app for Android, rewritten from an activity-centric design into a
layered MVVM architecture.

The original submission is preserved verbatim under [`original/`](original/) for side-by-side
comparison. This document explains what changed and, more importantly, **why** — the reasoning
is the point of the exercise, not the line count.

---

## Before and after

```
BEFORE                                    AFTER

MainActivity ──▶ DBHelper                 LoginActivity ─observes─▶ LoginViewModel
    │            (raw SQL,                                              │
    │             main thread,                                          ▼
    │             plaintext passwords)                          UserRepository
    │                                                                   │
DashboardActivity ──▶ DBHelper                                          ▼
    │                                                          UserDao (Room)
    └──▶ WeightAdapter ──▶ DBHelper
              ▲                            DashboardActivity ─observes─▶ DashboardViewModel
              └── the adapter wrote                                          │
                  to the database                                            ▼
                                                                    WeightRepository
                                                                             │
5 files, 0 real tests                                                        ▼
                                                              WeightDao / GoalDao (Room)

                                          33 files, 96 unit tests
```

| | Before | After |
|---|---|---|
| Persistence | Hand-written SQL, raw `Cursor` | Room, typed DAOs, `LiveData` queries |
| Threading | Everything on the main thread | Explicit `AppExecutors` (io / main) |
| Passwords | Plaintext | Salted PBKDF2, 120,000 iterations |
| List updates | Manual `swapCursor` + `refreshSummary` | Automatic — Room re-emits on write |
| Errors | `Toast` calls and `-1` sentinels | Typed `Result<T>` + `AppError` enum |
| Dependencies | `new DBHelper(this)` inside Activities | Constructor injection via `AppContainer` |
| Tests | 2 template stubs | 96 tests, no emulator required |

The layering rule: **each layer has one job.** The Activity handles platform concerns
(views, permissions, dialogs). The ViewModel holds state and applies business rules. The
Repository owns data and hides persistence. The `domain` package is pure Java with zero
`android.*` imports.

---

## Design decisions worth explaining

### Validation lives in two places, and there is a rule for which

> *Validation that needs no I/O is a pure function called by the ViewModel.
> Validation that requires the datastore lives in the Repository.*

That single sentence decides every case. Whether a weight parses is answerable from the string
alone → `WeightValidator`. Whether a username is taken is answerable only by the UNIQUE index →
`UserRepository`. Ask "what does this check need in order to run?" and the answer places it.

### `Double.parseDouble` is not validation

`Double.parseDouble("183f")` returns `183.0`. It also accepts `"NaN"`, `"Infinity"`, and hex
float literals. A `try/catch` around it — which is what most code does — lets all of those
through. `WeightValidator` checks the shape with a regex *before* parsing, then range-checks the
result. Each of those cases has a test.

Relatedly, `validateDate` is not cosmetic. Entries are sorted with `ORDER BY date_text DESC`, a
lexicographic sort over text, which coincides with chronological order **only** while every
string is zero-padded ISO-8601. Let `2026-8-5` through and it sorts after `2026-12-01`, silently
and permanently. **The sort's correctness depends on a format invariant, so enforcing the format
is a data-integrity concern.**

### The SMS feature, split three ways

Sending a goal-reached text involves three different kinds of work, and they belong in three
different places:

1. **The decision** — is the latest weight at or under goal, and is there a phone number?
   Pure business logic → `domain/GoalPolicy`.
2. **The capability check** — do we hold `SEND_SMS`? Platform state → the Activity.
3. **The effect** — send it, show a Toast. Platform → `ui/SmsNotifier`.

The ViewModel owns only (1), and emits a **request** rather than a command. A direct consequence:
**the ViewModel never learns whether the SMS was actually sent.** That is correct — delivery is
not its business — and the urge to "just return a boolean" is the instinct this design trains
away.

`GoalPolicy` exists because of a question: *how do I test the boundary case where the latest
weight exactly equals the goal?* Inside a ViewModel that requires `InstantTaskExecutorRule`, a
dependency this project does without. Extracting the rule made it testable in six lines.
**The pressure to make something testable revealed a seam worth having anyway.**

### `Event<T>` and the duplicate-SMS bug

`LiveData` is a state holder, not an event stream: it re-delivers its latest value to every newly
attached observer. Perfect for "the current list of weights", wrong for "send an SMS now". Rotate
the device after hitting your goal and the Activity is recreated, re-subscribes, receives the
stale value, and sends a second text. Rotate five times, send five texts.

`SingleLiveEvent` is the usual fix. It works, but it quietly breaks the `LiveData` contract —
attach two observers and one nondeterministically never fires. Google deprecated it for that
reason. Its failure mode is remote and silent.

`Event<T>` instead puts consumed-ness in the **data**, leaving delivery alone. Every observer
still receives every emission, exactly as promised; the *content* can only be taken once. Its
failure mode is local and visible, and it has no Android imports so it is unit-testable.
**The pattern that is easier to test is also the semantically honest one — that correlation is
not a coincidence.**

Verified on device: after the SMS fires, rotating four times sends nothing further.

### Password hashing under `minSdk 24`

`PBKDF2WithHmacSHA256` is only guaranteed from API 26; API 24–25 has only the SHA-1 variant. The
code **probes the provider** and falls back on `NoSuchAlgorithmException` rather than branching on
`Build.VERSION.SDK_INT` — it asks the question actually being asked, and keeps the class free of
`android.*`.

The stored value is self-describing:

```
pbkdf2$PBKDF2WithHmacSHA256$120000$5e4ef53ec0a481f3...$3fca12533ba33acc191ed976...
```

Verification reads the algorithm and iteration count **out of the stored string**, not from
current defaults. So a hash written on an API 24 device still verifies after the user upgrades,
and the iteration count can be raised later without invalidating any account. **Parameters travel
with the data** — the same reasoning behind bcrypt's `$2b$` prefix.

*The tradeoff:* on API 24–25 the PRF is HMAC-SHA-1. Weaker in principle, not practically broken —
SHA-1's published weaknesses are *collision* attacks, while PBKDF2 relies on preimage/PRF
properties with no practical break. The alternatives were raising `minSdk` to 26 (dropping real
devices) or bundling third-party crypto (a new dependency, and rolling your own is exactly what
not to do).

*Why hex and not base64:* `android.util.Base64` throws in JVM unit tests; `java.util.Base64` is
API 26+ on Android and would crash on precisely the devices the SHA-1 fallback exists for. Hex
costs ten lines, works everywhere, and is readable by eye in Device Explorer.

### A bug the rewrite itself introduced

In the original, `db.loginUserId()` blocked the main thread, so the UI was *physically* unable to
respond to a second tap while a query ran. Now that hashing takes 200 ms+ on a background thread,
the buttons stay live — and a double-tap fires two registrations.

**Moving work off the main thread removed an accidental safeguard.** The replacement has to be
deliberate: `LoginViewModel.isBusy()` guards at the ViewModel, and the Activity disables the
buttons. Worth noticing that making an app more correct in one dimension can open a hazard in
another.

### Why the database file was renamed

Pointing Room at the legacy `weighttracker.db` at version 1 throws. The file has no
`room_master_table`, so `checkIdentity()` validates the live schema directly, finds `password`
where `password_hash` is declared, and fails.

**`fallbackToDestructiveMigration()` does not catch this** — it only affects `onUpgrade`, which
never runs when the version numbers already agree. That is the surprising part: the option that
sounds like "wipe it if anything is wrong" does nothing here.

Declaring `version = 2` on the old filename *would* work, by forcing `onUpgrade` to run first.
Understanding why is the transferable part. A new filename was chosen instead because it needs no
such reasoning, and because it is the honest description: passwords are now salted hashes, so
every legacy row was unusable regardless. This genuinely is a new datastore.

### Smaller decisions

- **`OnConflictStrategy.IGNORE`** on user insert returns `-1` on a duplicate instead of throwing.
  Race-free (the index is the single source of truth, unlike check-then-insert), and the
  repository translates `-1` into a domain error at the layer boundary.
- **Login returns an identical error** for "no such user" and "wrong password". Distinguishing
  them is friendlier but hands an attacker a username-enumeration oracle. Pinned by a test, so a
  future "UX improvement" fails the build rather than quietly regressing security.
- **Immutable entities** — `DiffUtil` requires that already-submitted items are never mutated.
  Immutability guarantees that by construction rather than by everyone remembering.
- **`, id DESC` tiebreaker** — the original left same-date entries in unspecified order, which
  `DiffUtil` would render as rows visibly swapping for no reason.
- **Usernames are lowercased.** The original treated `Chris` and `chris` as different accounts.
- **Hand-written fakes, not Mockito.** Writing `FakeUserDao` forces the DAO's real contract to be
  articulated. **A fake is executable documentation of a contract; a mock is an assertion about a
  method call.**

---

## Defects fixed

| # | Defect | Where it was |
|---|---|---|
| 1 | Weight field never cleared after Add — `185.5` then `183.0` stored `185.51830` | `DashboardActivity` |
| 2 | `if (userId <= 0) finish();` missing `return`, so `onCreate` continued with an invalid id | `DashboardActivity:37` |
| 3 | `recyclerview` used but only resolved transitively via Material | `app/build.gradle` |
| 4 | Weights rendered at raw float precision (`185.5183`) | `WeightAdapter` |
| 5 | `SmsManager.getDefault()` deprecated | `SmsUtil` |
| 6 | Long-press deleted immediately, no confirmation, no undo | `WeightAdapter` |
| 7 | `ORDER BY date_text DESC` with no tiebreaker | `DBHelper` |
| — | Stale `position` captured in `onBindViewHolder` could edit the wrong row | `WeightAdapter` |

Not in the original submission but fixed before this work began: `AndroidManifest.xml` declared
neither `DashboardActivity` nor `SEND_SMS`, so the app crashed with `ActivityNotFoundException`
the moment Login was pressed. Committed separately as `c8c7134`.

---

## Deliberately not built

Knowing where to stop is part of the design. Each of these was considered and declined:

- **Immutable `UiState`** — a single state object per screen instead of several `LiveData`
  streams. Genuinely better for complex screens; here it would add Java boilerplate without
  preventing a bug this app can actually have.
- **`SavedStateHandle`** — would supply `userId` automatically and survive process death. Skipped
  because it hides wiring behind a magic string key, and more decisively because this app has no
  session persistence at all: killing the process returns you to login regardless. Making
  `userId` survive process death in an app whose *session* does not would be solving half a
  problem.
- **Hilt / Dagger** — solves a problem this app does not have. The entire object graph is nine
  legible lines in `AppContainer`. Knowing *where* that trade flips is the useful part.
- **`java.time`** — nicer than `SimpleDateFormat`, but needs core library desugaring plus
  `desugar_jdk_libs` for marginal benefit across two date operations.
- **Login timing equalisation** — hashing a dummy password when no user is found would close a
  real timing side channel. Three lines. Skipped because a local-only SQLite app with no network
  surface has no attacker positioned to measure it. If this ever grows a remote API, it is the
  first thing to revisit.
- **ViewModel unit tests** — they touch `LiveData` on every path, which needs
  `androidx.arch.core:core-testing`. This costs almost nothing *because the design already pushed
  the logic out of them*. The ViewModels are thin because they had to be testable-by-proxy, and
  the low value of a ViewModel test is evidence the extraction worked.

---

## Building and testing

Requires **JDK 21** — AGP 8.13 rejects newer JDKs:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew testDebugUnitTest    # 96 tests, no emulator needed
./gradlew assembleDebug
```

The unit tests run entirely on the JVM. That is not incidental: `domain/`, `ui/Event`,
`data/security/`, `util/WeightFormatter`, and both repositories have no `android.*` dependencies,
and `AppExecutors` takes its executors as constructor parameters so tests can pass
`Runnable::run` and make everything synchronous.

| Suite | Tests |
|---|---|
| `WeightValidatorTest` | 28 |
| `Pbkdf2PasswordHasherTest` | 15 |
| `CredentialsValidatorTest` | 12 |
| `WeightRepositoryTest` | 12 |
| `UserRepositoryTest` | 8 |
| `GoalPolicyTest` | 7 |
| `WeightFormatterTest` | 6 |
| `ResultTest` / `EventTest` | 8 |

**Known gap, stated rather than papered over:** the fake DAOs return `LiveData` they never mutate,
because `setValue` asserts the main thread and would throw in a JVM test. Repository tests
exercise command paths only; observation correctness is verified by running the app.

### Verified on device

Emulator, API 36. Register → login → add / edit / delete → goal → SMS → force-stop → re-login.
Zero `FATAL EXCEPTION` across the entire walk.

The one to reproduce: set a goal and phone, add a weight at or under goal so the SMS fires, then
**rotate the device several times.** No second message is sent. That is the whole justification
for `Event<T>`, in one observable behaviour.
