# Original Implementation (Pre-MVVM Baseline)

**Reference only. This directory is not compiled.**

It sits beside `app/` rather than inside it, and `settings.gradle` includes only `:app`, so the
Android Gradle Plugin never sees these files. They exist so the original design can be read
side by side with the rewrite.

## What this is

The activity-centric version of WeightTracker as originally submitted, preserved verbatim
before the MVVM rewrite began.

**One change from the as-submitted version:** `AndroidManifest.xml` here includes a
`DashboardActivity` declaration and the `SEND_SMS` permission. The submitted manifest was
missing both, which made the app crash with `ActivityNotFoundException` the moment you pressed
Login — the entire dashboard, all CRUD, and the SMS feature were unreachable. That two-line fix
is committed separately as `c8c7134`, so this snapshot is a *working* baseline rather than a
crashing one. Everything else is untouched.

## Architecture of this version

```
MainActivity ──▶ DBHelper (SQLiteOpenHelper, raw SQL, main thread)
DashboardActivity ──▶ DBHelper
                 └──▶ WeightAdapter ──▶ DBHelper     <-- adapter writes to the DB directly
```

Everything lives in the Activities: UI, validation, business rules, and SQL. There are five
source files and no layer boundaries.

## Characteristics worth comparing against the rewrite

| Aspect | This version |
|---|---|
| Persistence | Hand-written SQL over `SQLiteOpenHelper`, raw `Cursor` passed into the adapter |
| Threading | None — every database call runs on the main thread |
| Passwords | Stored in plaintext (`DBHelper.registerUser`, line 57) |
| List updates | Manual: `swapCursor()` + `notifyDataSetChanged()` + `refreshSummary()` |
| Error handling | Ad-hoc `Toast` calls and `-1` sentinel returns |
| Configuration change | State rebuilt from scratch in `onCreate` |
| Tests | Two untouched Android Studio templates (`assertEquals(4, 2 + 2)`) |
| Separation of concerns | None — `WeightAdapter` holds a `DBHelper` and deletes rows itself |

## Known defects present in this code

1. **Weight input field is never cleared after a successful Add.** The next entry concatenates
   onto the leftover text — entering `185.5` then `183.0` stores `185.51830`. Reproduced live
   during baseline testing.
2. **`DashboardActivity` line 37**: `if (userId <= 0) finish();` has no `return`, so `onCreate`
   continues executing and runs every query with an invalid user id.
3. **`androidx.recyclerview` is used but never declared as a dependency** — it resolves only
   transitively through `com.google.android.material`.
4. Weights render at raw float precision (`185.5183`) with no formatting.
5. `SmsManager.getDefault()` is deprecated.
6. Long-press deletes a row immediately, with no confirmation and no undo.
7. `ORDER BY date_text DESC` with no tiebreaker — entries sharing a date have unspecified order.

All seven are addressed in the rewrite. See the root `README.md` for the reasoning behind each
architectural decision.
