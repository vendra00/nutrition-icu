# AI insights knowledge base

Drop reference **PDF** files here — clinical nutrition studies, ESPEN/ASPEN guidelines, unit
protocols, etc. On the first **Generate insights** after startup, the app extracts the text of every
`*.pdf` in this folder and sends it to Claude as cached reference material, so its recommendations are
grounded in these documents (and it cites them by file name).

## Citations (optional `references.json`)

By default a reference is shown by its **title** (from the PDF metadata, falling back to a heuristic, then
the file name), and each is a link that opens the PDF. To show **curated full citations** (e.g. Vancouver),
add a `references.json` here mapping each PDF file name to its citation:

```json
{
  "espen-icu-2019.pdf": "Singer P, et al. ESPEN guideline on clinical nutrition in the ICU. Clin Nutr. 2019;38(1):48-79."
}
```

Files listed there show/cite that exact text; files not listed fall back to the derived title. See
`references.json.example` for a starting point. The sidecar is read with the knowledge base, so restart the
app after editing it. PDF titles can't reliably yield authors/journal/year, so this sidecar is the way to
get accurate full citations.

Notes:
- Only `.pdf` files are read. Text is extracted with the same PDFBox path used for lab reports (OCR
  fallback for scanned PDFs).
- These are loaded **once and cached**; after adding or removing PDFs, restart the app (or trigger a
  reload) to pick up the change.
- Combined text is capped (~240k characters) to bound token cost; keep the library curated.
- Prefer public guidelines/papers. Do **not** put patient data or PHI here.
- Change the folder with `app.insights.knowledge-root` in `application.properties`.
