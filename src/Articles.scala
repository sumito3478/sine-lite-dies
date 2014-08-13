package sine.lite.dies

import java.nio.file.attribute.BasicFileAttributes
import java.time.{ ZonedDateTime, LocalDate, ZoneId }

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.codec.binary.Base32
import org.eclipse.jgit.lib.ObjectId
import org.fusesource.scalate._
import java.nio.file._
import org.fusesource.scalate.scaml.ScamlOptions
import org.yaml.snakeyaml.Yaml
import java.io._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import Git.ObjectIdOps

object Articles {
  val engine = new TemplateEngine
  engine.workingDirectory = new File("tmp")
  engine.allowReload = true
  engine.allowCaching = true
  ScamlOptions.format = ScamlOptions.Format.html5
  ScamlOptions.indent = ""
  val layout = engine.load("layout.jade")
}

import Articles._

class Articles extends LazyLogging {
  val article = Paths.get("article")
  lazy val articles = About :: Files.list(article).iterator.asScala.filter(_.getFileName.toString.endsWith(".md")).map(Article(_)).toList
  lazy val tagMap = articles.map(_.tags).flatten.toSet.map { tag: String => tag -> articles.filter(_.tags.contains(tag)) }.toMap
  lazy val allTags = articles.map(_.tags).flatten.toSet.toList.sorted
  case class Tag(name: String) {
    lazy val path = Paths.get("tag", s"$name.html")
    def html = {
      val root = path.getParent.relativize(Paths.get("."))
      val css = path.getParent.relativize(Paths.get("style.css"))
      val articles = tagMap.lift(name).toList.flatten
      val updatedAt = articles.map(_.updatedAt).maxBy(x => x.toInstant.toEpochMilli)
      val createdAt = articles.map(_.createdAt).maxBy(x => x.toInstant.toEpochMilli)
      val contents = articles.map(_.content)
      engine.layout("layout.jade",
        Map("title" -> s"タグ $name",
          "contents" -> contents,
          "lang" -> "ja",
          "css" -> css.toString,
          "allTags" -> allTags,
          "highlighted" -> HighlightJS.dependencies(contents.map(_.body).mkString("\n")),
          "root" -> root.toString,
          "path" -> path.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
  }
  object All {
    lazy val path = article.resolve("all.html")
    def html = {
      val root = path.getParent.relativize(Paths.get("."))
      val css = path.getParent.relativize(Paths.get("style.css"))
      val updatedAt = articles.map(_.updatedAt).maxBy(x => x.toInstant.toEpochMilli)
      val createdAt = articles.map(_.createdAt).maxBy(x => x.toInstant.toEpochMilli)
      val contents = articles.sortBy(x => x.updatedAt.toInstant.toEpochMilli)(implicitly[Ordering[Long]].reverse).map(_.content)
      engine.layout("layout.jade",
        Map("title" -> s"全ての記事",
          "contents" -> contents,
          "lang" -> "ja",
          "css" -> css.toString,
          "allTags" -> allTags,
          "highlighted" -> HighlightJS.dependencies(contents.map(_.body).mkString("\n")),
          "root" -> root.toString,
          "path" -> path.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
    lazy val atomPath = path.toString.replaceFirst(".html$", ".xml")
    def atom = {
      val root = path.getParent.relativize(Paths.get("."))
      val updatedAt = articles.map(_.updatedAt).maxBy(x => x.toInstant.toEpochMilli)
      val createdAt = articles.map(_.createdAt).maxBy(x => x.toInstant.toEpochMilli)
      engine.layout("feed.jade",
        Map("title" -> s"Sine Lite Dies",
          "contents" -> articles.map(_.content).sortBy(_.updatedAt.toInstant.toEpochMilli)(implicitly[Ordering[Long]].reverse),
          "lang" -> "ja",
          "root" -> root.toString,
          "urn" -> Atom.stringToURN("sine.lite.dies.feed"),
          "path" -> atomPath.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
  }
  object Index {
    lazy val path = Paths.get("./index.html")
    def html = {
      val root = Paths.get(".")
      val css = Paths.get("style.css")
      val updatedAt = articles.map(_.updatedAt).maxBy(x => x.toInstant.toEpochMilli)
      val createdAt = articles.map(_.createdAt).maxBy(x => x.toInstant.toEpochMilli)
      val as = articles.sortBy(_.updatedAt.toInstant.toEpochMilli)(implicitly[Ordering[Long]].reverse)
      val indexMarkdown = as.map(x => s"- [${x.content.title}](${x.path.toString.replaceFirst(".md$", ".html")})").mkString("", "\n", "\n")
      engine.layout("layout.jade",
        Map("title" -> s"INDEX",
          "contents" -> Seq(Content(body = indexMarkdown, metaData = Map(), tags = List(), title = "記事一覧", permalink = path.toString, updatedAt = updatedAt, createdAt = createdAt, urn = Atom.stringToURN("sine.lite.dies.index"))),
          "lang" -> "ja",
          "css" -> css.toString,
          "allTags" -> allTags,
          "highlighted" -> Set(),
          "root" -> root.toString,
          "path" -> path.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
  }
  object Tags {
    lazy val path = Paths.get("./tags.html")
    def html = {
      val root = Paths.get(".")
      val css = Paths.get("style.css")
      val indexMarkdown = allTags.map(tag => s"- [$tag](tag/$tag.html)").mkString("", "\n", "\n")
      val updatedAt = articles.map(_.updatedAt).maxBy(x => x.toInstant.toEpochMilli)
      val createdAt = articles.map(_.createdAt).maxBy(x => x.toInstant.toEpochMilli)
      engine.layout("layout.jade",
        Map("title" -> s"TAGS",
          "contents" -> Seq(Content(body = indexMarkdown, metaData = Map(), tags = List(), title = "タグ一覧", permalink = path.toString, updatedAt = updatedAt, createdAt = createdAt, urn = Atom.stringToURN("sine.lite.dies.tags"))),
          "lang" -> "ja",
          "css" -> css.toString,
          "allTags" -> allTags,
          "highlighted" -> Set(),
          "root" -> root.toString,
          "path" -> path.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
  }
  object About extends Article(path = Paths.get("./about.md"))
  lazy val tags = allTags.map(Tag(_))
  case class Article(path: Path) {
    private[this] def asString(x: Any) = x match {
      case x: java.util.Date => x.toInstant.atZone(ZoneId.systemDefault).toLocalDate.toString
      case x => x.toString
    }
    private[this] def asStringPandoc(x: Any) = x match {
      case x: String => PandocFilter.pandocConvert(x)
      case x => asString(x)
    }
    lazy val src = new String(Files.readAllBytes(path), "UTF-8")
    // TODO: make this typesafe
    lazy val metaData: Map[String, Any] = {
      val lines = src.lines.toList
      val (acc, rest) = (for {
        firstLine <- lines.headOption
        if firstLine == "---"
      } yield {
        val (yaml, rest) = lines.tail.span(line => line != "---" && line != "...")
        (List(yaml.mkString("\n")), rest.tail)
      }) match {
        case None => (List(), lines.tail)
        case Some((yamls, rest)) => (yamls, rest)
      }
      @tailrec
      def readRest(yamls: List[String], rest: List[String]): List[String] = {
        (for {
          firstLine <- rest.headOption
          if firstLine.isEmpty
          secondLine <- rest.tail.headOption
          if secondLine == "---"
          r = rest.tail.tail
        } yield {
          val (yaml, rest) = r.span(line => line != "---" || line != "...")
          (yaml.mkString("\n"), rest.tail)
        }) match {
          case None if rest.isEmpty => yamls.reverse
          case None => readRest(yamls, rest.tail)
          case Some((yaml, rest)) => readRest(yaml :: yamls, rest)
        }
      }
      val yamls = readRest(acc, rest)
      val yaml = new Yaml
      val metaDataList: List[Map[String, Any]] = for (y <- yamls) yield yaml.load(y) match {
        case xs: java.util.Map[_, _] => xs.asScala.map { case (k, v) => asString(k) -> v }.toMap
        case xs: java.util.List[_] => xs.asScala.toList.zipWithIndex.map { case (x, i) => s"_$i" -> x }.toMap
      }
      metaDataList.foldLeft(Map[String, Any]()) { case (acc, x) => acc ++ x }
    }
    lazy val tags: List[String] = metaData.lift("tags") match {
      case Some(x: java.util.List[_]) =>
        val directTags = x.asScala.map(_.toString).toList
        directTags.map(_.split("/").toList).flatMap(tag => for (i <- 0 until tag.size) yield tag.dropRight(i)).map(_.mkString("/"))
      case _ => List()
    }
    lazy val title = metaData.lift("title").map(_.toString).getOrElse(path.getFileName.toString)
    lazy val content = Content(
      body = new String(Files.readAllBytes(path), "UTF-8"),
      metaData = metaData.filterKeys(!List("tags", "title").contains(_)).mapValues(asStringPandoc(_)),
      tags = tags,
      title = title,
      permalink = path.toString.replaceFirst(".md$", ".html"),
      updatedAt = updatedAt,
      createdAt = createdAt,
      urn = creationId.toURN)
    lazy val updatedAt: ZonedDateTime = {
      import scala.util.control.Exception
      val ret = Exception.allCatch[Option[ZonedDateTime]].either(Git.updatedAt(Some(path))) match {
        case Left(e) =>
          logger.error("Failed to get last modified date from git log", e)
          None
        case Right(None) =>
          None
        case Right(Some(x)) => Some(x)
      }
      ret.getOrElse {
        logger.warn("Could not get last modified date from git log, falling back to information from the file system")
        Files.getLastModifiedTime(path).toInstant.atZone(ZoneId.systemDefault)
      }
    }
    lazy val createdAt = {
      import scala.util.control.Exception
      val ret = Exception.allCatch[Option[ZonedDateTime]].either(Git.createdAt(Some(path))) match {
        case Left(e) =>
          logger.error("Failed to get last modified date from git log", e)
          None
        case Right(None) =>
          None
        case Right(Some(x)) => Some(x)
      }
      ret.getOrElse {
        logger.warn("Could not get  date from git log, falling back to information from the file system")
        val attr = Files.readAttributes(path, classOf[BasicFileAttributes])
        attr.creationTime.toInstant.atZone(ZoneId.systemDefault)
      }
    }
    lazy val creationId = {
      Git.creationId(path).getOrElse(sys.error("Could not get creation id"))
    }
    def html: String = {
      val menu = Seq(
        "# top" -> "index.html",
        "# tags" -> "tags.html")
      val css = path.getParent.relativize(Paths.get("style.css"))
      logger.info(s"allTags = $allTags")
      val root = path.getParent.relativize(Paths.get("."))
      engine.layout("layout.jade",
        Map("title" -> title,
          "contents" -> Seq(content),
          "lang" -> "ja",
          "css" -> css.toString,
          "allTags" -> allTags,
          "root" -> root.toString,
          "highlighted" -> HighlightJS.dependencies(content.body),
          "menu" -> menu.map { case (k, v) => asStringPandoc(k) -> v },
          "path" -> path.toString,
          "updatedAt" -> updatedAt,
          "createdAt" -> createdAt))
    }
  }
}
