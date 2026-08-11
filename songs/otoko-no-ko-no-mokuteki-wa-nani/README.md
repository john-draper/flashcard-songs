# 🌸 男の子の目的は何？ — Japanese Flashcards

A mobile-friendly, **self-contained** flashcard app for learning the unique Japanese
vocabulary words from the song **「男の子の目的は何？」 / 高嶺のなでしこ (HoneyWorks)**.

## ▶️ How to use
1. Open **`index.html`** in any modern web browser (Chrome, Edge, Firefox, Safari) —
   on desktop *or* mobile. No server, no install, no internet needed.
2. Keep `mv-thumb.jpg` next to `index.html` (it's the themed banner image).

## 🃏 What it does
- **75 unique cards** — every distinct word in the song gets its own card (no repeats).
- **Two-sided cards:** front shows the Japanese word (kanji/kana) + reading;
  back shows **romaji + English meaning**. Tap the card (or Space) to flip.
- **Mark studied / not studied** with the big green button (or the `S` key).
- **Progress bar** tracks how many of the 75 words you've studied.
- **Save Progress** 💾 — downloads a JSON file containing a simple array of the
  word numbers you've learned, e.g. `[0, 3, 5, 12, ...]`.
- **Load Progress** 📂 — restore your progress by re-opening a saved JSON file.
- **Shuffle** 🔀 to randomize card order, **Reset** 🗑 to clear everything.
- Auto-saves to your browser's localStorage between sessions.

## ⌨️ Shortcuts
| Key | Action |
|-----|--------|
| Space / Enter | Flip card |
| ← / → | Previous / Next card |
| S | Toggle studied |

## 📁 Files
- `index.html` — the whole app (HTML + CSS + JS in one file)
- `mv-thumb.jpg` — song MV thumbnail used for the themed banner
- `lyrics-source.txt` — the full lyrics (for reference)

## ℹ️ Notes
- Lyrics sourced from uta-net (publicly available song page).
- Vocabulary segmentation, readings, and English translations are study aids and
  may not be the only valid interpretation.
- Banner image is the song's YouTube MV thumbnail.
