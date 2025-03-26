package io.github.edadma.fluxus.pdf.examples

import io.github.edadma.fluxus._
import io.github.edadma.pdfjs._
import org.scalajs.dom

import scala.concurrent.Future
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import scala.util.{Failure, Success}

object PDFViewerApp {
  def App: FluxusNode = {
    div(
      cls := "container mx-auto p-4",
      div(
        cls := "card bg-base-300 shadow-xl",
        div(
          cls := "card-body",
          h2(cls := "card-title", "PDF.js Viewer"),
          p("Simple PDF viewer using Fluxus and PDF.js"),
          PDFViewer <> PDFViewerProps(
            url = "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
            // "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            // "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
          ),
        ),
      ),
    )
  }
}

case class PDFViewerProps(
    url: String,
    initialPage: Int = 1,            // Default to first page
    className: String = "",          // Optional additional classes
    maxHeight: Option[String] = None, // Optional max height constraint
)

def PDFViewer(props: PDFViewerProps): FluxusNode = {
  // Create a reference to our canvas element
  val canvasRef = useRef[dom.html.Canvas]()

  // State for loading and errors
  val (isLoading, setIsLoading, _) = useState(true)
  val (error, setError, _)         = useState(Option.empty[String])

  // State for PDF document and pagination
  val (pdfDocument, setPdfDocument, _)           = useState[Option[PDFDocument]](None)
  val (currentPage, setCurrentPage, _)           = useState(props.initialPage)
  val (totalPages, setTotalPages, _)             = useState(0)
  val (zoomScale, setZoomScale, updateZoomScale) = useState(0.5) // Default 50% zoom

  // Effect to load the PDF document
  useEffect(
    () => {
      setIsLoading(true)
      setError(None)

      try {
        // Set the worker source
        PDF.setWorkerSrc("./node_modules/pdfjs-dist/build/pdf.worker.min.js")

        logger.debug("Starting PDF load process", category = "PDFViewer", Map("url" -> props.url))

        // Load the PDF document using our facade
        val documentTask = PDF.getDocument(PDFSource.Url(props.url))

        documentTask.toFuture.onComplete {
          case Success(doc) => {
            logger.debug("PDF document loaded", category = "PDFViewer", Map("numPages" -> doc.numPages.toString))
            setPdfDocument(Some(doc))
            setTotalPages(doc.numPages)
            setIsLoading(false)
          }
          case Failure(pdfError) => {
            logger.error("Failed to load PDF document", category = "PDFViewer", Map("error" -> pdfError.toString))
            setError(Some(s"Document error: ${pdfError.getMessage}"))
            setIsLoading(false)
          }
        }
      } catch {
        case e: Throwable => {
          logger.error("Exception during PDF loading", category = "PDFViewer", Map("error" -> e.toString))
          setError(Some(s"Loading error: ${e.getMessage}"))
          setIsLoading(false)
        }
      }

      // Cleanup function
      () => {}
    },
    Seq(props.url), // Only re-run if the URL changes
  )

  // Effect to render the current page
  useEffect(
    () => {
      if (pdfDocument.isEmpty) {
        // Just return an empty cleanup function
        () => {}
      } else {
        setIsLoading(true)

        // Make sure the current page is valid
        val pageToRender = Math.max(1, Math.min(currentPage, totalPages))
        if (pageToRender != currentPage) {
          setCurrentPage(pageToRender)
        }

        // Get and render the current page
        pdfDocument.get.getPage(pageToRender).onComplete {
          case Success(page) => {
            logger.debug("PDF page loaded", category = "PDFViewer", Map("page" -> pageToRender.toString))

            // Render to canvas
            if (canvasRef.current != null) {
              val canvas  = canvasRef.current
              val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

              // Clear previous content
              context.clearRect(0, 0, canvas.width, canvas.height)

              // Get device pixel ratio for better scaling
              val pixelRatio = Option(dom.window.devicePixelRatio).getOrElse(1.0)

              // Get container width (parent element or 800px default width)
              val containerWidth = Option(canvas.parentElement)
                .map(_.clientWidth)
                .filter(_ > 0)
                .getOrElse(800)

              // Get the default viewport at scale 1
              val defaultViewport = page.getViewport(1.0)
              val pdfWidth        = defaultViewport.width

              // Calculate base scale to fit container width
              val baseScale = containerWidth / pdfWidth

              // Apply zoom factor to get final scale
              val finalScale = baseScale * zoomScale

              // Create a viewport scaled for the display
              val viewport = page.getViewport(finalScale * pixelRatio)

              // Set canvas dimensions to the scaled size (internal resolution)
              canvas.width = viewport.width.toInt
              canvas.height = viewport.height.toInt

              // Calculate display dimensions in CSS pixels
              val displayWidth  = Math.round(viewport.width / pixelRatio)
              val displayHeight = Math.round(viewport.height / pixelRatio)

              // Set display size (CSS pixels)
              canvas.style.width = s"${displayWidth}px"
              canvas.style.height = s"${displayHeight}px"

              // Make sure our outer container has the right sizing
              Option(canvas.parentElement).foreach { container =>
                container.style.position = "relative"
                container.style.width = s"${displayWidth}px"
                // Apply max height if specified, otherwise use actual height
                props.maxHeight match {
                  case Some(maxH) => container.style.maxHeight = maxH
                  case None       => container.style.height = s"${displayHeight}px"
                }
              }

              logger.debug(
                "Canvas dimensions set",
                category = "PDFViewer",
                Map(
                  "internalWidth"  -> canvas.width.toString,
                  "internalHeight" -> canvas.height.toString,
                  "displayWidth"   -> displayWidth.toString,
                  "displayHeight"  -> displayHeight.toString,
                  "pixelRatio"     -> pixelRatio.toString,
                  "zoomScale"      -> zoomScale.toString,
                ),
              )

              // Render PDF page to the canvas
              val renderTask = page.render(context, viewport)

              renderTask.promise.toFuture.onComplete {
                case Success(_) => {
                  logger.debug("PDF rendering completed successfully", category = "PDFViewer")
                  setIsLoading(false)
                }
                case Failure(renderError) => {
                  logger.error(
                    "PDF rendering failed",
                    category = "PDFViewer",
                    Map("error" -> renderError.toString),
                  )
                  setError(Some(s"Render error: ${renderError.getMessage}"))
                  setIsLoading(false)
                }
              }
            } else {
              setError(Some("Canvas element not available"))
              setIsLoading(false)
            }
          }
          case Failure(pageError) => {
            logger.error("Failed to load PDF page", category = "PDFViewer", Map("error" -> pageError.toString))
            setError(Some(s"Page error: ${pageError.getMessage}"))
            setIsLoading(false)
          }
        }

        // Cleanup function
        () => {}
      }
    },
    Seq(pdfDocument, currentPage, zoomScale), // Re-run when document, page, or zoom changes
  )

  // Page navigation functions
  def goToNextPage(): Unit = {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1)
    }
  }

  def goToPreviousPage(): Unit = {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1)
    }
  }

  def goToPage(pageNum: Int): Unit = {
    val targetPage = Math.max(1, Math.min(pageNum, totalPages))
    setCurrentPage(targetPage)
  }

  // Render the PDF viewer UI
  div(
    cls := s"bg-white border border-gray-300 ${props.className}".trim,

    // Error display
    error.map(msg =>
      div(
        cls := "bg-red-100 text-red-700 p-4 rounded mb-4",
        div(cls := "font-bold", "Error loading PDF:"),
        div(msg),
      ),
    ),

    // The canvas (sized programmatically)
    div(
      cls := "relative", // For positioning the loading overlay
      canvas(
        ref := canvasRef,
        cls := "block", // No width/height classes - we size programmatically
      ),

      // Conditional loading overlay
      if (isLoading)
        div(
          cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
          div(cls := "bg-blue-500 p-3 rounded shadow text-white", "Loading PDF..."),
        )
      else null,
    ),

    // Pagination controls - only show if we have pages
    if (totalPages > 0)
      div(
        cls := "flex items-center justify-between mt-4 px-2 py-2 bg-gray-100 rounded",

        // Previous page button
        button(
          cls := "px-3 py-1 rounded " +
            (if (currentPage > 1) "bg-blue-500 text-white hover:bg-blue-600"
             else "bg-gray-300 text-gray-500 cursor-not-allowed"),
          disabled := (currentPage <= 1),
          onClick  := (() => goToPreviousPage()),
          "Previous",
        ),

        // Current page / total pages
        div(
          cls := "flex items-center",
          span(cls := "text-gray-700 mr-2", "Page"),

          // Page input field
          input(
            typ   := "number",
            cls   := "w-14 px-2 py-1 border rounded text-center",
            min   := "1",
            max   := totalPages.toString,
            value := currentPage.toString,
            onInput := ((e: dom.Event) => {
              val input   = e.target.asInstanceOf[dom.html.Input]
              val pageNum = input.value.toIntOption.getOrElse(currentPage)
              if (pageNum >= 1 && pageNum <= totalPages) {
                goToPage(pageNum)
              }
            }),
          ),
          span(cls := "text-gray-700 ml-2", s"of $totalPages"),
        ),

        // Next page button
        button(
          cls := "px-3 py-1 rounded " +
            (if (currentPage < totalPages) "bg-blue-500 text-white hover:bg-blue-600"
             else "bg-gray-300 text-gray-500 cursor-not-allowed"),
          disabled := (currentPage >= totalPages),
          onClick  := (() => goToNextPage()),
          "Next",
        ),
      )
    else null,
  )
}
