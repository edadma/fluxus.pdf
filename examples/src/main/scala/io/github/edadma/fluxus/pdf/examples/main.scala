package io.github.edadma.fluxus.pdf.examples

import io.github.edadma.fluxus.{logger, render}
import io.github.edadma.logger.LogLevel

@main def run(): Unit =
  logger.setLogLevel(LogLevel.DEBUG)
  render(PDFViewerApp.App, "app")
