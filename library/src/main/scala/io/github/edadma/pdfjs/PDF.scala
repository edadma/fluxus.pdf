package io.github.edadma.pdfjs

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

/** Provides a type-safe Scala.js facade for the PDF.js library.
  *
  * This package allows Scala.js applications to interact with PDF documents using Mozilla's PDF.js library in a
  * type-safe manner.
  *
  * Example usage:
  * {{{
  * import io.github.edadma.pdfjs._
  *
  * // Set worker src
  * PDF.setWorkerSrc("./pdf.worker.min.js")
  *
  * // Load a PDF
  * val documentTask = PDF.getDocument(PDFSource.Url("example.pdf"))
  *
  * // Process the document
  * documentTask.toFuture.foreach { document =>
  *   // Get the first page
  *   document.getPage(1).foreach { page =>
  *     // Render the page to a canvas
  *     val viewport = page.getViewport(1.0)
  *     val canvas = document.getElementById("canvas").asInstanceOf[dom.html.Canvas]
  *     val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  *
  *     page.render(context, viewport)
  *   }
  * }
  * }}}
  */
// Type aliases for commonly used types
type PDFRenderContext = org.scalajs.dom.CanvasRenderingContext2D

// Main entry point for PDF.js
@js.native
@JSImport("pdfjs-dist", JSImport.Namespace)
object PDFJSLib extends js.Object {
  def getDocument(source: js.Any): PDFDocumentLoadingTask = js.native

  @js.native
  trait GlobalWorkerOptions extends js.Object {
    var workerSrc: String = js.native
  }

  val GlobalWorkerOptions: GlobalWorkerOptions = js.native
}

// Document loading and management
@js.native
trait PDFDocumentLoadingTask extends js.Object {
  val promise: js.Promise[PDFDocumentProxy]           = js.native
  var onProgress: js.Function1[PDFProgressData, Unit] = js.native
  def cancel(): Unit                                  = js.native
}

@js.native
trait PDFDocumentProxy extends js.Object {
  def getPage(pageNumber: Int): js.Promise[PDFPageProxy] = js.native
  val numPages: Int                                      = js.native
  def getMetadata(): js.Promise[PDFMetadata]             = js.native
  def getOutline(): js.Promise[js.Array[PDFOutlineNode]] = js.native
  def cleanup(): js.Promise[Unit]                        = js.native
  def destroy(): js.Promise[Unit]                        = js.native
}

// Page-related types
@js.native
trait PDFPageProxy extends js.Object {
  val pageNumber: Int                                                           = js.native
  def getViewport(options: js.Dynamic): PDFPageViewport                         = js.native
  def render(renderContext: js.Dynamic): PDFRenderTask                          = js.native
  def getTextContent(params: js.Any = js.undefined): js.Promise[PDFTextContent] = js.native
  def getAnnotations(): js.Promise[js.Array[PDFAnnotation]]                     = js.native
  def cleanup(): js.Promise[Unit]                                               = js.native
  def destroy(): js.Promise[Unit]                                               = js.native
}

@js.native
trait PDFPageViewport extends js.Object {
  val width: Double               = js.native
  val height: Double              = js.native
  val scale: Double               = js.native
  val rotation: Int               = js.native
  val offsetX: Double             = js.native
  val offsetY: Double             = js.native
  val transform: js.Array[Double] = js.native

  def clone(params: js.Dynamic): PDFPageViewport                           = js.native
  def convertToViewportPoint(x: Double, y: Double): js.Array[Double]       = js.native
  def convertToViewportRectangle(rect: js.Array[Double]): js.Array[Double] = js.native
}

@js.native
trait PDFRenderTask extends js.Object {
  val promise: js.Promise[js.Any] = js.native
  def cancel(): Unit              = js.native
}

// Metadata and content types
@js.native
trait PDFMetadata extends js.Object {
  val info: js.Dictionary[js.Any]               = js.native
  val metadata: js.UndefOr[PDFDocumentMetadata] = js.native
}

@js.native
trait PDFDocumentMetadata extends js.Object {
  def getAll: js.Dictionary[String]         = js.native
  def get(name: String): js.UndefOr[String] = js.native
  def has(name: String): Boolean            = js.native
}

@js.native
trait PDFTextContent extends js.Object {
  val items: js.Array[PDFTextItem]        = js.native
  val styles: js.Dictionary[PDFTextStyle] = js.native
}

@js.native
trait PDFTextItem extends js.Object {
  val str: String                  = js.native
  val dir: String                  = js.native
  val transform: js.Array[Double]  = js.native
  val width: Double                = js.native
  val height: Double               = js.native
  val fontName: js.UndefOr[String] = js.native
}

