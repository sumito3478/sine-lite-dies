package sine.lite.dies

object HighlightJS {
  val languages = List("1c", "actionscript", "apache", "applescript", "asciidoc", "autohotkey", "avrasm", "axapta", "bash", "brainfuck", "clojure", "cmake", "coffeescript", "cpp", "cs", "css", "d", "delphi", "diff", "django", "dos", "erlang-repl",
    "erlang", "fix", "fsharp", "glsl", "go", "haml", "handlebars", "haskell", "http", "ini", "java", "javascript", "json", "lasso", "lisp", "livecodeserver", "lua", "makefile", "markdown", "mathematica", "matlab", "mel", "mizar", "nginx",
    "objectivec", "ocaml", "oxygene", "parser3", "perl", "php", "profile", "python", "r", "rib", "rsl", "ruby", "ruleslanguage", "rust", "scala", "scilab", "scss", "smalltalk", "sql", "tex", "vala", "vbnet", "vbscript", "vhdl", "xml")

  val MarkdownCodeFenceStart = "^```(.+)$".r
  def dependencies(markdownText: String): Set[String] = markdownText.lines.flatMap {
    case MarkdownCodeFenceStart(lang) if languages.contains(lang) => Some(lang)
    case _ => None
  }.toSet
}
