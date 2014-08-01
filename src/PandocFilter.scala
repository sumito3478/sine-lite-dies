/*
Copyright (C) 2014 sumito3478 <sumito3478@gmail.com>
*/

package sine.lite.dies

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.fusesource.scalate.{ RenderContext, TemplateEngine, TemplateEngineAddOn }
import org.fusesource.scalate.filter.Filter
import scala.sys.process._
import java.io._

object PandocFilter extends Filter with TemplateEngineAddOn with LazyLogging {
  def pandocConvert(x: String) = {
    // avoid misterious hangs...
    // FIXME: fix hangs without retrying
    def retry(i: Int): String = {
      if (i > 10)
        sys.error("Pandoc conversion timed out for 10 times")
      val currentThread = Thread.currentThread
      try {
        val watcher = new Thread(new Runnable {
          def run = {
            try {
              Thread.sleep(3000 + i * 700)
              currentThread.interrupt
            } catch {
              case _: InterruptedException =>
            }
          }
        })
        watcher.start
        val ret = (Seq("pandoc", "-f", "markdown", "-t", "html5", "--no-highlight") #< new ByteArrayInputStream(x.getBytes("UTF-8"))).!!
        watcher.interrupt
        ret
      } catch {
        case _: InterruptedException =>
          logger.info("Pandoc conversion timed out! retrying...")
          retry(i + 1)
      }
    }
    val ret = retry(0)
    ret
  }
  def filter(context: RenderContext, content: String): String = pandocConvert(content)
  def apply(te: TemplateEngine) = {
    if (!te.filters.contains("pandoc-markdown")) {
      logger.info("adding PandocFilter to template engine")
      logger.info(te.filters.toString)
      te.filters += "pandoc-markdown" -> this
      te.pipelines += "md" -> List(PandocFilter)
      te.pipelines += "markdown" -> List(PandocFilter)
    }
  }
}
