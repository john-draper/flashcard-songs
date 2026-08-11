# 🎵 Song Lyric Flashcards

A growing collection of **self-contained, mobile-friendly HTML flashcard apps** for studying the
vocabulary of specific songs. Each song is its own deck — flip cards, mark what you've learned,
and save/load your progress — all in a single HTML file with **no build step and no server**.

🌐 **Live site:** https://john-draper.github.io/flashcard-songs/

## Songs
| Song | Artist | Cards |
|------|--------|------:|
| 男の子の目的は何？ _(Otoko no Ko no Mokuteki wa Nani)_ | 高嶺のなでしこ · HoneyWorks | 75 |

## How it's organized
```
flashcard-songs/
├── index.html              # the hub site (lists every song as a card)
└── songs/
    └── <song-slug>/        # one folder per song
        ├── index.html      # the self-contained flashcard app
        ├── mv-thumb.jpg
        ├── lyrics-source.txt
        └── README.md
```

## Add a new song
1. Create a folder: `songs/<your-song-slug>/`
2. Drop in a self-contained flashcard `index.html` (copy an existing deck as a template).
3. Register it in the **`SONGS`** array at the top of the root `index.html`.
4. Commit & push — GitHub Pages updates automatically.

## Each song deck features
- Two-sided flip cards (Japanese + reading  ⇄  romaji + English)
- Mark-as-studied with a live progress bar
- Save / load your progress to a JSON file
- Shuffle, reset, and keyboard shortcuts (Space = flip, ←/→ = nav, S = mark)
- Auto-saves to your browser between sessions

## Run locally
Just open `index.html` (the hub) or any `songs/<slug>/index.html` in a browser. Nothing to install.

## Deploy
GitHub Pages serves the **`main`** branch from the repo **root**. Any push to `main` publishes the site.

## Notes
- Lyrics are sourced from publicly available pages and included for study/reference.
- Vocabulary segmentation, readings, and English glosses are study aids and may not be the only valid interpretation.
- Banner/thumbnail images are the songs' official music-video stills, used for theming.
