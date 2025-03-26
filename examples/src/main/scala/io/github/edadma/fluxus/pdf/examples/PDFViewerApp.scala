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
          ),
        ),
      ),
    )
  }
}

case class PDFViewerProps(
    url: String,
    className: String = "",          // Optional additional classes
    maxHeight: Option[String] = None, // Optional max height constraint
)

def PDFViewer(props: PDFViewerProps): FluxusNode = {
  // Create a reference to our canvas element
  val canvasRef                                  = useRef[dom.html.Canvas]()
  val (isLoading, setIsLoading, _)               = useState(true)
  val (error, setError, _)                       = useState(Option.empty[String])
  val (zoomScale, setZoomScale, updateZoomScale) = useState(0.5) // Default 100% zoom

  // Effect to load and render the PDF
  useEffect(
    () => {
      // Define our load PDF function
      def loadPDF(): Unit = {
        setIsLoading(true)

        try {
          // Set the worker source
          PDF.setWorkerSrc("./node_modules/pdfjs-dist/build/pdf.worker.min.js")

          logger.debug("Starting PDF load process", category = "PDFViewer", Map("url" -> props.url))

          // Load the PDF document using our facade
          val documentTask = PDF.getDocument(PDFSource.Url(props.url))

          documentTask.toFuture.onComplete {
            case Success(pdfDoc) =>
              logger.debug("PDF document loaded", category = "PDFViewer", Map("numPages" -> pdfDoc.numPages.toString))

              // Get the first page
              pdfDoc.getPage(1).onComplete {
                case Success(page) =>
                  logger.debug("PDF page loaded", category = "PDFViewer")

                  // Render to canvas with improved quality
                  if (canvasRef.current != null) {
                    val canvas  = canvasRef.current
                    val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

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
                      case Success(_) =>
                        logger.debug("PDF rendering completed successfully", category = "PDFViewer")
                        setIsLoading(false)
                      case Failure(renderError) =>
                        logger.error(
                          "PDF rendering failed",
                          category = "PDFViewer",
                          Map("error" -> renderError.toString),
                        )
                        setError(Some(s"Render error: ${renderError.getMessage}"))
                        setIsLoading(false)
                    }
                  } else {
                    setError(Some("Canvas element not available"))
                    setIsLoading(false)
                  }
                case Failure(pageError) =>
                  logger.error("Failed to load PDF page", category = "PDFViewer", Map("error" -> pageError.toString))
                  setError(Some(s"Page error: ${pageError.getMessage}"))
                  setIsLoading(false)
              }
            case Failure(pdfError) =>
              logger.error("Failed to load PDF document", category = "PDFViewer", Map("error" -> pdfError.toString))
              setError(Some(s"Document error: ${pdfError.getMessage}"))
              setIsLoading(false)
          }
        } catch {
          case e: Throwable =>
            logger.error("Exception during PDF loading", category = "PDFViewer", Map("error" -> e.toString))
            setError(Some(s"Loading error: ${e.getMessage}"))
            setIsLoading(false)
        }
      }

      // Start loading right away
      loadPDF()

      // Cleanup function
      () => {}
    },
    Seq(props.url, zoomScale), // Re-run if the URL or zoom changes
  )

  // Render the PDF viewer UI with simplified structure
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
    canvas(
      ref := canvasRef,
      cls := "block", // No width/height classes - we size programmatically
    ),

    // Conditional loading overlay (absolutely positioned)
    if (isLoading)
      div(
        cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
        div(cls := "bg-blue-500 p-3 rounded shadow text-white", "Loading PDF..."),
      )
    else null,
  )
}
