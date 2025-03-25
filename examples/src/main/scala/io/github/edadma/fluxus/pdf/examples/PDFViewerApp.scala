package io.github.edadma.fluxus.pdf.examples

import io.github.edadma.fluxus._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}
import scala.concurrent.Future
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import scala.util.{Failure, Success}

// Define PDF.js from NPM package
@js.native
@JSImport("pdfjs-dist", JSImport.Namespace)
object PDFjs extends js.Object {
  val getDocument: js.Function1[js.Any, PDFDocumentLoadingTask] = js.native

  @js.native
  trait GlobalWorkerOptions extends js.Object {
    var workerSrc: String = js.native
  }

  val GlobalWorkerOptions: GlobalWorkerOptions = js.native
}

// Define necessary PDF.js types
@js.native
trait PDFDocumentLoadingTask extends js.Object {
  val promise: js.Promise[PDFDocumentProxy] = js.native
}

@js.native
trait PDFDocumentProxy extends js.Object {
  def getPage(pageNumber: Int): js.Promise[PDFPageProxy] = js.native
  val numPages: Int                                      = js.native
}

@js.native
trait PDFPageProxy extends js.Object {
  def getViewport(options: js.Dynamic): PDFPageViewport = js.native
  def render(renderContext: js.Dynamic): PDFRenderTask  = js.native
}

@js.native
trait PDFPageViewport extends js.Object {
  val width: Double  = js.native
  val height: Double = js.native
}

@js.native
trait PDFRenderTask extends js.Object {
  val promise: js.Promise[js.Any] = js.native
}

object PDFViewerApp {
  def App: FluxusNode = {
    div(
      cls := "container mx-auto p-4",
      div(
        cls := "card bg-base-100 shadow-xl",
        div(
          cls := "card-body",
          h2(cls := "card-title", "PDF.js Viewer"),
          p("Simple PDF viewer using Fluxus and PDF.js"),
          PDFViewer <> PDFViewerProps(url =
            "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf",
          ),
        ),
      ),
    )
  }
}