@js.native
trait PDFTextStyle extends js.Object {
  val ascent: Double     = js.native
  val descent: Double    = js.native
  val vertical: Boolean  = js.native
  val fontFamily: String = js.native
}

// Outline and annotation types
@js.native
trait PDFOutlineNode extends js.Object {
  val title: String                               = js.native
  val bold: js.UndefOr[Boolean]                   = js.native
  val italic: js.UndefOr[Boolean]                 = js.native
  val color: js.UndefOr[js.Array[Double]]         = js.native
  val dest: js.Any                                = js.native
  val url: js.UndefOr[String]                     = js.native
  val items: js.UndefOr[js.Array[PDFOutlineNode]] = js.native
}

@js.native
trait PDFAnnotation extends js.Object {
  val subtype: String        = js.native
  val rect: js.Array[Double] = js.native
  val annotationType: Int    = js.native
  val id: String             = js.native
}

// Progress data
@js.native
trait PDFProgressData extends js.Object {
  val loaded: Int = js.native
  val total: Int  = js.native
}

// Source types for loading PDFs
sealed trait PDFSource {
  def toJS: js.Any
}

object PDFSource {
  case class Url(url: String) extends PDFSource {
    def toJS: js.Any = url
  }

  case class Data(data: js.typedarray.ArrayBuffer) extends PDFSource {
    def toJS: js.Any = js.Dynamic.literal(data = data)
  }

  case class Base64(data: String, contentType: String = "application/pdf") extends PDFSource {
    def toJS: js.Any = {
      val encodedData = s"data:$contentType;base64,$data"
      js.Dynamic.literal(url = encodedData)
    }
  }
}

// Helper class for easier Scala usage
class PDF private (jsLib: PDFJSLib.type) {
  // Configure worker location
  def setWorkerSrc(workerSrc: String): Unit = {
    jsLib.GlobalWorkerOptions.workerSrc = workerSrc
  }

  // Load a PDF document
  def getDocument(source: PDFSource): PDFDocumentTask = {
    val task = jsLib.getDocument(source.toJS)
    new PDFDocumentTask(task)
  }
}

object PDF {
  // Singleton instance
  private val instance = new PDF(PDFJSLib)

  // Access the singleton
  def apply(): PDF = instance

  // Configure worker source
  def setWorkerSrc(workerSrc: String): Unit = instance.setWorkerSrc(workerSrc)

  // Load a document
  def getDocument(source: PDFSource): PDFDocumentTask = instance.getDocument(source)
}

// Scala-friendly wrappers for better ergonomics
class PDFDocumentTask(val task: PDFDocumentLoadingTask) {
  // Convert to Scala Future
  def toFuture: Future[PDFDocument] = {
    val promise = Promise[PDFDocument]()

    task.promise.toFuture.onComplete {
      case Success(doc) => promise.success(new PDFDocument(doc))
      case Failure(err) => promise.failure(err)
    }

    promise.future
  }

  // Cancel loading
  def cancel(): Unit = task.cancel()
}

class PDFDocument(private val doc: PDFDocumentProxy) {
  // Expose properties
  def numPages: Int = doc.numPages

  // Get a page
  def getPage(pageNumber: Int): Future[PDFPage] = {
    doc.getPage(pageNumber).toFuture.map(page => new PDFPage(page))
  }

  // Get metadata
  def getMetadata(): Future[PDFMetadata] = doc.getMetadata().toFuture

  // Get outline
  def getOutline(): Future[js.Array[PDFOutlineNode]] = doc.getOutline().toFuture

  // Cleanup resources
  def cleanup(): Future[Unit] = doc.cleanup().toFuture

  // Destroy document
  def destroy(): Future[Unit] = doc.destroy().toFuture
}

class PDFPage(private val page: PDFPageProxy) {
  // Expose properties
  def pageNumber: Int = page.pageNumber

  // Get viewport
  def getViewport(scale: Double, rotation: Int = 0): PDFPageViewport = {
    val options = js.Dynamic.literal(scale = scale, rotation = rotation)
    page.getViewport(options)
  }

  // Render page to canvas
  def render(canvasContext: dom.CanvasRenderingContext2D, viewport: PDFPageViewport): PDFRenderTask = {
    val renderContext = js.Dynamic.literal(
      canvasContext = canvasContext,
      viewport = viewport,
    )
    page.render(renderContext)
  }

  // Get text content
  def getTextContent(): Future[PDFTextContent] = page.getTextContent().toFuture

  // Get annotations
  def getAnnotations(): Future[js.Array[PDFAnnotation]] = page.getAnnotations().toFuture

  // Cleanup resources
  def cleanup(): Future[Unit] = page.cleanup().toFuture

  // Destroy page
  def destroy(): Future[Unit] = page.destroy().toFuture
}
