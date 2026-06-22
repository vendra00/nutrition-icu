# AI insights knowledge base

Drop reference **PDF** files here — clinical nutrition studies, ESPEN/ASPEN guidelines, unit
protocols, etc. On the first **Generate insights** after startup, the app extracts the text of every
`*.pdf` in this folder and sends it to Claude as cached reference material, so its recommendations are
grounded in these documents (and it cites them by file name).

Notes:
- Only `.pdf` files are read. Text is extracted with the same PDFBox path used for lab reports (OCR
  fallback for scanned PDFs).
- These are loaded **once and cached**; after adding or removing PDFs, restart the app (or trigger a
  reload) to pick up the change.
- Combined text is capped (~240k characters) to bound token cost; keep the library curated.
- Prefer public guidelines/papers. Do **not** put patient data or PHI here.
- Change the folder with `app.insights.knowledge-root` in `application.properties`.
