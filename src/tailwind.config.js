/** @type {import('tailwindcss').Config} */
module.exports = {
  theme: {
    extend: {
      typography: {
        DEFAULT: {
          css: {
            a: {
              color: 'var(--color-blue-600)'
            },
            'a:hover': {
              color: 'var(--color-blue-500)'
            }
          }
        },
        invert: {}
      }
    }
  },
}