//case class PDFViewerProps(
//    url: String,
//    className: String = "w-full",    // Controls the overall component width
//    maxHeight: Option[String] = None, // Optional max height constraint
//)
//
//def PDFViewer(props: PDFViewerProps): FluxusNode = {
//  // Create a reference to our canvas element
//  val canvasRef                    = useRef[dom.html.Canvas]()
//  val (isLoading, setIsLoading, _) = useState(true)
//  val (error, setError, _)         = useState(Option.empty[String])
//
//  // Effect to load and render the PDF
//  useEffect(
//    () => {
//      // Define our load PDF function
//      def loadPDF(): Unit = {
//        setIsLoading(true)
//
//        try {
//          // Set the worker source
//          PDFjs.GlobalWorkerOptions.workerSrc =
//            "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js"
//
//          logger.debug("Starting PDF load process", category = "PDFViewer", Map("url" -> props.url))
//
//          // Load the PDF document
//          val loadingTask = PDFjs.getDocument(props.url)
//
//          loadingTask.promise.asInstanceOf[js.Promise[js.Dynamic]].toFuture.onComplete {
//            case Success(pdfDoc) =>
//              logger.debug("PDF document loaded", category = "PDFViewer", Map("numPages" -> pdfDoc.numPages.toString))
//
//              // Get the first page
//              val pagePromise = pdfDoc.getPage(1).asInstanceOf[js.Promise[js.Dynamic]]
//              pagePromise.toFuture.onComplete {
//                case Success(page) =>
//                  logger.debug("PDF page loaded", category = "PDFViewer")
//
//                  // Render to canvas
//                  if (canvasRef.current != null) {
//                    val canvas  = canvasRef.current
//                    val context = canvas.getContext("2d")
//
//                    val scale    = 1
//                    val viewport = page.getViewport(js.Dynamic.literal(scale = scale))
//
//                    // Set canvas dimensions
//                    canvas.width = viewport.width.asInstanceOf[Int]
//                    canvas.height = viewport.height.asInstanceOf[Int]
//
//                    logger.debug(
//                      "Canvas dimensions set",
//                      category = "PDFViewer",
//                      Map("width" -> canvas.width.toString, "height" -> canvas.height.toString),
//                    )
//
//                    // Render PDF page to the canvas
//                    val renderContext = js.Dynamic.literal(
//                      canvasContext = context,
//                      viewport = viewport,
//                    )
//
//                    val renderTask = page.render(renderContext)
//                    renderTask.promise.asInstanceOf[js.Promise[js.Any]].toFuture.onComplete {
//                      case Success(_) =>
//                        logger.debug("PDF rendering completed successfully", category = "PDFViewer")
//                        setIsLoading(false)
//                      case Failure(renderError) =>
//                        logger.error(
//                          "PDF rendering failed",
//                          category = "PDFViewer",
//                          Map("error" -> renderError.toString),
//                        )
//                        setError(Some(s"Render error: ${renderError.getMessage}"))
//                        setIsLoading(false)
//                    }
//                  } else {
//                    setError(Some("Canvas element not available"))
//                    setIsLoading(false)
//                  }
//                case Failure(pageError) =>
//                  logger.error("Failed to load PDF page", category = "PDFViewer", Map("error" -> pageError.toString))
//                  setError(Some(s"Page error: ${pageError.getMessage}"))
//                  setIsLoading(false)
//              }
//            case Failure(pdfError) =>
//              logger.error("Failed to load PDF document", category = "PDFViewer", Map("error" -> pdfError.toString))
//              setError(Some(s"Document error: ${pdfError.getMessage}"))
//              setIsLoading(false)
//          }
//        } catch {
//          case e: Throwable =>
//            logger.error("Exception during PDF loading", category = "PDFViewer", Map("error" -> e.toString))
//            setError(Some(s"Loading error: ${e.getMessage}"))
//            setIsLoading(false)
//        }
//      }
//
//      // Start loading right away
//      loadPDF()
//
//      // Cleanup function
//      () => {}
//    },
//    Seq(props.url), // Re-run if the URL changes
//  )
//
//  // Style for max height if provided
//  val maxHeightStyle = props.maxHeight.map(h => s"max-height: $h;").getOrElse("")
//
//  // Render the PDF viewer UI
//  div(
//    cls := props.className,
//
//    // Error display
//    error.map(msg =>
//      div(
//        cls := "bg-red-100 text-red-700 p-4 rounded mb-4",
//        div(cls := "font-bold", "Error loading PDF:"),
//        div(msg),
//      ),
//    ),
//
//    // Always keep the canvas in the DOM, but overlay a loading indicator
//    div(
//      cls   := "relative border border-gray-300 bg-white",
//      style := maxHeightStyle, // Apply max height if provided
//
//      // The canvas - will maintain PDF aspect ratio
//      canvas(
//        ref := canvasRef,
//        cls := "block w-full", // Full width of container to maintain aspect ratio
//      ),
//
//      // Conditional loading overlay
//      if (isLoading)
//        div(
//          cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
//          div(cls := "bg-blue-500 p-3 rounded shadow text-white", "Loading PDF..."),
//        )
//      else null,
//    ),
//  )
//}

case class PDFViewerProps(
    url: String,
    className: String = "w-full",    // Controls the overall component width
    maxHeight: Option[String] = None, // Optional max height constraint
)

