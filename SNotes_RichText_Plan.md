# Plan: Implementing Rich-Text Formatting in SNotes

To bring the rich-text formatting tools (Bold, Italic, Underline, etc.) from the standard Free-Text Notes into the floating text blocks of **SNotes**—without altering or breaking the existing standard Notes—we will take a decoupled approach. 

Here is exactly how that architecture would work:

### 1. Data Model Evolution (Room & JSON)
Currently, the `DrawingLine` class in SNotes stores plain text as a raw `String`. The standard Notes editor uses special characters (PUA markers like `\uE000`) or Markdown formatting to serialize bold/italic styles.
* **How to adapt SNotes:** We will start saving those same formatting markers directly into the `DrawingLine.text` string. Since the data model is just storing a string, no Room database migrations are required. 

### 2. Upgrading the SNote Text Editor UI
Right now, when you tap to type in SNotes, a standard `BasicTextField` pops up holding a plain `TextFieldValue`. 
* **How to adapt SNotes:** We will introduce a local formatting toolbar that *only* appears when `isTextMode` is active and you are typing. We can instantiate a secondary, sandboxed instance of the existing `RichTextEditorController` (used in standard Notes) purely for managing the `activeTextValue` inside `SNoteEditor`. 

### 3. Rendering Formatted Text on the Canvas
Currently, saved SNote text is drawn over the canvas using a standard Compose `Text` layer applying an unstyled layout.
* **How to adapt SNotes:** We will update the SNote `Text()` overlay renderer to check the `DrawingLine.text` payload. If the string contains the project's formatting markers, we parse them into an `AnnotatedString` (applying standard Compose `SpanStyle` for bolding/italics) before rendering. 

### Why this leaves standard Free-Text Notes untouched:
By keeping standard Notes and SNotes completely separate Compose views (`PageBodyEditor` vs `SNoteEditor`), we will only be borrowing the mathematical *parsing patterns* (the regex or PUA parsing) used in standard Notes and applying them strictly to the `DrawingLine` floating elements. The standard note database queries, view models, and `RichTextEditorController` singletons used by normal text documents will not be touched or modified in any way. 

