# Getting started with Powerpack

This repo is a step by step tutorial on setting up a static site with
[Powerpack](https://github.com/cjohansen/powerpack), a static site toolkit for
Clojure.

The code in this repository represents the result of following the full
tutorial. You can check out the commit for each step to follow along.

You might wonder why there is no generator that can generate a new project for
you. I personally don't like wading through a bunch of generated code, and I
think generators can lead to poorer API design, and have deliberately chosen to
not make one. Instead, Powerpack aims to get up and running with minimal code.
Punching it in manually will cost you next to nothing in the grand scheme of
things, and it will give you a better understanding of your app.

## Getting started

Create a new project:

```sh
mkdir powerblog
cd powerblog
mkdir -p src/powerblog
mkdir -p dev/powerblog
mkdir content
mkdir -p resources/public
```

Add `deps.edn` to the root with the following content:

```clj
{:paths ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}
        no.cjohansen/powerpack {:mvn/version "2023.12.21"}}
 :aliases {:dev {:extra-paths ["dev"]}}}
```

### Configure the app

Powerpack has reasonable defaults for most things, so we'll start with a minimal
configuration and build it out as we go.

Add this to `src/powerblog/core.clj`:

```clj
(ns powerblog.core)

(defn render-page [context page]
  "<h1>Hello world</h1>")

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'render-page})
```

### Add a schema

Add an empty Datomic schema to the default location, `resources/schema.edn`:

```clj
[]
```

If you ned, you can change the location of the schema with the
`:datomic/schema-file` config option:

```clj
(def config
  {:datomic/schema-file "resources/schema.edn"
   :site/title "The Powerblog"
   :powerpack/render-page #'render-page})
```

### Add a dev namespace

We will need a namespace to run the site in development. Add the following to
`dev/powerblog/dev.clj`:

```clj
(ns powerblog.dev
  (:require [powerblog.core :as blog]
            [powerpack.dev :as dev]))

(defmethod dev/configure! :default []
  blog/config)  ;; 1

(comment

  (dev/start)   ;; 2
  (dev/stop)    ;; 3
  (dev/reset)   ;; 4

  (dev/get-app) ;; 5

  )
```

So, what's all this?

1. This is how Powerpack gets a hold of your configuration in development.
2. Evaluate this form to start the site
3. Evaluate this form to stop the site
4. Evaluate this form to reload all your code, rebuild the database and start
   the site.
5. Evaluate this form to grab a copy of the app instance. You can inspect it to
   find what configuration is being used, etc. You do not need this, it's just
   for the curious.

### Development convenience

Before we start up the app, I recommend a small convenience that ensures that
your code is loaded automatically when the REPL starts up. Add the following to
`dev/user.clj`:

```clj
(ns user
  (:require powerblog.dev))
```

If you're planning to run REPLs from Emacs, which I warmly recommend, you will
also want to add the following to `.dir-locals.el` at the root of the project:

```elisp
((nil
  (cider-clojure-cli-aliases . "-A:dev")
  (cider-preferred-build-tool . clojure-cli)))
```

This makes sure that CIDER includes the `:dev` alias when starting your REPL.

### Your first content

Powerpack doesn't come with content, so we'll have to add at least one page in
order to have something to test. Add the following to `content/test.md`:

```md
# Hello world!

Hello there, thanks for stopping by.
```

Powerpack will read all files in `content` (or whatever you set
`:powerpack/content-dir` to) into Datomic. It understands markdown and EDN out
of the box, and you can teach it how to parse other file formats as well.

By default, markdown files will be considered a page, and will receive a
`:page/uri` that corresponds to its relative path under
`:powerpack/content-dir`. In other words, `content/test.md` will be available as
`/test/`.

### Run the site

Start a REPL, and evaluate `(dev/start)` from the dev namespace. With the
default configuration, you should see a message about Powerpack being available
on [http://localhost:5050/](http://localhost:5050/). The frontpage will greet
you with a 404 for now, but your test page is available at
[http://localhost:5050/test/](http://localhost:5050/test/).

## Rendering pages

You probably noticed that the test page did not render the markdown content.
That's because our rendering function in `powerblog.core` looks like this:

```clj
(defn render-page [context page]
  "<h1>Hello world</h1>")
```

This function receives two parameters, a `context`, and a `page`. The `context`
contains `:uri`, just like a Ring request, a Datomic database value in `:app/db`
and `:powerpack/app`, your full Powerpack app. It is also possible to add
additional keys to the context per request, like secondary data sources, custom
configuration and whatever. We'll get back to that.

The more interesting parameter is the `page`, it is an entity map from Datomic.
It will contain whatever information you have added to the database under this
specific URL. Since we haven't added our own schema yet, it only contains the
URL and the contents of the markdown file:

```clj
{:page/uri "/test/",
 :page/body "# Hello world!\n\nHello there, thanks for stopping by.\n"}
```

Let's update the test page to render this as Markdown. Powerpack comes with a
markdown utility. Be sure to keep your browser open, visiting the test page,
then update `src/powerblog/core.clj`:

```clj
(ns powerblog.core
  (:require [powerpack.markdown :as md]))

(defn render-page [context page]
  (md/render-html (:page/body page)))

;; ...
```

You will now be introduced to Powerpack's development experience: the browser
automatically refreshes to render the updated version. Powerpack live reloads
your page whenever you change the content, the code that renders it, or any
assets (CSS, images, etc).

## Introducing: Mapdown

Powerpack understands [mapdown](https://github.com/magnars/mapdown), a small
extension to Markdown that allows you to put key/value pairs into a markdown
file. Let's use it to create a frontpage.

Add the following to `content/index.md`:

```md
:page/uri /
:page/body

# The Powerblog

You have reached the Powerblog, the highly fictitious blog that simply exists to
showcase [Powerpack](https://github.com/cjohansen/powerpack).
```

Because this file contains the `:page/uri` key, Powerpack will not give it a URL
based on its path. Instead, this will be our frontpage. You should be able to
see it on [http://localhost:5050/](http://localhost:5050/).

To demonstrate yet another development feature, try changing `:page/body` to
`:page-body` in the markdown file and save it. Powerpack will encounter an
error that is displayed in a HUD at the bottom of the page until you fix it.
Change it back and save the file to see the error go away.

## Adding a schema

Since we're building a blog, let's add a schema to store blog posts and authors.
Change `resources/schema.edn` so it contains the following:

```clj
[{:db/ident :blog-post/author
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one}
 {:db/ident :blog-post/tags
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/many}
 {:db/ident :person/id
  :db/valueType :db.type/keyword
  :db/cardinality :db.cardinality/one
  :db/unique :db.unique/identity}
 {:db/ident :person/full-name
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one}]
```

Whenever you change the schema, Powerpack will reboot your app and refresh the
browser.

Refer to Datomic's [schema
documentation](https://docs.datomic.com/pro/schema/schema.html) for details on
the schema.

Next up, we will add an author. Since we're not expecting large amounts of
markup, we'll stick this information in EDN files rather than markdown. You can
have multiple authors in one file, or one file per author - it's up to you.
Let's just create a file with a single author in it.

### Adding EDN content

Add the following to `content/authors/christian.edn`:

```clj
{:person/id :christian
 :person/full-name "Christian Johansen"}
```

Unlike markdown files, EDN files are not treated as pages by Powerpack. The
content is just read into Datomic. If you want EDN files to create one or more
pages, it must contain map(s) with `:page/uri` on them.

### Add a mapdown blog post

Create a blog post in `content/blog-posts/first-post.md`:

```md
:page/title On the wonders of climbing
:blog-post/tags [:climbing :nature]
:blog-post/author {:person/id :christian}
:page/body

# On the wonders of climbing

[Climbing](https://en.wikipedia.org/wiki/Climbing), a primal instinct ingrained
in our evolutionary history, takes on a playful and acrobatic twist when
observed in the world of monkeys. As we delve into the realm of these agile and
nimble creatures, we uncover a captivating tapestry of tree-bound adventures,
showcasing their unparalleled mastery of the vertical realm.
```

This is mapdown once again. Because we didn't give it a specific `:page/uri`, it
will be available at its path,
[/blog-posts/first-post/](http://localhost:5050/blog-posts/first-post/). Notice
the use of `:page/title` -- another built-in schema attribute. Also notice that
the `:blog-post/tags` is parsed as a collection of keywords to match the
database schema.

### Interact with the data model

The structured information about the blog post and the author is currently
nowhere to be seen on the site. You can verify that it's present by interacting
with the database. Update the `comment`-block in `powerblog.dev` to the
following and evaluate the forms:

```clj
(comment

  (set! *print-namespace-maps* false)

  (dev/start)
  (dev/stop)
  (dev/reset)

  (def app (dev/get-app))

  (require '[datomic.api :as d])

  (def db (d/db (:datomic/conn app)))

  (->> (d/entity db [:page/uri "/blog-posts/first-post/"])
       :blog-post/author
       (into {}))

  ;;=> {:person/id :christian
  ;;    :person/full-name "Christian Johansen"}

  )
```

This example shows you that the page is indeed a blog post, and you can navigate
to the author from the entity map.

### Differentiate rendering

We are currently rendering all pages the same way. We can add a dispatching
mechanism in `render-page` to render the frontpage differently from the blog
posts and other pages.

We'll handle the frontpage by dispatching on the URL, and then we'll see a more
robust mechanism that includes that page kind in the database.

Update `render-page` in `powerblog.core` to the following:

```clj
(defn render-page [context page]
  (cond
    (= "/" (:page/uri page))
    (render-frontpage context page)

    :else
    (md/render-html (:page/body page))))
```

Add this new function above it:

```clj
(defn render-frontpage [context page]
  [:html
   [:head
    [:title "The Powerblog"]]
   [:body
    (md/render-html (:page/body page))
    [:h2 "Blog posts"]]])
```

This page returns [hiccup](https://github.com/weavejester/hiccup), which
Powerpack knows how to render.

With this function in place, we can now render the frontpage differently from
the rest of the pages. Let's spice up the frontpage with a list of blog posts.

### Querying the database

The database is available on the `context` parameter under `:app/db`. We can use
it to query for any page that contains the `blog-post/author` key:

```clj
(defn get-blog-posts [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :blog-post/author]]
            db)
       (map #(d/entity db %))))

(defn render-frontpage [context page]
  [:html
   [:head
    [:title "The Powerblog"]]
   [:body
    (md/render-html (:page/body page))
    [:h2 "Blog posts"]
    [:ul
     (for [blog-post (get-blog-posts (:app/db context))]
       [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]]])
```

When you save this, the frontpage should update immediately and show your one
blog post. Clicking the link should take you to the blog post.

### HTML post processing

Having added a title to the frontpage, it becomes clear that the blog post is a
little bare bones. It doesn't even have a `body` tag. When that is the case,
Powerpack assumes the page is a fragment (perhaps for fetching via JavaScript)
and does no post processing. If we update the render function to wrap the page
in an HTML document, Powerpack adds `:page/title` as the head `title` if you
don't provide one yourself:

```clj
(defn render-page [context page]
  (cond
    (= "/" (:page/uri page))
    (render-frontpage context page)

    :else
    [:html [:body (md/render-html (:page/body page))]]))
```

Now the blog posts also have a DOCTYPE and the document title reads:

> On the wonders of climbing | The Powerblog

E.g. the page title, with the `:site/title` global configuration option added.

## Ingesting content

As demonstrated so far, Powerpack will do its best to ingest content from files
to Datomic automatically. But automatic based on assumptions only takes us so
far. For instance, it would be nice if the blog posts had a URL that started
with `/blog/` instead of the current `/blog-posts/`, and if they had a more
stable marker than the presence of `:blog-post/author`. We can fix this by
processing data on the way from files to the database.

You can give Powerpack a function with `:powerpack/create-ingest-tx` that will
be called every time content has been read from a file to ingest. The function
receives the file name and the parsed content. The content is always a vector --
even if the source is an EDN file with a single map, Powerpack will wrap it in a
vector. This is done so that the data can be transacted into Datomic
automatically, even when there is no `:powerpack/create-ingest-tx`. Let's see an
example. Head over to `powerblog.core` and add the option:

```clj
(defn create-tx [file-name txes]
  (cond->> txes
    (re-find #"^blog-posts/" file-name)
    (map #(assoc % :page/kind :page.kind/blog-post))))

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'render-page
   :powerpack/create-ingest-tx #'create-tx})
```

This adds the built-in attribute `:page/kind` to all the blog posts. The
attribute takes any keyword, and is well suited for separating different
kinds of pages.

When you change the main configuration, Powerpack will automatically reboot.

While we're at it, let's place the rendering functions in `powerblog.pages` and
the ingest function in `powerblog.ingest` -- gotta keep 'em separated. The
resulting `powerblog.core` namespace looks like this:

```clj
(ns powerblog.core
  (:require [powerblog.ingest :as ingest]
            [powerblog.pages :as pages]))

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx})
```

As your site grows, you might continue this line and add separate namespaces for
individual page types as well. We'll get there eventually.

### Improved render dispatch

Before we fix the render dispatch from before, lets expand the ingest function
to add `:page/kind` to all pages, that is all transaction entries that have a
`:page/uri`:

```clj
(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))
```

Let's now revisit `powerblog.pages/render-page` and use `:page/kind` for
dispatch:

```clj
(defn layout [{:keys [title]} & content]
  [:html
   [:head
    (when title [:title title])]
   [:body
    content]])

(def header
  [:header [:a {:href "/"} "Powerblog"]])

(defn render-frontpage [context page]
  (layout {:title "The Powerblog"}
   (md/render-html (:page/body page))
   [:h2 "Blog posts"]
   [:ul
    (for [blog-post (get-blog-posts (:app/db context))]
      [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]))

(defn render-article [context page]
  (layout {}
   header
   (md/render-html (:page/body page))))

(defn render-blog-post [context page]
  (render-article context page))

(defn render-page [context page]
  (case (:page/kind page)
    :page.kind/frontpage (render-frontpage context page)
    :page.kind/blog-post (render-blog-post context page)
    :page.kind/article (render-article context page)))
```

This is now starting to look like something to build upon. The structure that's
emerging is one where data processing happens at ingest, and page rendering is
about converting data from the database to markup. Notice that while Powerpack
caters to this sort of structure (e.g. by providing `:page/kind`), you are free
to find your own approach.

## Adding some pizazz

Most modern websites have some colors and typography that deviates from the bare
browser defaults (unfortunately, not every day is [CSS naked
day](https://css-naked-day.github.io/)). Let's add some of our own.

Powerpack has no opinion on how you do CSS. For this demonstration, we'll start
small with a single CSS file. Add the following to
[resources/public/styles.css](./resources/public/styles.css):

```css
html {
  font-size: 20px;
  font-family: Helvetica, arial, sans-serif;
}

body {
  background: #18181b;
  color: #f0f0f0;
  max-width: 800px;
  margin: 20px auto;
  line-height: 1.5;
}

a:link, a:visited {
  color: #2563eb;
  text-decoration: underline;
}

a:hover {
  text-decoration: none;
}
```

Then update the main Powerpack configuration in `powerblog.core` by adding the
CSS file as a bundle:

```clj
(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx

   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}}})
```

Powerpack uses [Optimus](https://github.com/magnars/optimus) to serve assets.
This way you will have perfectly optimized assets for use in production.

`:optimus/bundles` will automatically be included in any HTML response. CSS
bundles go in `head`, and JavaScript bundles go in the end of `body`. So,
without any further ado, the site should now look a little bit more smashing
than before.

Whenever you update the CSS file, Powerpack will hot reload it for you.

### Adding Tailwind to the mix

Let's try our hand at a slightly more involved asset setup by adding
[TailwindCSS](https://tailwindcss.com/).

Install and initialize Tailwind:

```sh
npm install -D tailwindcss
npm install -D @tailwindcss/typography
npx tailwindcss init
```

Next we'll configure Tailwind. It will be able to glean what classes we're using
from the Clojure source code and update the CSS file accordingly. Put the
following in `tailwind.config.js`:

```js
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
```

We'll need to run the Tailwind CLI to generate the CSS file. I like `Make`, so
put the following in a `Makefile`:

```
tailwind:
    npx tailwindcss -i ./src/main.css -o ./resources/public/styles.css --watch

.PHONY: tailwind
```

Add the source CSS file in `src/main.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html {
      font-size: 20px;
  }
}
```

Now run `make tailwind` in a terminal, and Tailwind will keep
`resources/public/styles.css` up to date for us. We can then add some Tailwind
utility classes to our page setup:

```clj
(defn layout [{:keys [title]} & content]
  [:html.dark:bg-zinc-900
   [:head
    (when title [:title title])]
   [:body.py-8
    content]])

(def header
  [:header.mx-auto.dark:prose-invert.prose.mb-8
   [:a {:href "/"} "Powerblog"]])

(defn render-frontpage [context page]
  (layout {:title "The Powerblog"}
   [:article.prose.dark:prose-invert.mx-auto
    (md/render-html (:page/body page))
    [:h2 "Blog posts"]
    [:ul
     (for [blog-post (get-blog-posts (:app/db context))]
       [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]]))

(defn render-article [context page]
  (layout {}
   header
   [:article.prose.dark:prose-invert.mx-auto
    (md/render-html (:page/body page))]))

(defn render-blog-post [context page]
  (render-article context page))
```

And just like that, all of Tailwind is at our hand.

## Going to production

A good development environment is all fun and games, but not worth alot if you
can't put something in production. So let's put something in production.

Create a new namespace in `src/powerblog/export.clj` with the following content:

```clj
(ns powerblog.export
  (:require [powerblog.core :as blog]
            [powerpack.export :as export]))

(defn ^:export export! [& args]
  (-> blog/config
      (assoc :site/base-url "https://www.example.com")
      export/export!))
```

As you can tell, this namespace is an excellent place to make adjustments to the
Powerpack configuration that are more suitable for the production environment.
For example, the `:site/base-url` property is used to qualify open graph URLs,
or - if you didn't add any - add an `og:url` meta tag to your pages,
differentiate internal and external links, and qualify asset and image urls.

Add a `:build` alias to `deps.edn`:

```clj
{,,,
 :aliases
 {:dev {:extra-paths ["dev"]}
  :build {:exec-fn powerblog.export/export!}}}
```

With this alias you can export the site like so:

```sh
$ clojure -X:build
[powerpack.export] Creating app
[powerpack.app] Created database in 245ms
[powerpack.export]  ... complete in 249ms
[powerpack.ingest] Ingested authors/christian.edn
[powerpack.ingest] Ingested blog-posts/first-post.md
[powerpack.ingest] Ingested index.md
[powerpack.ingest] Ingested test.md
[powerpack.app] Ingested all data in 37ms
[powerpack.export] Rendering, validating and exporting 3 pages
[powerpack.export]  ... complete in 89ms
[powerpack.export] Exporting assets
[powerpack.export]  ... complete in 2ms
[powerpack.export] Export complete
[powerpack.export] Exported 3 pages
[powerpack.export] Ran Powerpack export in 2081ms
```

Now you have a static copy of your site in `target/powerpack` (change the
destination by setting `:powerpack/build-dir` in the Powerpack configuration).
These files can be served as is by any static website host: nginx, AWS S3, etc.

## Displaying an image

Powerpack comes with image manipulation skills courtesy of
[imagine](https://github.com/cjohansen/imagine). Imagine allow you to configure
various image aliases that perform transformations such as duotone/greyscale,
crop, fit, scale, and transform any image accordingly.

We will use an image from the Wikipedia article on climbing, by Heinz Zak to
demonstrate:

```sh
mkdir resources/public/images

wget https://upload.wikimedia.org/wikipedia/commons/2/26/Heinz_Zak%2C_Separate_Reality_5%2C11d%2C_Free_Solo%2C_Yosemite-Nationalpark%2C_Kalifornien%2C_USA.jpg \
  -O resources/public/images/climbing.jpg
```

Let's add the image to our blog post. Update `content/blog-posts/first-post.md`
to the following:

```md
:page/title On the wonders of climbing
:blog-post/author {:person/id :christian}
:page/body

# On the wonders of climbing

[Climbing](https://en.wikipedia.org/wiki/Climbing), a primal instinct ingrained
in our evolutionary history, takes on a playful and acrobatic twist when
observed in the world of monkeys. As we delve into the realm of these agile and
nimble creatures, we uncover a captivating tapestry of tree-bound adventures,
showcasing their unparalleled mastery of the vertical realm.

![Heinz Zak climbing](/images/climbing.jpg)
```

When you do this, Powerpack will complain loudly. It does not like serving
assets that are not configured through either Optimus or Imagine. We'll explore
both options.

### Serving the image with Optimus

To serve the image through Optimus, add `:optimus/assets` to the Powerpack
configuration:

```clj
(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx

   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}}

   :optimus/assets [{:public-dir "public"
                     :paths [#".*\.jpg"]}]})
```

Now your image is available, and more importantly -- will be exported with the
rest of the site.

### Serving the image with Imagine

To serve the image with Imagine, we will add transformation configuration (refer
to the [Imagine readme](https://github.com/cjohansen/imagine) for more details
on that:

```clj
(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx

   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}}

   :optimus/assets [{:public-dir "public"
                     :paths [#".*\.jpg"]}]

   :imagine/config {:prefix "image-assets"
                    :resource-path "public"
                    :disk-cache? true
                    :transformations
                    {:preview-small
                     {:transformations [[:fit {:width 184 :height 184}]
                                        [:crop {:preset :square}]]
                      :retina-optimized? true
                      :retina-quality 0.4
                      :width 184}}}})
```

We can now prefix the image URL with the transformation name `preview-small` to
serve the image as a retina optimized 184x184 square image. In
`content/blog-posts/first-post.md`:

```md
...

![Heinz Zak climbing](/preview-small/images/climbing.jpg)
```

## Internationalizing content

Powerpack comes with i18n support through
[m1p](https://github.com/cjohansen/m1p), a tiny library for i18n, theming, and
other "content flavoring".

To use m1p, start by telling Powerpack where to find your dictionaries. In
`powerblog.core`:

```clj
(def config
  {;; ...

   :m1p/dictionaries {:nb ["src/powerblog/i18n/nb.edn"]
                      :en ["src/powerblog/i18n/en.edn"]}})
```

At their simplest, m1p dictionaries are just maps. Once again we'll start small.
Add these two files:

```clj
;; src/powerblog/i18n/nb.edn
{:powerblog.pages/blog-posts "Blogginnlegg"}

;; src/powerblog/i18n/en.edn
{:powerblog.pages/blog-posts "Blog posts"}
```

Next up, we'll need to know what locale a page is supposed to be in. The
built-in Powerpack schema includes `:page/locale`, which takes a keyword. We'll
use this to create two frontpages: one in Norwegian, and one in English.

Start by updating `content/index.md` like so:

```md
:page/uri /
:page/locale :en
:page/body

# The Powerblog

You have reached the Powerblog, the highly fictitious blog that simply exists to
showcase [Powerpack](https://github.com/cjohansen/powerpack).
```

Then add `content/index-nb.md`:

```md
:page/uri /nb/
:page/locale :nb
:page/body

# Powerbloggen

Du har nådd Powerbloggen, den høyst fiktive bloggen som kun eksisterer for å demonstrere
[Powerpack](https://github.com/cjohansen/powerpack).
```

Update the ingest function in `powerblog.ingest` so it marks the Norwegian
version as a frontpage as well:

```clj
(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index(-nb)?\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))
```

Now we're all set to localize the rendering function for the frontpage. Remember
that it currently contains the heading `[:h2 "Blog posts"]`. We can now replace
it with a reference to the key in our dictionaries, e.g.
`:powerblog.pages/blog-posts`:

```clj
(defn render-frontpage [context page]
  (layout {:title "The Powerblog"}
   [:article.prose.dark:prose-invert.mx-auto
    (md/render-html (:page/body page))
    [:h2 [:i18n :powerblog.pages/blog-posts]]
    [:ul
     (for [blog-post (get-blog-posts (:app/db context))]
       [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]]))
```

Since we were thoughtful enough to use the same namespace for the key as the
namespace this code lives in, we can even do this:

```clj
(defn render-frontpage [context page]
  (layout {:title "The Powerblog"}
   [:article.prose.dark:prose-invert.mx-auto
    (md/render-html (:page/body page))
    [:h2 [:i18n ::blog-posts]]
    [:ul
     (for [blog-post (get-blog-posts (:app/db context))]
       [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]]))
```

We can also make the dictionaries a little more convenient to edit by using
Clojure's namespaced maps feature:

```clj
;; src/powerblog/en.edn
#:powerblog.pages
{:blog-posts "Blog posts"}

;; src/powerblog/nb.edn
#:powerblog.pages
{:blog-posts "Blogginnlegg"}
```

As usual: when you edit your dictionaries, Powerpack automatically refreshes the
web page for you.

As the site grows and you have several namespaces, you can put multiple
namespaced maps in a vector in dictionary files -- or create multiple dictionary
files. It's up to you.

### Interpolating values

Simple key/value lookup is a little limited. Let's interpolate the number of
blog posts into the heading. Update the dictionaries:

```clj
#:powerblog.pages
{:blog-posts [:fn/str "Blog posts ({{:n}})"]}
```

`:fn/str` is a [dictionary
function](https://github.com/cjohansen/m1p?tab=readme-ov-file#dictionary-functions),
a m1p feature. Feed it a value like so:

```clj
(defn render-frontpage [context page]
  (let [blog-posts (get-blog-posts (:app/db context))]
    (layout {:title "The Powerblog"}
     [:article.prose.dark:prose-invert.mx-auto
      (md/render-html (:page/body page))
      [:h2 [:i18n ::blog-posts {:n (count blog-posts)}]]
      [:ul
       (for [blog-post blog-posts]
         [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]])))
```

### Custom dictionary functions

To install custom dictionary functions, like the [pluralization
helper](https://github.com/cjohansen/m1p?tab=readme-ov-file#pluralization) from
the m1p docs, provide them with the main Powerpack configuration:

```clj
(ns powerblog.core
  (:require [m1p.core :as m1p]
            [powerblog.ingest :as ingest]
            [powerblog.pages :as pages]))

(defn pluralize [opt n & plurals]
  (-> (nth plurals (min (if (number? n) n 0) (dec (count plurals))))
      (m1p/interpolate-string {:n n} opt)))

(def config
  {;; ...
   :m1p/dictionaries {:nb ["src/powerblog/i18n/nb.edn"]
                      :en ["src/powerblog/i18n/en.edn"]}
   :m1p/dictionary-fns {:fn/plural #'pluralize}})
```

We can use it in dictionaries like this:

```clj
#:powerblog.pages
{:blog-posts [:fn/plural
              "No blog posts yet"
              "My blog post"
              "Blog posts ({{:n}})"]}
```

Now the heading will read "No blog posts yet" when there are no blog posts, "My
blog post" when there is only one blog post, and "Blog posts (3)" when there are
3 blog posts.

## Open Graph

Powerpack comes with some built-in keys you can use to auto-generate [open
graph](https://opengraph.dev/) meta tags on your pages:

- `:open-graph/title`
- `:open-graph/description`
- `:open-graph/image`

These can be added directly to our blog post as such:

```md
:page/title On the wonders of climbing
:blog-post/author {:person/id :christian}
:open-graph/title Climbing
:open-graph/description An interesting piece about climbing
:open-graph/image /preview-small/images/climbing.jpg
:page/body

# On the wonders of climbing

[Climbing](https://en.wikipedia.org/wiki/Climbing), a primal instinct ingrained
in our evolutionary history, takes on a playful and acrobatic twist when
observed in the world of monkeys. As we delve into the realm of these agile and
nimble creatures, we uncover a captivating tapestry of tree-bound adventures,
showcasing their unparalleled mastery of the vertical realm.

![Heinz Zak climbing](/preview-small/images/climbing.jpg)
```

With these keys on the page entity, the markup will automatically include
sensible open graph tags:

```html
<!DOCTYPE html>
<html lang="en" prefix="og: http://ogp.me/ns#" class="dark:bg-zinc-900">
  <head>
    <title>On the wonders of climbing | The Powerblog</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta property="og:description" content="An interesting piece about climbing">
    <meta property="og:title" content="Climbing">
    <meta property="og:image" content="/image-assets/preview-small/fb6a746aee13f753872432da49c32a1cd019a334/images/climbing.jpg">
    <meta property="og:image:width" content="184">
    <meta property="og:image:height" content="184">
    <link rel="stylesheet" href="/af8dc61fd222/styles.css">
  </head>
  <body class="py-8">
    <!-- ... -->
  </body>
</html>
```

As you can see, Powerpack adds not only open graph metas, but some other useful
ones as well. It will only add these meta tags if they are not already present.
If you add your own `<meta name="viewport" ...>` with different `content`,
Powerpack will not touch it.

You will notice that the image URL is different from the one we set on the page.
The generated URL includes a [cache
buster](https://sparkbox.com/foundry/browser_cache_busting_explained), which
allows you to set a [far future expires
header](https://stevesouders.com/examples/rule-expires.php) on images for best
performance. The URL will change whenever the underlying image changes, but not
otherwise.
