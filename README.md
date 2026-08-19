# WeightTracker

A small Android app for logging your weight each day, setting a goal, and getting a text message
when you hit it.

It works. It worked before, too — but the way it was built made it hard to change and impossible
to test. This rewrite is about the *how*, not the *what*. The features are the same; the
structure underneath them is entirely different.

The original code is preserved in [`original/`](original/) so you can read the two side by side.

---

## What was wrong with the old version

Nothing you'd notice using it. Everything you'd notice trying to work on it.

All five source files were Activities or things Activities poked at directly. `MainActivity`
held a database helper. `DashboardActivity` held the same one. So did `WeightAdapter` — the class
whose only job is drawing rows on screen was also running `DELETE` statements. Every database
call happened on the main thread. Passwords were stored as plain text.

The part that bothered me most was subtler: after every write, the code had to remember to call
`refresh()` and `refreshSummary()` by hand. That's cache invalidation, and doing it manually
means eventually forgetting it somewhere.

There were also no real tests. The two test files were the ones Android Studio generates for you,
and one of them asserts that `2 + 2 == 4`.

---

## What it looks like now

```
Activity  ──observes──▶  ViewModel  ──calls──▶  Repository  ──▶  Room DAO
   │                         │                      │
platform stuff          business rules         persistence
(views, dialogs,        (validation,           (SQL, threading)
 permissions)            orchestration)
```

Four layers, each with one job. The Activity does the things that genuinely need an Activity.
The ViewModel holds state and decides what things mean. The Repository owns the data. Room
handles SQL.

There's also a `domain` package that has **zero Android imports** — pure Java, all of it
testable on a plain JVM in about a second. That constraint turned out to drive most of the good
decisions in this project.

| | Before | After |
|---|---|---|
| Database | Hand-written SQL, raw `Cursor` | Room with typed DAOs |
| Threading | All on the main thread | Explicit background/main executors |
| Passwords | Plain text | Salted PBKDF2, 120,000 iterations |
| Refreshing the list | Manual, after every write | Automatic — Room re-emits on change |
| Errors | Toasts and `-1` return codes | A `Result` type with named errors |
| Tests | 2 generated stubs | 96 real ones |

---

## Decisions I'd want to explain in a code review

### Splitting the SMS feature three ways

Sending a congratulations text sounds like one feature. It's actually three things that belong in
three different places:

- **Deciding** whether to send — is the weight at or under goal, is there a phone number? That's
  a business rule with no Android in it.
- **Checking** whether we're allowed to send — that's platform state, and only an Activity knows.
- **Actually sending** — also platform.

So the ViewModel does only the first one, and it emits a *request*, not a command: "this person
hit their goal, here's the number." The Activity decides whether it can act.

A consequence that felt wrong at first: **the ViewModel never finds out whether the message
actually sent.** I wanted to return a boolean. But delivery genuinely isn't the ViewModel's
business, and wanting that boolean was me not trusting the boundary I'd just drawn.

The rule itself lives in `GoalPolicy`, which exists for an unglamorous reason: I wanted to test
what happens when your weight *exactly equals* your goal, and I couldn't test that while it was
buried inside an Activity. Pulling it out made it a six-line function with seven tests. Trying to
make something testable showed me a seam that should have been there anyway.

### The rotation bug

This is my favourite thing in the project.

`LiveData` re-sends its most recent value to any new observer. That's exactly right for "the
current list of weights." It's exactly wrong for "send a text message now" — because rotating
your phone destroys and recreates the Activity, which re-subscribes, which receives the old value
again, and sends a second text. Rotate five times, send five texts. It costs the user real money.

The fix is a small wrapper called `Event` that lets its contents be read once and then returns
null forever after. There's a better-known class for this called `SingleLiveEvent`, but it
achieves the same thing by quietly breaking the `LiveData` contract — with two observers attached,
one of them randomly never fires. Google deprecated it. `Event` leaves delivery alone and puts
the "has this been handled?" question on the data itself, where you can see it at the call site.

I verified it on the emulator: trigger the SMS, rotate four times, nothing else sends.

### Passwords, and an API level that got in the way

