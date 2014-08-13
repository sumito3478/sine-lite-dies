/*
Copyright (C) 2014 sumito3478 <sumito3478@gmail.com>
*/

package sine.lite.dies

import com.typesafe.scalalogging.slf4j.LazyLogging
import java.nio.file._
import java.nio.file.attribute._
import java.io._
import scala.sys.process._

object Generate extends App with LazyLogging {
  val articles = new Articles
  val target = Paths.get("static-site")
  def syncLastModifiedTime(src: Path, dst: Path) =
    Files.setLastModifiedTime(dst, Git.updatedAt(Some(src)).map(x => FileTime.from(x.toInstant)).getOrElse(Files.getLastModifiedTime(src)))
  Files.createDirectories(target)
  Files.copy(Paths.get("eb-garamond-08-regular.woff"), target.resolve("eb-garamond-08-regular.woff"), StandardCopyOption.REPLACE_EXISTING)
  syncLastModifiedTime(Paths.get("eb-garamond-08-regular.woff"), target.resolve("eb-garamond-08-regular.woff"))
  val out = new FileOutputStream(new File("static-site/style.css"))
  logger.info(Seq("npm", "install", "nib").!!)
  logger.info((Seq("stylus", "--include", "node_modules/nib/lib") #< new FileInputStream(new File("style.styl")) #> out).!!)
  syncLastModifiedTime(Paths.get("style.styl"), target.resolve("style.css"))
  out.close
  try {
    for (article <- articles.articles) {
      val dst = target.resolve(article.path.toString.replaceFirst(".md$", ".html"))
      logger.info(s"Converting ${article.path} to $dst")
      Files.createDirectories(dst.getParent)
      Files.write(dst, article.html.getBytes("UTF-8"))
      syncLastModifiedTime(article.path, dst)
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
    val aboutDst = target.resolve(articles.About.path.toString.replaceFirst(".md$", ".html"))
    Files.write(aboutDst, articles.About.html.getBytes("UTF-8"))
    val feedDst = target.resolve(articles.Feed.path)
    Files.write(feedDst, articles.Feed.xml.getBytes("UTF-8"))
    logger.info("finished conversion")
  } catch {
    case e: Throwable => logger.error("Exception occurred!", e)
  }
  sys.exit
}
