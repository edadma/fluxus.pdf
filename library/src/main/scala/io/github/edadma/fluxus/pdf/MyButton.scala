package io.github.edadma.fluxus.pdf

import io.github.edadma.fluxus.*
import org.scalajs.dom

case class MyButtonProps(color: String, text: String, onClick: dom.MouseEvent => Unit)

def MyButton: MyButtonProps => FluxusNode =
  case MyButtonProps(color, text, onClick) =>
    button(
      cls := s"btn $color w-full",
      text,
      "onClick" := onClick,
    )