The obvious algorithm is PBKDF2 with SHA-256. Android only guarantees it from API 26, and this
app supports 24. So the code asks the platform whether it has SHA-256 and falls back to SHA-1 if
not — asking the real question rather than checking a version number as a proxy for it.

That fallback creates a problem: a password hashed on an old phone has to still work after the
user upgrades. So the stored value describes itself:

```
pbkdf2$PBKDF2WithHmacSHA256$120000$5e4ef53ec0a481f3...$3fca12533ba33acc...
```

Verification reads the algorithm and iteration count out of that string instead of assuming
today's defaults. Same trick bcrypt uses. It also means I can raise the iteration count later
without locking anyone out — the parameters travel with the data.

Small thing I'm pleased about: the encoding is hex rather than base64, because Android's base64
throws in unit tests and Java's isn't available on API 24. Ten lines of hex works everywhere,
keeps the class free of Android imports, and you can read the salt with your own eyes in Device
Explorer.

### A bug I created by fixing a different one

Moving the database off the main thread is unambiguously correct. It also introduced a bug.

In the old version, logging in blocked the main thread, so the UI was physically frozen and you
*couldn't* tap the button twice. That was never a design decision — it was an accident of doing
something slow in the wrong place. Once hashing moved to a background thread, the buttons stayed
responsive, and a double-tap would register you twice.

So there's now a `busy` flag that guards both ends. The lesson I took from it: making software
better along one axis can quietly remove a safeguard you didn't know you were relying on.

### Where validation lives

One rule settled every case: **if a check needs no I/O, it's a pure function; if it needs the
database, it belongs in the repository.**

"Is this a valid weight?" — answerable from the string alone. Pure function. "Is this username
taken?" — only the database knows. Repository.

Worth knowing: `Double.parseDouble("183f")` returns `183.0` without complaint. It also accepts
`"NaN"` and `"Infinity"`. A `try/catch` around it isn't validation — you have to check the shape
first. There's a test for each of those.

---

## Things I deliberately didn't build

Knowing when to stop is part of this.

- **Hilt or Dagger.** The entire object graph is nine readable lines in one file. Adding a
  dependency-injection framework would solve a problem this app doesn't have yet.
- **`SavedStateHandle`.** It would let `userId` survive the process being killed — but the app has
  no session persistence at all, so you'd land back at the login screen anyway. Solving half a
  problem is worse than leaving it visible.
- **Timing-equalised login.** Right now an unknown username fails faster than a wrong password,
  which is technically a side channel. Three lines to fix. Skipped because there's no network and
  no attacker who could measure it — but it's the first thing I'd revisit if this ever got an API.
- **ViewModel unit tests.** They'd need an extra test dependency, and they'd assert almost
  nothing, because all the logic already moved out into classes that *are* tested. The fact that
  a ViewModel test would be boring is evidence the extraction worked.

---

## Bugs fixed along the way

1. The weight field never cleared after adding an entry — type `185.5` then `183.0` and it stored
   `185.51830`.
2. `if (userId <= 0) finish();` was missing its `return`, so the code kept running with an
   invalid user.
3. RecyclerView was used but never declared as a dependency; it only worked by accident.
4. Weights displayed at raw float precision (`185.5183`).
5. `SmsManager.getDefault()` was deprecated.
6. Long-press deleted a row instantly — no confirmation, no undo.
7. Entries on the same date had no defined sort order, so the list could reshuffle on its own.

Separately, the manifest was missing both `DashboardActivity` and the `SEND_SMS` permission,
which crashed the app the instant you pressed Login. That's fixed in commit `c8c7134`, before any
of this work started.

---

## Running it

Needs JDK 21 — the Android Gradle Plugin rejects newer versions.

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew testDebugUnitTest    # 96 tests, no emulator needed
./gradlew assembleDebug
```

The tests run in seconds without a device, which is the whole payoff for the layering. The one
honest gap: the fake database objects hand back `LiveData` they never update, so the tests cover
writes rather than the observing. That part is verified by running the app.

If you want to see the interesting behaviour yourself: set a goal and a phone number, log a
weight at or under it so the text fires — then rotate the phone a few times. Nothing sends again.
That's the whole reason `Event` exists, in one thing you can watch happen.
