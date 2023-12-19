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
