# clj-reinit

`clj-reinit` is to [clj-reload](https://github.com/tonsky/clj-reload)/[init](https://github.com/ferdinand-beyer/init) as [integrant-repl](https://github.com/weavejester/integrant-repl) is to [tools.namespace](https://github.com/clojure/tools.namespace)/[integrant](https://github.com/weavejester/integrant).

Except it's way cooler.

## Dependency

```
io.github.z-silver/clj-reinit {...}
```

There is no jar release at the moment. Apologies for the inconvenience.

I recommend using the tagged versions, but feel free to pick your favorite commit.

## Why?

Because unlike the tools based on `tools.namespace`, `clj-reinit` offers not only reloading on demand but also reloading automatically. That is: it is capable of reloading your code as soon as you save it, without the need to explicitly ask for a reload. If you've used [figwheel](https://figwheel.org/) or [shadow-cljs](https://github.com/thheller/shadow-cljs), you should be familiar with this workflow.

## No, seriously, why?

Because I've recently discovered `init` and find it nicer to use than `integrant`.

I was slightly bummed out that there was no equivalent to `integrant-repl` for `init`. But since I had also recently learned about `clj-reload`, which I prefer over `tools.namespace`, I figured: why not?

Since I'm very used to the instant feedback cycle you typically get with Clojurescript, I decided to also attempt to do something similar for JVM Clojure, which is not something I've seen before.

## How?

We exploit the fact that `clj-reload` expects you to tell it which directories your source code is located, and use it to spin up a watcher that detects whenever a file in those directories has changed. We also exploit the fact that we're using `init` and piggyback on it to manage the lifetime of said watcher.

Similar to `integrant-repl`, we expect the user to choose a var containing a configuration function. This is because we otherwise have no way of knowing what system you want to run.

I recommend reading the source code comments for all the details, as well as `dev/user.clj` for a tiny example of how you may set up your own project's dev environment.

## Caveats

If compilation fails during a reload, you'll have to manually run `reset` after you fix your code. This is because reloading stops the system, which also stops the watcher component.

If you need to run commands in the REPL after having an auto-reload, you'll still need to run `reset` to make the new version of the code available in the REPL. For this reason, auto-reloading is actually off by default and needs to be explicitly enabled.

## Acknowledgements

* `clj-reload` and `init`, the foundations which we compose together.
* [dirwatch](https://github.com/juxt/dirwatch), for implementing a very nice, no-nonsense interface over Java's WatchService.
* `integrant-repl`, the direct inspiration for this lib.
* You, for taking the time to read and perhaps use this.
