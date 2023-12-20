/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.clj"],
  theme: {
    extend: {
      typography: theme => ({
        DEFAULT: {
          css: {
            a: {
              color: theme('colors.blue.600')
            },
            'a:hover': {
              color: theme('colors.blue.500')
            }
          }
        },
        invert: {}
      })
    }
  },
  plugins: [
    require('@tailwindcss/typography')
  ]
}
