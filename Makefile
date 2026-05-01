tailwind:
	npx @tailwindcss/cli -i ./src/main.css -o ./resources/public/styles.css --watch

resources/public/styles.css:
	npx @tailwindcss/cli -i ./src/main.css -o ./resources/public/styles.css

.PHONY: tailwind
