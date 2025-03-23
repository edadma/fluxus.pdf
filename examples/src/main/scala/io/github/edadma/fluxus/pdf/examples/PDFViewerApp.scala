package io.github.edadma.fluxus.pdf.examples
import io.github.edadma.fluxus.*
import io.github.edadma.fluxus.pdf.*
import org.scalajs.dom

object PDFViewerApp {
  def App: FluxusNode = {
    // State for tracking the current PDF URL
    val (pdfUrl, setPdfUrl, _) =
      useState("https://raw.githubusercontent.com/mozilla/pdf.js/master/web/compressed.tracemonkey-pldi-09.pdf")
    val (inputUrl, setInputUrl, _) = useState(pdfUrl)

    div(
      cls := "min-h-screen bg-base-200 p-4",
      div(
        cls := "container mx-auto",

        // Header
        div(
          cls := "mb-4 text-center",
          h1(cls := "text-2xl font-bold", "Fluxus PDF Viewer Example"),
          p(cls  := "text-gray-500", "Using PDF.js from NPM"),
        ),

        // URL input
        div(
          cls := "flex gap-2 mb-4",
          input(
            cls         := "input input-bordered flex-grow",
            typ         := "text",
            value_      := inputUrl,
            placeholder := "Enter PDF URL",
            onInput     := ((e: dom.Event) => setInputUrl(e.target.asInstanceOf[dom.html.Input].value)),
          ),
          button(
            cls     := "btn btn-primary",
            onClick := (() => setPdfUrl(inputUrl)),
            "Load PDF",
          ),
        ),

        // PDF Viewer with custom styling
        div(
          cls := "card bg-base-100 shadow-xl",
          div(
            cls := "card-body",
            // Add custom CSS for the PDF viewer
            style := """
              .pdf-viewer-container {
                display: flex;
                flex-direction: column;
                align-items: center;
              }

              .pdf-viewer-controls {
                display: flex;
                gap: 10px;
                margin-bottom: 10px;
                align-items: center;
              }

              .pdf-btn {
                @apply btn btn-sm;
              }

              .pdf-canvas {
                border: 1px solid #ddd;
                box-shadow: 0 2px 5px rgba(0,0,0,0.2);
              }

              .pdf-loading {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: rgba(255,255,255,0.8);
                padding: 10px 20px;
                border-radius: 4px;
                z-index: 10;
              }

              .pdf-error {
                color: red;
                margin: 20px 0;
                text-align: center;
              }
            """,

            // PDF Viewer component
            PDFViewer <> PDFViewerProps(
              url = pdfUrl,
              scale = 1.2,
              className = "w-full",
              onDocumentLoad = pdf => {
                logger.debug(
                  "PDF document loaded",
                  category = "PDFViewerApp",
                  Map("numPages" -> pdf.numPages.toString),
                )
              },
            ),
          ),
        ),

        // Instructions
        div(
          cls := "mt-4 p-4 bg-base-100 rounded-lg shadow",
          h2(cls := "text-xl font-semibold mb-2", "Instructions"),
          ul(
            cls := "list-disc ml-6",
            li("Enter a PDF URL in the input box above and click 'Load PDF'"),
            li("Use the 'Previous' and 'Next' buttons to navigate between pages"),
            li("Use the '+' and '-' buttons to zoom in and out"),
            li("This example uses PDF.js installed from NPM"),
          ),
        ),

        // Setup instructions
        div(
          cls := "mt-4 p-4 bg-yellow-100 text-yellow-800 rounded-lg shadow",
          h2(cls := "text-xl font-semibold mb-2", "Setup Instructions"),
          p("To use this component in your project:"),
          ol(
            cls := "list-decimal ml-6 mt-2",
            li("Install PDF.js: ", code("npm install pdfjs-dist")),
            li("Import the PDFViewer component"),
            li("Use it as shown in this example"),
          ),
        ),
      ),
    )
  }
}
