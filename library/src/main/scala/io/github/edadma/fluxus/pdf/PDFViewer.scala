package io.github.edadma.fluxus.pdf

import io.github.edadma.fluxus.*
import org.scalajs.dom
import org.scalajs.dom.{HTMLCanvasElement, Element}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}
import scala.util.{Try, Success, Failure}

// PDF.js imports from npm package
@js.native
@JSImport("pdfjs-dist", JSImport.Default)
object PDFjsLib extends js.Object {
  val GlobalWorkerOptions: GlobalWorkerOptions                = js.native
  def getDocument(source: js.Dynamic): PDFDocumentLoadingTask = js.native
}

@js.native
trait GlobalWorkerOptions extends js.Object {
  var workerSrc: String = js.native
}

@js.native
trait PDFDocumentLoadingTask extends js.Object {
  def promise: js.Promise[PDFDocumentProxy] = js.native
}

@js.native
trait PDFDocumentProxy extends js.Object {
  def numPages: Int                                      = js.native
  def getPage(pageNumber: Int): js.Promise[PDFPageProxy] = js.native
}

@js.native
trait PDFPageProxy extends js.Object {
  def getViewport(options: js.Dynamic): PDFPageViewport = js.native
  def render(renderContext: js.Dynamic): PDFRenderTask  = js.native
}

@js.native
trait PDFPageViewport extends js.Object {
  def width: Double  = js.native
  def height: Double = js.native
}

@js.native
trait PDFRenderTask extends js.Object {
  def promise: js.Promise[Unit] = js.native
}

// Optional worker import - you'll need to initialize this
@js.native
@JSImport("pdfjs-dist/build/pdf.worker.entry", JSImport.Default)
object PDFWorker extends js.Any

// Component Props
case class PDFViewerProps(
    url: String,
    scale: Double = 1.0,
    pageNumber: Int = 1,
    onDocumentLoad: PDFDocumentProxy => Unit = _ => (),
    className: String = "pdf-viewer",
)

// Component initialization function
def initPDFjs(): Unit = {
  // Set the worker source
  PDFjsLib.GlobalWorkerOptions.workerSrc = "pdfjs-dist/build/pdf.worker.js"
}

