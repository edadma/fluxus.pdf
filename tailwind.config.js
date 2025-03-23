module.exports = {
  // Specify the paths to all of your template files
  content: [
    './examples/target/scala-3.6.4/examples-fastopt/**/*.js', // Your Scala.js source files
    './index.html',         // Your HTML files
  ],
  daisyui: {
      themes: ["light", "dark", "night", "cupcake"],
  },
  // Extend the default Tailwind CSS configuration
  theme: {
    extend: {
      fontFamily: {
      }
    },
  },
  // Add DaisyUI as a plugin
  plugins: [require("@tailwindcss/typography"), require('daisyui')],
};
