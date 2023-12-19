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