// Component
def PDFViewer(props: PDFViewerProps): FluxusNode = {
  // State for tracking current page and total pages
  val (currentPage, setCurrentPage, _) = useState(props.pageNumber)
  val (totalPages, setTotalPages, _)   = useState(0)
  val (scale, setScale, _)             = useState(props.scale)
  val (isLoading, setIsLoading, _)     = useState(true)
  val (error, setError, _)             = useState(Option.empty[String])

  // Refs
  val canvasRef = useRef[HTMLCanvasElement]()
  val pdfDocRef = useRef[PDFDocumentProxy]()

  // Initialize PDF.js on first render
  useEffect(
    () => {
      // This is a one-time initialization
      try {
        initPDFjs()
      } catch {
        case e: Throwable =>
          logger.error(
            "Failed to initialize PDF.js",
            category = "PDFViewer",
            Map("error" -> e.toString),
          )
      }

      () => () // No cleanup needed
    },
    Seq(), // Empty deps array means it runs once on mount
  )

  // Load PDF document
  useEffect(
    () => {
      setIsLoading(true)
      setError(None)

      try {
        val loadingTask = PDFjsLib.getDocument(js.Dynamic.literal(
          url = props.url,
        ))

        // Define the Promise handlers properly
        val onSuccess = (pdf: PDFDocumentProxy) => {
          pdfDocRef.current = pdf
          setTotalPages(pdf.numPages)
          setCurrentPage(Math.min(props.pageNumber, pdf.numPages))
          props.onDocumentLoad(pdf)
          renderPage(currentPage, scale)
          setIsLoading(false)
        }

        val onError = (err: Any) => {
          logger.error(
            "Failed to load PDF",
            category = "PDFViewer",
            Map("error" -> err.toString, "url" -> props.url),
          )
          setError(Some(s"Failed to load PDF: ${err.toString}"))
          setIsLoading(false)
        }

        // Attach handlers to the promise
        loadingTask.promise
          .`then`[Unit](onSuccess)
          .`catch`[Unit](onError)
      } catch {
        case e: Throwable =>
          logger.error(
            "Error in PDF loading effect",
            category = "PDFViewer",
            Map("error" -> e.toString),
          )
          setError(Some(s"Error loading PDF: ${e.toString}"))
          setIsLoading(false)
      }

      // Cleanup function
      () => {
        // Any cleanup needed for the PDF document
        pdfDocRef.current = null
      }
    },
    Seq(props.url), // Only reload when URL changes
  )

  // Render page when page number or scale changes
  useEffect(
    () => {
      if (!isLoading && pdfDocRef.current != null) {
        renderPage(currentPage, scale)
      }
    },
    Seq(currentPage, scale, isLoading),
  )

  // Function to render a specific page
  def renderPage(pageNum: Int, pageScale: Double): Unit = {
    if (pdfDocRef.current == null || canvasRef.current == null) return

    setIsLoading(true)

    val onPageLoaded = (page: PDFPageProxy) => {
      val viewport = page.getViewport(js.Dynamic.literal(
        scale = pageScale,
      ))

      val canvas  = canvasRef.current
      val context = canvas.getContext("2d")

      canvas.height = viewport.height.toInt
      canvas.width = viewport.width.toInt

      val renderContext = js.Dynamic.literal(
        canvasContext = context,
        viewport = viewport,
      )

      // Define render success/error handlers
      val onRenderSuccess = (_: Any) => {
        setIsLoading(false)
      }

      val onRenderError = (err: Any) => {
        logger.error(
          "Failed to render page",
          category = "PDFViewer",
          Map("error" -> err.toString, "page" -> pageNum.toString),
        )
        setError(Some(s"Failed to render page $pageNum: ${err.toString}"))
        setIsLoading(false)
      }

      // Render the page
      page.render(renderContext).promise
        .`then`[Unit](onRenderSuccess)
        .`catch`[Unit](onRenderError)
    }

    // Get the page and handle any errors
    pdfDocRef.current.getPage(pageNum)
      .`then`[Unit](onPageLoaded)
      .`catch`[Unit]((err: Any) => {
        logger.error(
          "Failed to get page",
          category = "PDFViewer",
          Map("error" -> err.toString, "page" -> pageNum.toString),
        )
        setError(Some(s"Failed to get page $pageNum: ${err.toString}"))
        setIsLoading(false)
      })
  }

  // Navigation functions
  def goToPrevPage(): Unit = {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1)
    }
  }

  def goToNextPage(): Unit = {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1)
    }
  }

  def zoomIn(): Unit = {
    setScale(scale * 1.2)
  }

  def zoomOut(): Unit = {
    setScale(scale / 1.2)
  }

  // Render the component
  div(
    cls := props.className,
    div(
      cls := "pdf-viewer-container",

      // Controls
      div(
        cls := "pdf-viewer-controls",
        button(
          cls      := "pdf-btn prev-btn",
          onClick  := (() => goToPrevPage()),
          disabled := (currentPage <= 1),
          "Previous",
        ),
        span(
          cls := "pdf-page-info",
          s"Page $currentPage of $totalPages",
        ),
        button(
          cls      := "pdf-btn next-btn",
          onClick  := (() => goToNextPage()),
          disabled := (currentPage >= totalPages),
          "Next",
        ),
        button(
          cls     := "pdf-btn zoom-in-btn",
          onClick := (() => zoomIn()),
          "+",
        ),
        span(
          cls := "pdf-scale-info",
          f"${scale * 100}%.0f%%",
        ),
        button(
          cls     := "pdf-btn zoom-out-btn",
          onClick := (() => zoomOut()),
          "-",
        ),
      ),

      // Loading indicator
      if (isLoading) {
        div(
          cls := "pdf-loading",
          "Loading...",
        )
      } else null,

      // Error message
      error.map(err =>
        div(
          cls := "pdf-error",
          err,
        ),
      ),

      // PDF canvas
      createElement("canvas", "ref" := canvasRef, cls := "pdf-canvas"),
    ),
  )
}
