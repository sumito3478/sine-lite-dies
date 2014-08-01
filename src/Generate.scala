/*
Copyright (C) 2014 sumito3478 <sumito3478@gmail.com>
*/

package sine.lite.dies

import java.time.ZoneId

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.fusesource.scalate._
import java.nio.file._
import java.io._
import org.yaml.snakeyaml.Yaml

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.sys.process._

object Generate extends App with LazyLogging {
  val articles = new Articles
  val target = Paths.get("static-site")
  Files.createDirectories(target)
  logger.info(Process(Seq("python3", "tools/build.py"), new File("highlight.js")).!!)
  Files.copy(Paths.get("highlight.js/build/highlight.pack.js"), target.resolve("highlight.pack.js"), StandardCopyOption.REPLACE_EXISTING)
  Files.copy(Paths.get("gwebfont.css"), target.resolve("gwebfont.css"), StandardCopyOption.REPLACE_EXISTING)
  val out = new FileOutputStream(new File("static-site/style.css"))
  logger.info(Seq("npm", "install", "nib").!!)
  logger.info((Seq("stylus", "--include", "node_modules/nib/lib") #< new FileInputStream(new File("style.styl")) #> out).!!)
  out.close
  try {
    for (article <- articles.articles) {
      //    Files.write(dst, output.getBytes("UTF-8"))
      val dst = target.resolve(article.path.toString.replaceFirst(".md$", ".html"))
      logger.info(s"Converting ${article.path} to $dst")
      Files.createDirectories(dst.getParent)
      Files.write(dst, article.html.getBytes("UTF-8"))
    }
    for (tag <- articles.tags) {
      val dst = target.resolve(tag.path)
      logger.info(s"Converting tag $tag into $dst")
      Files.createDirectories(dst.getParent)
      Files.write(dst, tag.html.getBytes("UTF-8"))
    }
    val all = articles.All
    val dst = target.resolve(all.path)
    logger.info(s"Converting all articles into $dst")
    Files.createDirectories(dst.getParent)
    Files.write(dst, all.html.getBytes("UTF-8"))
    val indexDst = target.resolve(articles.Index.path)
    logger.info(s"Converting the article index into $indexDst")
    Files.createDirectories(indexDst.getParent)
    Files.write(indexDst, articles.Index.html.getBytes("UTF-8"))
    val tagsDst = target.resolve(articles.Tags.path)
    logger.info(s"Converting the tag index into $tagsDst")
    Files.createDirectories(tagsDst.getParent)
    Files.write(tagsDst, articles.Tags.html.getBytes("UTF-8"))
    logger.info("finished conversion")
  } catch {
    case e: Throwable => logger.error("Exception occurred!", e)
  }
  sys.exit
  //    logger.info("Try starting conversion!")
  //    val article = Paths.get("article")
  //    val target = Paths.get("static-site")
  //    val menu = Seq(
  //      "top" -> "index.html",
  //      "tags" -> "tags.html")
  //    val modifiedDate = java.time.LocalDate.now.toString
  //    def asString(x: Any) = x match {
  //      case x: java.util.Date => x.toInstant.atZone(ZoneId.systemDefault).toLocalDate.toString
  //      case x => x.toString
  //    }
  //    def asStringPandoc(x: Any) = x match {
  //      case x: String => PandocFilter.pandocConvert(x)
  //      case x => asString(x)
  //    }
  //    case class Article(path: Path) {
  //      lazy val src = new String(Files.readAllBytes(path), "UTF-8")
  //
  //      // TODO: make this typesafe
  //      lazy val metaData: Map[String, Any] = {
  //        val lines = src.lines.toList
  //        val (acc, rest) = (for {
  //          firstLine <- lines.headOption
  //          if firstLine == "---"
  //        } yield {
  //          val (yaml, rest) = lines.tail.span(line => line != "---" && line != "...")
  //          (List(yaml.mkString("\n")), rest.tail)
  //        }) match {
  //          case None => (List(), lines.tail)
  //          case Some((yamls, rest)) => (yamls, rest)
  //        }
  //        @tailrec
  //        def readRest(yamls: List[String], rest: List[String]): List[String] = {
  //          (for {
  //            firstLine <- rest.headOption
  //            if firstLine.isEmpty
  //            secondLine <- rest.tail.headOption
  //            if secondLine == "---"
  //            r = rest.tail.tail
  //          } yield {
  //            val (yaml, rest) = r.span(line => line != "---" || line != "...")
  //            (yaml.mkString("\n"), rest.tail)
  //          }) match {
  //            case None if rest.isEmpty => yamls.reverse
  //            case None => readRest(yamls, rest.tail)
  //            case Some((yaml, rest)) => readRest(yaml :: yamls, rest)
  //          }
  //        }
  //        val yamls = readRest(acc, rest)
  //        val yaml = new Yaml
  //        val metaDataList: List[Map[String, Any]] = for (y <- yamls) yield yaml.load(y) match {
  //          case xs: java.util.Map[_, _] => xs.asScala.map { case (k, v) => asString(k) -> v }.toMap
  //          case xs: java.util.List[_] => xs.asScala.toList.zipWithIndex.map { case (x, i) => s"_$i" -> x }.toMap
  //        }
  //        metaDataList.foldLeft(Map[String, Any]()) { case (acc, x) => acc ++ x }
  //      }
  //
  //      lazy val tags: List[String] = metaData.lift("tags") match {
  //        case x: java.util.List[_] => x.asScala.map(_.toString).toList
  //        case _ => List()
  //      }
  //    }
  //    val articles = Files.list(article).iterator.asScala.filter(_.getFileName.toString.endsWith(".md")).map(Article(_)).toList
  //    val tagMap = articles.flatMap(_.tags).toSet.map(tag => tag -> articles.filter(_.tags.contains(tag))).toMap
  //    val metaDataMap = for (article <- articles) yield {
  //      val src = new String(Files.readAllBytes(article), "UTF-8")
  //      // Read the YAML metadata block(s)
  //      val lines = src.lines.toList
  //      val (acc, rest) = (for {
  //        firstLine <- lines.headOption
  //        if firstLine == "---"
  //      } yield {
  //        val (yaml, rest) = lines.tail.span(line => line != "---" && line != "...")
  //        logger.info(yaml.toString)
  //        logger.info(rest.toString)
  //        (List(yaml.mkString("\n")), rest.tail)
  //      }) match {
  //        case None => (List(), lines.tail)
  //        case Some((yamls, rest)) => (yamls, rest)
  //      }
  //      @tailrec
  //      def readRest(yamls: List[String], rest: List[String]): List[String] = {
  //        (for {
  //          firstLine <- rest.headOption
  //          if firstLine.isEmpty
  //          secondLine <- rest.tail.headOption
  //          if secondLine == "---"
  //          r = rest.tail.tail
  //        } yield {
  //          val (yaml, rest) = r.span(line => line != "---" || line != "...")
  //          (yaml.mkString("\n"), rest.tail)
  //        }) match {
  //          case None if rest.isEmpty => yamls.reverse
  //          case None => readRest(yamls, rest.tail)
  //          case Some((yaml, rest)) => readRest(yaml :: yamls, rest)
  //        }
  //      }
  //      val yamls = readRest(acc, rest)
  //      val yaml = new Yaml
  //      val metaDataList: List[Map[String, Any]] = for (y <- yamls) yield yaml.load(y) match {
  //        case xs: java.util.Map[_, _] => xs.asScala.map { case (k, v) => asString(k) -> v }.toMap
  //        case xs: java.util.List[_] => xs.asScala.toList.zipWithIndex.map { case (x, i) => s"_$i" -> x }.toMap
  //      }
  //      val metaData = metaDataList.foldLeft(Map[String, Any]()) { case (acc, x) => acc ++ x }
  //
  //    }
  //    val metaDataList = (for (article <- articles) yield {
  //      val dst = target.resolve(article.toString.replaceFirst(".md$", ".html"))
  //      logger.info(s"Converting layout.jade and $article into $dst")
  //      val src = new String(Files.readAllBytes(article), "UTF-8")
  //      // Read the YAML metadata block
  //      val lines = src.lines.toList
  //      val (acc, rest) = (for {
  //        firstLine <- lines.headOption
  //        if firstLine == "---"
  //      } yield {
  //        val (yaml, rest) = lines.tail.span(line => line != "---" && line != "...")
  //        logger.info(yaml.toString)
  //        logger.info(rest.toString)
  //        (List(yaml.mkString("\n")), rest.tail)
  //      }) match {
  //        case None => (List(), lines.tail)
  //        case Some((yamls, rest)) => (yamls, rest)
  //      }
  //      @tailrec
  //      def readRest(yamls: List[String], rest: List[String]): List[String] = {
  //        (for {
  //          firstLine <- rest.headOption
  //          if firstLine.isEmpty
  //          secondLine <- rest.tail.headOption
  //          if secondLine == "---"
  //          r = rest.tail.tail
  //        } yield {
  //          val (yaml, rest) = r.span(line => line != "---" || line != "...")
  //          (yaml.mkString("\n"), rest.tail)
  //        }) match {
  //          case None if rest.isEmpty => yamls.reverse
  //          case None => readRest(yamls, rest.tail)
  //          case Some((yaml, rest)) => readRest(yaml :: yamls, rest)
  //        }
  //      }
  //      val yamls = readRest(acc, rest)
  //      val yaml = new Yaml
  //      val metaDataList: List[Map[String, Any]] = for (y <- yamls) yield yaml.load(y) match {
  //        case xs: java.util.Map[_, _] => xs.asScala.map { case (k, v) => asString(k) -> v }.toMap
  //        case xs: java.util.List[_] => xs.asScala.toList.zipWithIndex.map { case (x, i) => s"_$i" -> x }.toMap
  //      }
  //      val metaData = metaDataList.foldLeft(Map[String, Any]()) { case (acc, x) => acc ++ x }
  //      val engine = new TemplateEngine
  //      val output = engine.layout("layout.jade",
  //        Map("title" -> "Sine Lite Dies",
  //          "contents" -> Seq(src),
  //          "lang" -> "ja",
  //          "css" -> "../style.css",
  //          "menu" -> menu,
  //          "modifiedDate" -> modifiedDate,
  //          "metaData" -> metaData.mapValues(asStringPandoc _)))
  //      logger.info(output)
  //      Files.write(dst, output.getBytes("UTF-8"))
  //      article -> metaData
  //    }).toMap
  //    val metaData = metaDataList.values.foldLeft(Map[String, Any]()) { case (acc, x) => acc ++ x }
  //    val dst = target.resolve("index.html")
  //    val engine = new TemplateEngine
  //    val output = engine.layout("layout.jade",
  //      Map("title" -> "Sine Lite Dies",
  //        "contents" -> articles.map(x => new String(Files.readAllBytes(x), "UTF-8")),
  //        "lang" -> "ja",
  //        "css" -> "style.css",
  //        "menu" -> menu,
  //        "modifiedDate" -> modifiedDate,
  //        "metaData" -> metaData.mapValues(asStringPandoc _)))
  //    logger.info(output)
  //    Files.write(dst, output.getBytes("UTF-8"))
}