def PDFViewer(props: PDFViewerProps): FluxusNode = {
  // Create a reference to our canvas element
  val canvasRef                       = useRef[dom.html.Canvas]()
  val containerRef                    = useRef[dom.html.Div]()
  val (isLoading, setIsLoading, _)    = useState(true)
  val (error, setError, _)            = useState(Option.empty[String])
  val (zoomScale, _, updateZoomScale) = useState(.5) // Default 100% zoom

  // Effect to load and render the PDF
  useEffect(
    () => {
      // Define our load PDF function
      def loadPDF(): Unit = {
        setIsLoading(true)

        try {
          // Set the worker source
          PDFjs.GlobalWorkerOptions.workerSrc =
            "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js"

          logger.debug("Starting PDF load process", category = "PDFViewer", Map("url" -> props.url))

          // Load the PDF document
          val loadingTask = PDFjs.getDocument(props.url)

          loadingTask.promise.asInstanceOf[js.Promise[js.Dynamic]].toFuture.onComplete {
            case Success(pdfDoc) =>
              logger.debug("PDF document loaded", category = "PDFViewer", Map("numPages" -> pdfDoc.numPages.toString))

              // Get the first page
              val pagePromise = pdfDoc.getPage(1).asInstanceOf[js.Promise[js.Dynamic]]
              pagePromise.toFuture.onComplete {
                case Success(page) =>
                  logger.debug("PDF page loaded", category = "PDFViewer")

                  // Render to canvas with improved quality
                  if (canvasRef.current != null && containerRef.current != null) {
                    val canvas  = canvasRef.current
                    val context = canvas.getContext("2d")

                    // Get device pixel ratio for better scaling
                    val pixelRatio = Option(dom.window.devicePixelRatio).getOrElse(1.0)

                    // Get container width
                    val containerWidth = containerRef.current.clientWidth

                    // Get the default viewport at scale 1
                    val defaultViewport = page.getViewport(js.Dynamic.literal(scale = 1.0))
                    val pdfWidth        = defaultViewport.width.asInstanceOf[Double]

                    // Get base scale to fit container width
                    val baseScale = containerWidth / pdfWidth

                    // Apply zoom factor to the base scale
                    val scale = baseScale * zoomScale

                    // Create a viewport scaled for the display
                    val viewport = page.getViewport(js.Dynamic.literal(
                      scale = scale * pixelRatio, // Scale up for device pixel ratio
                    ))

                    // Set canvas dimensions to the scaled size (internal resolution)
                    canvas.width = viewport.width.asInstanceOf[Int]
                    canvas.height = viewport.height.asInstanceOf[Int]

                    // Set display size (CSS pixels)
                    canvas.style.width = s"${Math.round(viewport.width.asInstanceOf[Double] / pixelRatio)}px"
                    canvas.style.height = s"${Math.round(viewport.height.asInstanceOf[Double] / pixelRatio)}px"

                    logger.debug(
                      "Canvas dimensions set",
                      category = "PDFViewer",
                      Map(
                        "internalWidth"  -> canvas.width.toString,
                        "internalHeight" -> canvas.height.toString,
                        "displayWidth"   -> canvas.style.width,
                        "displayHeight"  -> canvas.style.height,
                        "pixelRatio"     -> pixelRatio.toString,
                      ),
                    )

                    // Render PDF page to the canvas
                    val renderContext = js.Dynamic.literal(
                      canvasContext = context,
                      viewport = viewport,
                    )

                    val renderTask = page.render(renderContext)
                    renderTask.promise.asInstanceOf[js.Promise[js.Any]].toFuture.onComplete {
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
    Seq(props.url), // Re-run if the URL changes
  )

  // Style for max height if provided
  val maxHeightStyle = props.maxHeight.map(h => s"max-height: $h;").getOrElse("")

  // Render the PDF viewer UI
  div(
    cls := props.className,

    // Error display
    error.map(msg =>
      div(
        cls := "bg-red-100 text-red-700 p-4 rounded mb-4",
        div(cls := "font-bold", "Error loading PDF:"),
        div(msg),
      ),
    ),

    // Always keep the canvas in the DOM, but overlay a loading indicator
    div(
      ref   := containerRef,
      cls   := "relative border border-gray-300 bg-white",
      style := maxHeightStyle, // Apply max height if provided

      // The canvas
      canvas(
        ref := canvasRef,
        cls := "block mx-auto bg-white", // No width/height classes - we'll set the size programmatically
      ),

      // Conditional loading overlay
      if (isLoading)
        div(
          cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
          div(cls := "bg-blue-500 p-3 rounded shadow text-white", "Loading PDF..."),
        )
      else null,
    ),
  )
}
