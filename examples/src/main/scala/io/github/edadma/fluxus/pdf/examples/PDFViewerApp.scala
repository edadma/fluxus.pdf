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

case class PDFViewerProps(url: String)

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

def PDFViewer(props: PDFViewerProps): FluxusNode = {
  // Create a reference to our canvas element
  val canvasRef                    = useRef[dom.html.Canvas]()
  val (isLoading, setIsLoading, _) = useState(true)
  val (error, setError, _)         = useState(Option.empty[String])

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

                  // Render to canvas
                  if (canvasRef.current != null) {
                    val canvas  = canvasRef.current
                    val context = canvas.getContext("2d")

                    val scale    = 1
                    val viewport = page.getViewport(js.Dynamic.literal(scale = scale))

                    // Set canvas dimensions
                    canvas.width = viewport.width.asInstanceOf[Int]
                    canvas.height = viewport.height.asInstanceOf[Int]

                    logger.debug(
                      "Canvas dimensions set",
                      category = "PDFViewer",
                      Map("width" -> canvas.width.toString, "height" -> canvas.height.toString),
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

  // Render the PDF viewer UI
  div(
    cls := "pdf-viewer-container w-full",

    // Error display
    error.map(msg =>
      div(
        cls := "error bg-red-100 text-red-700 p-4 rounded mb-4",
        div(cls := "font-bold", "Error loading PDF:"),
        div(msg),
      ),
    ),

    // Always keep the canvas in the DOM, but overlay a loading indicator
    div(
      cls := "canvas-container relative border border-gray-300 bg-white",
      // The canvas
      createElement(
        "canvas",
        ref := canvasRef,
        cls := "pdf-canvas block w-full",
        // No inline style here to keep it simple
      ),

      // Conditional loading overlay
      if (isLoading)
        div(
          cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
          div(cls := "bg-white p-3 rounded shadow", "Loading PDF..."),
        )
      else null,
    ),
  )
}

//def PDFViewer(props: PDFViewerProps): FluxusNode = {
//  // Create a reference to our canvas element
//  val canvasRef                    = useRef[dom.html.Canvas]()
//  val (isLoading, setIsLoading, _) = useState(true)
//  val (error, setError, _)         = useState(Option.empty[String])
//  val (scale, setScale, _)         = useState(0.8) // Start with smaller scale
//  val (pdfPage, setPdfPage, _)     = useState(Option.empty[js.Dynamic])
//
//  // Function to render the PDF page at current scale
//  def renderPage(): Unit = {
//    pdfPage.foreach { page =>
//      if (canvasRef.current != null) {
//        val canvas  = canvasRef.current
//        val context = canvas.getContext("2d")
//
//        val viewport = page.getViewport(js.Dynamic.literal(scale = scale))
//
//        // Set canvas dimensions
//        canvas.width = viewport.width.asInstanceOf[Int]
//        canvas.height = viewport.height.asInstanceOf[Int]
//
//        // Clear the canvas
//        context.clearRect(0, 0, canvas.width, canvas.height)
//
//        // Render PDF page to the canvas
//        val renderContext = js.Dynamic.literal(
//          canvasContext = context,
//          viewport = viewport,
//        )
//
//        val renderTask = page.render(renderContext)
//        renderTask.promise.asInstanceOf[js.Promise[js.Any]].toFuture.onComplete {
//          case Success(_) =>
//            logger.debug("PDF re-rendered at scale", category = "PDFViewer", Map("scale" -> scale.toString))
//          case Failure(renderError) =>
//            logger.error("PDF rendering failed", category = "PDFViewer", Map("error" -> renderError.toString))
//        }
//      }
//    }
//  }
//
//  // Effect to re-render when scale changes
//  useEffect(
//    () => {
//      if (pdfPage.isDefined) {
//        renderPage()
//      }
//      () => {}
//    },
//    Seq(scale),
//  )
//
//  // Effect to load the PDF
//  useEffect(
//    () => {
//      // Define our load PDF function
//      def loadPDF(): Unit = {
//        setIsLoading(true)
//        setPdfPage(None)
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
//                  setPdfPage(Some(page))
//
//                  // Render immediately at current scale
//                  if (canvasRef.current != null) {
//                    val canvas  = canvasRef.current
//                    val context = canvas.getContext("2d")
//
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
//  // Render the PDF viewer UI
//  div(
//    cls := "pdf-viewer-container w-full",
//
//    // Error display
//    error.map(msg =>
//      div(
//        cls := "error bg-red-100 text-red-700 p-4 rounded mb-4",
//        div(cls := "font-bold", "Error loading PDF:"),
//        div(msg),
//      ),
//    ),
//
//    // Zoom controls
//    div(
//      cls := "zoom-controls mb-2 flex gap-2",
//      button(
//        cls     := "btn btn-sm",
//        onClick := (() => setScale(scale - 0.1)),
//        "Zoom Out",
//      ),
//      span(cls := "self-center", f"${scale * 100}%.0f%%"),
//      button(
//        cls     := "btn btn-sm",
//        onClick := (() => setScale(scale + 0.1)),
//        "Zoom In",
//      ),
//      button(
//        cls     := "btn btn-sm ml-auto",
//        onClick := (() => setScale(0.8)),
//        "Reset",
//      ),
//    ),
//
//    // Always keep the canvas in the DOM, but overlay a loading indicator
//    div(
//      cls := "canvas-container relative border border-gray-300 bg-white",
//      // The canvas
//      createElement(
//        "canvas",
//        ref := canvasRef,
//        cls := "pdf-canvas block mx-auto", // Center the canvas
//      ),
//
//      // Conditional loading overlay
//      if (isLoading)
//        div(
//          cls := "absolute inset-0 flex items-center justify-center bg-black bg-opacity-30",
//          div(cls := "bg-white p-3 rounded shadow", "Loading PDF..."),
//        )
//      else null,
//    ),
//  )
//}
