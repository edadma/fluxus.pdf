package io.github.edadma.fluxus_library_template.examples

import io.github.edadma.fluxus.*
import io.github.edadma.fluxus_library_template.{MyButton, MyButtonProps}
import org.scalajs.dom.MouseEvent

object TestApp {
  def App: FluxusNode =
    val (message, setMessage, _) = useState("Welcome to the Test app!")

    div(
      cls := "min-h-screen bg-base-200 flex items-center justify-center",
      div(
        cls := "p-6 bg-base-100 rounded-lg shadow-lg text-center space-y-6",
        h1(cls := "text-2xl font-bold", message),
        div(
          cls := "space-y-2",
          MyButton <> MyButtonProps(
            color = "btn-primary",
            text = "Say Hello",
            onClick = (_: MouseEvent) => { setMessage("Hello, world!") },
          ),
          MyButton <> MyButtonProps(
            color = "btn-secondary",
            text = "Say Goodbye",
            onClick = (_: MouseEvent) => setMessage("Goodbye, world!"),
          ),
          MyButton <> MyButtonProps(
            color = "btn-accent",
            text = "Welcome to DaisyUI!",
            onClick = (_: MouseEvent) => setMessage("Welcome to DaisyUI!"),
          ),
        ),
      ),
    )
}
