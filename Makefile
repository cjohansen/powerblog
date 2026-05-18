.PHONY: tailwind
tailwind:
	npx @tailwindcss/cli -i ./src/main.css -o ./resources/public/styles.css --watch

resources/public/styles.css:
	npx @tailwindcss/cli -i ./src/main.css -o ./resources/public/styles.css -m

.PHONY: clean
clean:
	rm -rf target resources/public/styles.css

.PHONY: export
export: clean resources/public/styles.css
	clojure -M:export
